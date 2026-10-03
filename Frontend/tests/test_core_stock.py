from app.core.stock import filter_products, is_low_stock
from app.models.dto import Product

MILK = Product(1, "SKU-1", "Milk", "Dairy", "pcs", 5, 10)
RICE = Product(2, "SKU-2", "Rice", "Grocery", "pcs", 50, 10)


def test_is_low_stock_when_quantity_below_minimum() -> None:
    assert is_low_stock(MILK)
    assert not is_low_stock(RICE)


def test_filter_products_matches_sku_or_name_case_insensitive() -> None:
    assert filter_products([MILK, RICE], "milk") == [MILK]
    assert filter_products([MILK, RICE], "sku-2") == [RICE]
    assert filter_products([MILK, RICE], "  ") == [MILK, RICE]
