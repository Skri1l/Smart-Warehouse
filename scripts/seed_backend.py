"""Create what the emulator scenarios need in a fresh backend database.

The scenarios expect shelves 2 (scenarios/demo) and 3 (scenarios/webcam),
each with exactly one Coca-Cola ShelfStock record; shelf 1 is kept for the
physical device. Safe to run again: existing rows are reused, never
duplicated. Standard library only.

    python3 scripts/seed_backend.py [--base-url http://127.0.0.1:8080]
"""

from __future__ import annotations

import argparse
import json
import sys
from typing import Any
from urllib.error import HTTPError
from urllib.request import Request, urlopen

SHELF_COUNT = 3
STOCKED_SHELVES = (2, 3)
SKU = "COCA-COLA"


def call(base_url: str, method: str, path: str, body: object = None) -> Any:
    data = None if body is None else json.dumps(body).encode()
    request = Request(
        base_url + path,
        data=data,
        method=method,
        headers={"Content-Type": "application/json"},
    )
    with urlopen(request, timeout=30) as response:
        raw = response.read()
    return json.loads(raw) if raw else None


def created_id(response: Any) -> int:
    """The backend answers a create with the id, bare or as ``{"id": ...}``."""
    return int(response["id"] if isinstance(response, dict) else response)


def seed(base_url: str) -> None:
    warehouses = call(base_url, "GET", "/warehouses")
    if warehouses:
        warehouse_id = warehouses[0]["id"]
    else:
        warehouse_id = created_id(
            call(
                base_url,
                "POST",
                "/warehouses",
                {"name": "Main warehouse", "address": "Minsk"},
            )
        )
        print(f"created warehouse {warehouse_id}")

    racks = call(base_url, "GET", "/racks")
    if racks:
        rack_id = racks[0]["id"]
    else:
        rack_id = created_id(
            call(
                base_url,
                "POST",
                f"/racks/warehouse/{warehouse_id}",
                {"label": "A", "warehouseId": warehouse_id},
            )
        )
        print(f"created rack {rack_id}")

    shelf_ids = {shelf["id"] for shelf in call(base_url, "GET", "/shelves")}
    while len(shelf_ids) < SHELF_COUNT:
        label = f"A-{len(shelf_ids) + 1}"
        shelf_id = created_id(
            call(
                base_url,
                "POST",
                "/shelves",
                {"label": label, "capacity": 50, "rackId": rack_id},
            )
        )
        shelf_ids.add(shelf_id)
        print(f"created shelf {shelf_id} ({label})")
    missing = [shelf for shelf in STOCKED_SHELVES if shelf not in shelf_ids]
    if missing:
        sys.exit(
            f"shelves {missing} are not in this database (it has {sorted(shelf_ids)}); "
            "set shelf_id in the scenarios or reset the database"
        )

    try:
        product_id = call(base_url, "GET", f"/products/sku/{SKU}")["id"]
    except HTTPError as exc:
        if exc.code != 404:
            raise
        product_id = created_id(
            call(
                base_url,
                "POST",
                "/products",
                {"sku": SKU, "name": "Coca-Cola can 0.33 l"},
            )
        )
        print(f"created product {product_id} ({SKU})")

    for shelf_id in STOCKED_SHELVES:
        stock = call(base_url, "GET", f"/shelf-stock/shelf/{shelf_id}")
        if stock:
            continue
        stock_id = created_id(
            call(
                base_url,
                "POST",
                "/shelf-stock",
                {
                    "shelfId": shelf_id,
                    "productId": product_id,
                    "quantity": 1,
                    "expirationDate": "2030-12-31",
                },
            )
        )
        print(f"created shelf stock {stock_id} on shelf {shelf_id}")

    print(
        f"ready: warehouse {warehouse_id}, rack {rack_id}, shelves {sorted(shelf_ids)}, "
        f"{SKU} stocked on shelves {list(STOCKED_SHELVES)}"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    args = parser.parse_args()
    try:
        seed(args.base_url.rstrip("/"))
    except HTTPError as exc:
        sys.exit(f"{exc.code} {exc.reason}: {exc.read().decode(errors='replace')}")


if __name__ == "__main__":
    main()
