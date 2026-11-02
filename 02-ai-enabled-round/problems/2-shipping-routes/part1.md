# Shipping routes: Part 1

You're given shipping lanes as a single string:

```
"US:UK:UPS:20,US:CA:FedEx:3,CA:UK:DHL:7,US:UK:DHL:15"
```

Each entry is `from:to:carrier:cost`. Lanes are **one-directional**: `US:UK`
does not mean you can ship `UK` to `US`. There may be several carriers on the
same lane.

Write a class that parses this input and answers: **what is the cheapest
direct shipment from one country to another?** Return the cost and the
carrier, or indicate that there is no direct lane.

Example: `US -> UK` direct is `15` with `DHL`.

Malformed entries should produce a clear error.
