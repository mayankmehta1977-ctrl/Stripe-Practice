# Shipping routes: Part 4

Customers can now add constraints:

1. **Excluded carriers**: never use these carriers.
2. **Maximum legs**: the route may use at most K legs.

Return the cheapest route that satisfies both, or none.

Examples with the Part 3 input:
- `US -> DE`, max 3 legs, no exclusions: `6`
- `US -> DE`, max 2 legs: `15` (`US -> CA -> DE`)
- `US -> DE`, excluding FedEx: `19` (`US -> UK -> DE`)
