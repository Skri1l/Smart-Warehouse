"""Print where an emulator scenario's photos will land and where to look.

For every shelf of the scenario: warehouse, rack and shelf label, the
device registered for its key (or that the emulator will register it),
the current stock, and links to Swagger and to the raw JSON endpoints.
Standard library only (``tomllib`` needs Python 3.11+).

    python3 scripts/pipeline_info.py Device/scenarios/webcam/scenario.toml
"""

from __future__ import annotations

import argparse
import json
import tomllib
from pathlib import Path
from typing import Any
from urllib.error import URLError
from urllib.request import urlopen

BOLD, DIM, RESET = "\033[1m", "\033[2m", "\033[0m"


def get(base_url: str, path: str) -> Any:
    try:
        with urlopen(base_url + path, timeout=10) as response:
            return json.load(response)
    except (URLError, ValueError):
        return None


def describe_shelf(base_url: str, name: str, shelf_id: int, device_key: str) -> None:
    shelf = get(base_url, f"/shelves/{shelf_id}")
    if shelf is None:
        print(f"  {BOLD}{name}{RESET}: shelf {shelf_id} does NOT exist in the backend")
        print("      run: scripts/pipeline.sh seed")
        return
    rack = get(base_url, f"/racks/{shelf['rackId']}") or {}
    warehouse = get(base_url, f"/warehouses/{rack.get('warehouseId')}") or {}
    devices = get(base_url, "/devices") or []
    device = next((d for d in devices if d["deviceKey"] == device_key), None)
    stock = get(base_url, f"/shelf-stock/shelf/{shelf_id}") or []

    print(
        f"  {BOLD}{name}{RESET} -> shelf {BOLD}#{shelf_id} {shelf['label']}{RESET}"
        f"  (rack {rack.get('label', '?')}, {warehouse.get('name', '?')},"
        f" {warehouse.get('address', '?')})"
    )
    if device is None:
        print(
            f"      device   '{device_key}': not registered yet, the emulator registers it"
        )
    else:
        print(
            f"      device   '{device_key}' = device #{device['id']} on shelf"
            f" {device['shelfId']}, {device['status']}"
        )
    if device is not None and device["shelfId"] != shelf_id:
        print(
            f"      WARNING: the device is linked to shelf {device['shelfId']},"
            " the emulator will stop; run scripts/pipeline.sh reset"
        )
    for row in stock:
        product = get(base_url, f"/products/{row['productId']}") or {}
        print(
            f"      stock    {product.get('sku', row['productId'])}:"
            f" quantity {BOLD}{row['quantity']}{RESET}"
        )
    print(f"      {DIM}quantity  {base_url}/shelf-stock/shelf/{shelf_id}{RESET}")
    print(f"      {DIM}history   {base_url}/stock-movements/shelf/{shelf_id}{RESET}")
    if device is not None:
        print(
            f"      {DIM}photos    {base_url}/image-events/device/{device['id']}{RESET}"
        )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("scenario", type=Path)
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--vision-url", default="http://127.0.0.1:8000")
    parser.add_argument("--webcam", type=int, help="camera index in use, if any")
    args = parser.parse_args()
    base_url = args.base_url.rstrip("/")
    scenario = tomllib.loads(args.scenario.read_text(encoding="utf-8"))

    print()
    print(f"{BOLD}Smart Warehouse pipeline{RESET}")
    print(f"  Backend Swagger  {base_url}/swagger-ui.html")
    print(f"  Vision Swagger   {args.vision_url.rstrip('/')}/docs")
    print(f"  Counts per photo {base_url}/recognition-results")
    print(f"  All devices      {base_url}/devices")
    print()
    source = f"webcam {args.webcam}" if args.webcam is not None else "photos from disk"
    scenario_name = f"{args.scenario.parent.parent.name}/{args.scenario.parent.name}"
    print(f"{BOLD}Shelves of {scenario_name}{RESET} ({source}):")
    for shelf in scenario.get("shelves", []):
        describe_shelf(base_url, shelf["name"], shelf["shelf_id"], shelf["device_key"])
    print()


if __name__ == "__main__":
    main()
