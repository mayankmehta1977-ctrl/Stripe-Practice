# Load balancer: Part 2

Servers now have a **capacity**: the maximum number of open connections each
can hold. The balancer is created with server ids and capacities, in order.

Change routing to **least connections**: send each new connection to the
server with the fewest open connections that still has room. Break ties by
server order. If every server is full, `connect` returns `"REJECTED"`.

Add:

```java
void disconnect(String connectionId)
```

which closes a connection and frees its slot. Disconnect messages can arrive
twice; a second disconnect for the same id should do nothing.

Example: capacities `a=1, b=1`. Connections 1 and 2 go to `a` and `b`;
connection 3 is `REJECTED`; after `disconnect(1)`, connection 4 goes to `a`.
