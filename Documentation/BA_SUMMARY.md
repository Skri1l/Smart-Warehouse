# Summary of Answers for the BA

## Vision, Problem, Project

### Vision
A warehouse that runs autonomously, like a single factory with no people.

### Problem
The accounting department is doing work that isn't theirs: counting products and forecasting purchases.
Instead of putting products straight into storage, warehouse workers have to register and count them first.

### Business goals and objectives
Increase employee efficiency so that everyone focuses on their actual job rather than on registration tasks, and thereby increase the revenue of warehouse companies.

### Key stakeholders
- Warehouse companies.
- Employees, who benefit from less tedious and uninteresting work.

### In-Scope
To be updated based on what we manage to deliver.

### Out-of-Scope
To be updated based on what we manage to deliver.

## High-Level Solution Overview

### Proposed Solution
- An inventory accounting and warehouse management system for small and medium-sized warehouses
- Automates receiving, storage, movement, and shipping of goods
- Forecasts demand and suggests when to replenish stock
- A cross-platform desktop client for employees and a central server with a database

### Architecture / Integration Landscape

- Desktop Client (Python) -> Backend (Java)
Backend <-> PostgreSQL
Backend <-> ML Service (Python) (demand forecasting, replenishment recommendations)

## Features & Requirements

### Core Features
- Product catalog management
- Warehouse operations (receiving / movement / shipping)
- Tracking of stock levels and storage locations
- Demand forecasting and replenishment recommendations
- Reports and analytics
- Users, roles, and access control

### Use Case Diagram
- Worker: handles products (receiving, shipping)
- Manager: views stock levels, gets forecasts, generates reports
- Administrator: manages users and roles
