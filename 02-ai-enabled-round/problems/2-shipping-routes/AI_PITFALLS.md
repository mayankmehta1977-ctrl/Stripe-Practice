# Shipping routes: what AI agents tend to get wrong

- **Treating lanes as two-way.** Agents often add reverse edges "for
  convenience." The spec says one-directional.
- **Keeping only one carrier per lane** by putting edges in a
  `Map<from, Map<to, cost>>`, which silently overwrites cheaper carriers.
- **Part 2 routes that loop**, like `US -> US -> UK`, or returning a 1-stop
  route when the direct one is cheaper.
- **Dijkstra without handling stale queue entries**, or stopping too early.
- **Part 4 with Dijkstra plus a visited set.** This is the big one. With a
  leg limit, the cheapest way to reach a midpoint may use too many legs, so
  "visited" prunes the only valid route. The fix is a leg-bounded search
  (Bellman-Ford for K rounds, or a state of (country, legs used)). Ask the
  agent: "Is Dijkstra still correct with a hop limit?" and judge its answer.
- **Negative costs** not validated, which would make Dijkstra wrong.
- **Integer overflow** from using `Integer.MAX_VALUE` as infinity and then
  adding to it.
