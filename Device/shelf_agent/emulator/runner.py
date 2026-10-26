"""Runs a scenario: N virtual shelves, each a real :class:`ShelfAgent`."""

from __future__ import annotations

import threading
from collections.abc import Callable, Sequence

import httpx

from shelf_agent.agent import Delivery, ShelfAgent, TickResult, run_heartbeat
from shelf_agent.camera.base import CameraError, CameraSource, SourceExhausted
from shelf_agent.camera.folder import FolderSource
from shelf_agent.emulator.scenario import EmulatedShelf, Scenario, WebcamSettings
from shelf_agent.registration import connect
from shelf_agent.transport.client import BackendClient
from shelf_agent.transport.outbox import Outbox
from shelf_agent.trigger.interval import IntervalTrigger

# Opens the webcam shared by the shelves with one index; injectable for tests.
WebcamFactory = Callable[[int, WebcamSettings, Sequence[str]], CameraSource]

HELP = """\
  Enter        next frame from the next shelf (round-robin)
  <shelf>      next frame from that shelf, e.g. A1
  all          one frame from every shelf
  flush        retry queued frames now
  status       frames left / queued per shelf
  help         this text
  q            quit"""


class Emulator:
    """Drives several emulated shelves against a backend.

    Two modes: :meth:`run_auto` fires every shelf on a timer (the same
    trigger loop the device runs), :meth:`run_interactive` lets the presenter
    decide when each shelf "sees" a change.

    Args:
        scenario: Loaded scenario.
        transport: Custom httpx transport for every client (tests).
        output: Line printer for the interactive console.
        webcam_factory: Opens a webcam; :class:`WebcamSource` by default.
    """

    def __init__(
        self,
        scenario: Scenario,
        *,
        transport: httpx.BaseTransport | None = None,
        output: Callable[[str], None] = print,
        webcam_factory: WebcamFactory | None = None,
    ) -> None:
        self._scenario = scenario
        self._output = output
        self._sources: dict[str, CameraSource] = {}
        self._clients: dict[str, BackendClient] = {}
        self._agents: dict[str, ShelfAgent] = {}
        webcams = _open_webcams(scenario, webcam_factory or _open_webcam)
        for shelf in scenario.shelves:
            source: CameraSource
            if shelf.webcam is not None:
                source = webcams[shelf.webcam]
            else:
                source = FolderSource(
                    shelf.frames, labels=scenario.labels, loop=shelf.loop
                )
            client = BackendClient(
                scenario.backend, shelf.identity, transport=transport
            )
            self._sources[shelf.name] = source
            self._clients[shelf.name] = client
            self._agents[shelf.name] = ShelfAgent(
                name=shelf.name,
                camera=source,
                client=client,
                outbox=Outbox(scenario.outbox_dir / shelf.name),
            )
        self._cursor = 0

    def connect(self) -> None:
        """Look every shelf's device up in the backend (registering it if allowed).

        Raises:
            RegistrationError: A device contradicts the scenario.
            BackendError: The backend refused a request or is unreachable for a
                shelf without a configured ``device_id``.
        """
        never = threading.Event()
        for name, client in self._clients.items():
            connect(
                client,
                register=self._scenario.register_devices,
                stop=never,
                retry_s=0,
                wait=False,
            )
            identity = client.identity
            self._output(
                f"  {name:<8} device key '{identity.device_key}' -> "
                f"device id {identity.device_id}, shelf {identity.shelf_id}"
            )

    @property
    def shelf_names(self) -> list[str]:
        return list(self._agents)

    def step(self, name: str) -> TickResult | None:
        """Send the next frame of one shelf; ``None`` if it has no frames left.

        Round-robin (:meth:`step_next`) continues with the shelf after this one.
        """
        names = self.shelf_names
        self._cursor = (names.index(name) + 1) % len(names)
        try:
            return self._agents[name].tick()
        except SourceExhausted:
            return None

    def step_next(self) -> tuple[str, TickResult] | None:
        """Send the next frame of the next shelf that still has frames."""
        names = self.shelf_names
        for offset in range(len(names)):
            name = names[(self._cursor + offset) % len(names)]
            if self._has_frames(name):
                result = self.step(name)
                if result is not None:
                    return name, result
        return None

    def step_all(self) -> list[tuple[str, TickResult]]:
        """Send one frame from every shelf that still has frames."""
        results = []
        for name in self.shelf_names:
            result = self.step(name) if self._has_frames(name) else None
            if result is not None:
                results.append((name, result))
        return results

    def flush(self) -> int:
        """Retry queued frames of every shelf."""
        return sum(agent.flush() for agent in self._agents.values())

    def status_lines(self) -> list[str]:
        lines = []
        for shelf in self._scenario.shelves:
            source = self._sources[shelf.name]
            if not isinstance(source, FolderSource):
                left = f"webcam {shelf.webcam}"
            elif shelf.loop:
                left = "loop"
            else:
                left = f"{source.remaining}/{source.total} left"
            identity = self._clients[shelf.name].identity
            lines.append(
                f"  {shelf.name:<8} device={identity.device_id} "
                f"shelf={identity.shelf_id}  {left}, "
                f"{self._agents[shelf.name].queued} queued"
            )
        return lines

    def run_auto(self, stop: threading.Event, interval_s: float | None = None) -> None:
        """Every shelf captures on its own timer until all are exhausted or ``stop``."""
        interval = interval_s or self._scenario.interval_s
        workers = [
            threading.Thread(
                target=agent.run,
                args=(IntervalTrigger(interval), stop),
                name=f"shelf-{name}",
                daemon=True,
            )
            for name, agent in self._agents.items()
        ]
        with self._heartbeat(stop):
            for worker in workers:
                worker.start()
            for worker in workers:
                while worker.is_alive():
                    worker.join(timeout=0.5)

    def run_interactive(
        self, stop: threading.Event, input_fn: Callable[[str], str] = input
    ) -> None:
        """Presenter console: each command sends frames right away."""
        target = self._scenario.backend.base_url
        self._output(f"Emulating {len(self._agents)} shelves -> {target}")
        self._output(HELP)
        with self._heartbeat(stop):
            while not stop.is_set():
                try:
                    command = input_fn("emulator> ").strip()
                except EOFError:
                    return
                if not self._handle(command):
                    return

    def close(self) -> None:
        for agent in self._agents.values():
            agent.close()

    def _handle(self, command: str) -> bool:
        try:
            return self._dispatch(command)
        except CameraError as exc:
            self._output(f"  camera error: {exc}")
            return True

    def _dispatch(self, command: str) -> bool:
        lowered = command.lower()
        if lowered in {"q", "quit", "exit"}:
            return False
        if lowered in {"help", "?"}:
            self._output(HELP)
        elif lowered == "status":
            for line in self.status_lines():
                self._output(line)
        elif lowered == "flush":
            self._output(f"  delivered {self.flush()} queued frame(s)")
        elif lowered == "all":
            for name, result in self.step_all():
                self._output(describe(name, result))
        elif lowered == "":
            stepped = self.step_next()
            if stepped is None:
                self._output("  all shelves are out of frames (status / q)")
            else:
                self._output(describe(*stepped))
        elif command in self._agents:
            single = self.step(command)
            if single is None:
                self._output(f"  {command}: no frames left")
            else:
                self._output(describe(command, single))
        else:
            self._output(f"  unknown command or shelf: {command!r} (help)")
        return True

    def _has_frames(self, name: str) -> bool:
        source = self._sources[name]
        if not isinstance(source, FolderSource):
            return True
        return self._scenario.shelf(name).loop or source.remaining > 0

    def _heartbeat(self, stop: threading.Event) -> _HeartbeatThread:
        return _HeartbeatThread(
            list(self._agents.values()), self._scenario.heartbeat_interval_s, stop
        )


def _open_webcams(
    scenario: Scenario, factory: WebcamFactory
) -> dict[int, CameraSource]:
    """Open each webcam once: shelves on the same index share the camera,
    and each still sends its own frames as its own device."""
    by_index: dict[int, list[EmulatedShelf]] = {}
    for shelf in scenario.shelves:
        if shelf.webcam is not None:
            by_index.setdefault(shelf.webcam, []).append(shelf)
    return {
        index: factory(index, scenario.webcam, [s.name for s in shelves])
        for index, shelves in by_index.items()
    }


def _open_webcam(
    index: int, settings: WebcamSettings, shelf_names: Sequence[str]
) -> CameraSource:
    from shelf_agent.camera.webcam import WebcamSource

    return WebcamSource(
        index,
        settings.capture_dir,
        width=settings.width,
        height=settings.height,
        preview=settings.preview,
        title=f"webcam {index}: shelf {', '.join(shelf_names)}",
    )


def describe(name: str, result: TickResult) -> str:
    """One console line per delivered frame, with the expected label if known."""
    frame = result.frame
    if result.delivery is Delivery.SENT and result.receipt is not None:
        outcome = f"sent, imageEvent #{result.receipt.image_event_id}"
        if result.receipt.quantity is not None:
            outcome += f", recognised x{result.receipt.quantity}"
    elif result.delivery is Delivery.QUEUED:
        outcome = "queued (backend unreachable, will retry)"
    else:
        outcome = f"REJECTED: {result.error}"
    truth = frame.ground_truth
    expected = f" | expected {truth.sku} x{truth.count}" if truth else ""
    return f"  [{name}] {frame.filename} -> {outcome}{expected}"


class _HeartbeatThread:
    """Context manager running :func:`run_heartbeat` in the background."""

    def __init__(
        self, agents: list[ShelfAgent], interval_s: float, stop: threading.Event
    ) -> None:
        self._own_stop = threading.Event()
        self._outer_stop = stop
        self._thread = threading.Thread(
            target=run_heartbeat,
            args=(agents, interval_s, self._own_stop),
            name="heartbeat",
            daemon=True,
        )

    def __enter__(self) -> _HeartbeatThread:
        self._thread.start()
        return self

    def __exit__(self, *exc_info: object) -> None:
        self._own_stop.set()
        self._outer_stop.set()
        self._thread.join(timeout=5)
