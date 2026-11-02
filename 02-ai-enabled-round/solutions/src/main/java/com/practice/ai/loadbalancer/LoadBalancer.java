package com.practice.ai.loadbalancer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reference solution for the progressive load-balancer problem.
 *
 * Part 1: round robin across servers.
 * Part 2: servers have a capacity; route to the least-loaded server with room,
 *         reject when every server is full. Connections can disconnect.
 * Part 3: sticky sessions - a user's connections go to the server they used
 *         last, while it has room.
 * Part 4: a server can shut down; its open connections are re-routed in the
 *         order they were originally opened.
 *
 * Design note: one class with a simple mode switch is enough here. A
 * strategy-pattern hierarchy is the kind of over-engineering an interviewer
 * expects you to push back on when an AI agent proposes it.
 */
public class LoadBalancer {

    public static final String REJECTED = "REJECTED";

    public enum Mode { ROUND_ROBIN, LEAST_CONNECTIONS }

    private record Connection(String id, String userId, String server) {}

    private final Mode mode;
    private final Map<String, Integer> capacity = new LinkedHashMap<>(); // keeps server order
    private final Map<String, Integer> load = new HashMap<>();
    private final Map<String, Connection> open = new LinkedHashMap<>();  // keeps open order
    private final Map<String, String> stickyServer = new HashMap<>();
    private int roundRobinNext = 0;

    /** Part 1 constructor: no capacities, round robin. */
    public LoadBalancer(List<String> servers) {
        this.mode = Mode.ROUND_ROBIN;
        for (String s : servers) {
            capacity.put(s, Integer.MAX_VALUE);
            load.put(s, 0);
        }
    }

    /** Part 2+ constructor: capacities in server order, least connections. */
    public LoadBalancer(Map<String, Integer> capacities) {
        this.mode = Mode.LEAST_CONNECTIONS;
        capacities.forEach((s, c) -> {
            if (c < 0) throw new IllegalArgumentException("negative capacity for " + s);
            capacity.put(s, c);
            load.put(s, 0);
        });
    }

    /** Routes a new connection. Returns the server id, or REJECTED. */
    public String connect(String connectionId, String userId) {
        if (open.containsKey(connectionId)) {
            throw new IllegalArgumentException("duplicate connection id " + connectionId);
        }
        Optional<String> server = pickServer(userId);
        if (server.isEmpty()) {
            return REJECTED;
        }
        String s = server.get();
        open.put(connectionId, new Connection(connectionId, userId, s));
        load.merge(s, 1, Integer::sum);
        if (userId != null) {
            stickyServer.put(userId, s);
        }
        return s;
    }

    /** Closes a connection. Unknown ids are ignored (disconnects can arrive twice). */
    public void disconnect(String connectionId) {
        Connection c = open.remove(connectionId);
        if (c != null) {
            load.merge(c.server(), -1, Integer::sum);
        }
    }

    /**
     * Part 4: removes a server and re-routes its open connections, in the
     * order they were opened. Returns connection id -> new server or REJECTED.
     */
    public Map<String, String> shutdown(String serverId) {
        if (!capacity.containsKey(serverId)) {
            throw new IllegalArgumentException("unknown server " + serverId);
        }
        List<Connection> moved = new ArrayList<>();
        for (Connection c : open.values()) {
            if (c.server().equals(serverId)) moved.add(c);
        }
        moved.forEach(c -> open.remove(c.id()));
        capacity.remove(serverId);
        load.remove(serverId);
        stickyServer.values().removeIf(serverId::equals);

        Map<String, String> result = new LinkedHashMap<>();
        for (Connection c : moved) {
            result.put(c.id(), connect(c.id(), c.userId()));
        }
        return result;
    }

    public int load(String serverId) {
        return load.getOrDefault(serverId, 0);
    }

    // ---- routing ---------------------------------------------------------

    private Optional<String> pickServer(String userId) {
        if (capacity.isEmpty()) return Optional.empty();
        if (userId != null) {                                      // Part 3
            String preferred = stickyServer.get(userId);
            if (preferred != null && hasRoom(preferred)) return Optional.of(preferred);
        }
        return mode == Mode.ROUND_ROBIN ? nextRoundRobin() : leastLoaded();
    }

    private Optional<String> nextRoundRobin() {
        List<String> servers = new ArrayList<>(capacity.keySet());
        String s = servers.get(roundRobinNext % servers.size());
        roundRobinNext++;
        return Optional.of(s);
    }

    private Optional<String> leastLoaded() {
        String best = null;
        for (String s : capacity.keySet()) {              // first in order wins ties
            if (hasRoom(s) && (best == null || load.get(s) < load.get(best))) {
                best = s;
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean hasRoom(String s) {
        return capacity.containsKey(s) && load.get(s) < capacity.get(s);
    }
}
