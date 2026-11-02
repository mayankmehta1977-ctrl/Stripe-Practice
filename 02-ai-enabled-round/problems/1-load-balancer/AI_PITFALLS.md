# Load balancer: what AI agents tend to get wrong

Read after practicing. These are the things to catch when reviewing the
agent's code, and to mention out loud when you catch them.

- **Over-engineering.** Agents love a `RoutingStrategy` interface, a factory,
  and a class per strategy. For four parts in 45 minutes, one class with a
  mode is clearer. Push back: "Let's keep this in one class until we need more."
- **Losing server order.** `HashMap` iteration order is random, which breaks
  "ties go to the first server." Needs `LinkedHashMap` or a list.
- **Load going negative** when the same disconnect arrives twice.
- **Sticky server that's full** still being chosen, or the sticky mapping not
  updated after falling back.
- **Shutdown re-routing to the dead server**, because the server is removed
  after re-routing instead of before.
- **Re-routing order.** Agents often iterate a `HashMap` of connections, so
  the "original open order" requirement is silently violated.
- **Sticky entries pointing at a removed server** left behind after shutdown.
- **Tests that just mirror the implementation** (asserting whatever the code
  returns) instead of hand-computed expected values.
