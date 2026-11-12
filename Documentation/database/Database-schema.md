# Database schema

## General scheme

The diagram shows how the database organizes warehouse inventory, product
recognition, stock movements, demand planning, and procurement.

![img_1.png](img_1.png)

## Warehouses and remnants

A warehouse contains racks, and each rack contains shelves. Stock records track
the quantity of each product on a shelf.

![img_2.png](img_2.png)

## Product recognition and movement

Image events record photos received from shelf devices. Recognition results
store the outcome of processing those images, while stock movements record
changes in inventory.

![img_3.png](img_3.png)

## Planning and Procurement


Demand plans record expected product needs. Replenishment recommendations
suggest quantities to order, and purchase orders record planned purchases.

![img_4.png](img_4.png)

