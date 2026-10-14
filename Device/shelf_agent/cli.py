"""Command line: ``run`` (real device), ``emulate`` (PC) and ``mock-server``.

    python -m shelf_agent run --config config/device.toml
    python -m shelf_agent emulate --scenario scenarios/demo/scenario.toml
    python -m shelf_agent mock-server --port 8080
"""

from __future__ import annotations

import argparse
import logging
import signal
import threading
from dataclasses import replace
from pathlib import Path

from shelf_agent import __version__
from shelf_agent.config import ConfigError, load_device_config
from shelf_agent.registration import RegistrationError
from shelf_agent.transport.client import BackendError

logger = logging.getLogger("shelf_agent")


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    # The interactive console prints its own line per frame; agent INFO logs
    # would only duplicate it.
    quiet = args.command == "emulate" and not args.auto
    _configure_logging(args.verbose, quiet=quiet)
    try:
        result: int = args.handler(args)
        return result
    except (ConfigError, RegistrationError) as exc:
        logger.error("%s", exc)
        return 2
    except BackendError as exc:
        logger.error("backend: %s", exc)
        return 3
    except KeyboardInterrupt:
        return 130


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="shelf-agent", description="Smart Warehouse shelf agent and emulator"
    )
    parser.add_argument("--version", action="version", version=__version__)
    parser.add_argument(
        "-v", "--verbose", action="count", default=0, help="-v info, -vv debug"
    )
    commands = parser.add_subparsers(dest="command", required=True)

    run = commands.add_parser("run", help="run on a real shelf device")
    run.add_argument("--config", type=Path, required=True, help="device TOML config")
    run.set_defaults(handler=_cmd_run)

    emulate = commands.add_parser("emulate", help="emulate shelves from test images")
    emulate.add_argument("--scenario", type=Path, required=True, help="scenario TOML")
    emulate.add_argument(
        "--auto",
        action="store_true",
        help="every shelf sends on a timer instead of the interactive console",
    )
    emulate.add_argument(
        "--interval", type=float, help="seconds between frames (--auto)"
    )
    emulate.add_argument("--base-url", help="override [backend] base_url")
    emulate.add_argument(
        "--only", nargs="+", metavar="SHELF", help="emulate only these shelves"
    )
    emulate.add_argument(
        "--no-register",
        action="store_true",
        help="fail instead of creating missing devices with POST /devices",
    )
    emulate.add_argument(
        "--reset-outbox",
        action="store_true",
        help="drop frames left undelivered by a previous run",
    )
    emulate.set_defaults(handler=_cmd_emulate)

    mock = commands.add_parser("mock-server", help="fake backend device endpoints")
    mock.add_argument("--host", default="127.0.0.1")
    mock.add_argument("--port", type=int, default=8080)
    mock.add_argument(
        "--out", type=Path, default=Path("received"), help="where to save images"
    )
    mock.set_defaults(handler=_cmd_mock_server)
    return parser


def _cmd_run(args: argparse.Namespace) -> int:
    from shelf_agent.device import run_device

    config = load_device_config(args.config)
    stop = threading.Event()
    _stop_on_sigterm(stop)
    run_device(config, stop)
    return 0


def _cmd_emulate(args: argparse.Namespace) -> int:
    import shutil

    from shelf_agent.emulator.runner import Emulator
    from shelf_agent.emulator.scenario import load_scenario

    scenario = load_scenario(args.scenario)
    if args.base_url:
        backend = replace(scenario.backend, base_url=args.base_url.rstrip("/"))
        scenario = replace(scenario, backend=backend)
    if args.only:
        unknown = set(args.only) - {shelf.name for shelf in scenario.shelves}
        if unknown:
            raise ConfigError(f"unknown shelf(s): {sorted(unknown)}")
        shelves = tuple(s for s in scenario.shelves if s.name in args.only)
        scenario = replace(scenario, shelves=shelves)
    if args.no_register:
        scenario = replace(scenario, register_devices=False)
    if args.reset_outbox:
        shutil.rmtree(scenario.outbox_dir, ignore_errors=True)

    emulator = Emulator(scenario)
    stop = threading.Event()
    _stop_on_sigterm(stop)
    try:
        emulator.connect()
        if args.auto:
            emulator.run_auto(stop, args.interval)
        else:
            emulator.run_interactive(stop)
    finally:
        emulator.close()
    return 0


def _cmd_mock_server(args: argparse.Namespace) -> int:
    from shelf_agent.mock_server import MockBackend

    try:
        server = MockBackend((args.host, args.port), args.out)
    except OSError as exc:
        logger.error(
            "cannot listen on %s:%s (%s). Is the backend or another mock already "
            "running there? Use --port",
            args.host,
            args.port,
            exc.strerror or exc,
        )
        return 3
    logger.warning(
        "mock backend on %s, saving images to %s", server.base_url, args.out.resolve()
    )
    try:
        server.serve_forever()
    finally:
        server.server_close()
        logger.warning(
            "received %d image event(s), %d heartbeat(s)",
            len(server.events),
            len(server.heartbeats),
        )
    return 0


def _configure_logging(verbose: int, *, quiet: bool) -> None:
    level = logging.DEBUG if verbose >= 2 else logging.INFO
    if verbose == 0 and quiet:
        level = logging.WARNING
    logging.basicConfig(
        level=level,
        format="%(asctime)s %(levelname)-7s %(name)s: %(message)s",
        datefmt="%H:%M:%S",
    )
    logging.getLogger("httpx").setLevel(logging.WARNING)


def _stop_on_sigterm(stop: threading.Event) -> None:
    """systemd stops the service with SIGTERM; finish the current upload, then exit."""

    def handler(signum: int, frame: object) -> None:
        stop.set()

    signal.signal(signal.SIGTERM, handler)
