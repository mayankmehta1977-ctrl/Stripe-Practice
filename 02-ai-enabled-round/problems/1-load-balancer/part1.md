# Load balancer: Part 1

Stripe routes incoming connections across a pool of servers.

Write a `LoadBalancer` that is created with an ordered list of server ids and
has a method:

```java
String connect(String connectionId, String userId)
```

which returns the id of the server the connection is routed to.

For now, route connections **round robin**: the first connection goes to the
first server, the second to the second, and so on, wrapping around.

Example with servers `[a, b, c]`: four connections go to `a, b, c, a`.

Connection ids are unique. Treat a repeated connection id as an error.
