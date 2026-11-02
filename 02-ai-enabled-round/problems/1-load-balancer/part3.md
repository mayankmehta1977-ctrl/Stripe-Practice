# Load balancer: Part 3

Some services keep per-user state, so we want **sticky sessions**.

When a connection has a `userId`, send it to the server that user's most
recent connection went to, **as long as that server has room**. If it is
full, fall back to the normal Part 2 rule, and that new server becomes the
user's sticky server.

Connections with a null `userId` use the Part 2 rule.

Example: capacities `a=3, b=3`. alice goes to `a`, bob to `b`, then alice's
second connection goes to `a`, even though `b` is less loaded.
