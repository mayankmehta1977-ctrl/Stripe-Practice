# Shipping routes: Part 2

Allow routes with **at most one intermediate stop**.

Return the cheapest route from A to B that is either direct or goes through
exactly one other country, along with the legs it uses (each leg's from, to,
carrier and cost).

Example: `US -> UK` is now `10` via `US -(FedEx)-> CA -(DHL)-> UK`.
