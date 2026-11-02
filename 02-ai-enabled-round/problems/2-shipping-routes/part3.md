# Shipping routes: Part 3

Allow **any number of stops**. Return the cheapest route and its legs.

Use this larger input for testing:

```
"US:UK:UPS:20, US:CA:FedEx:3, CA:UK:DHL:7, US:UK:DHL:15, UK:DE:DHL:4,
 CA:FR:UPS:2, FR:DE:FedEx:1, CA:DE:UPS:12"
```

Example: `US -> DE` is `6` via `US -> CA -> FR -> DE`.

A route from a country to itself costs 0.
