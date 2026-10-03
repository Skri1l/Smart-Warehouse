"""Stock helpers with no Qt or HTTP dependencies."""

from collections.abc import Iterable

from app.models.dto import Product


def is_low_stock(product: Product) -> bool:
    """Return True when the product is at or below its minimum quantity."""
    return product.quantity <= product.min_quantity


def filter_products(products: Iterable[Product], query: str) -> list[Product]:
    """Return products whose SKU or name contains ``query`` (case-insensitive)."""
    needle = query.strip().lower()
    if not needle:
        return list(products)
    return [p for p in products if needle in p.sku.lower() or needle in p.name.lower()]
