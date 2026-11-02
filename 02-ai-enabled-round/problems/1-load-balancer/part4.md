# Load balancer: Part 4

Servers can be taken out of service. Add:

```java
Map<String, String> shutdown(String serverId)
```

It removes the server, then re-routes each of its open connections **in the
order they were originally opened**, using the normal routing rules (Parts 2
and 3). It returns a map of connection id to its new server, or `"REJECTED"`
if there was no room anywhere.

A user whose sticky server was shut down has no sticky server any more.
Shutting down an unknown server is an error.
