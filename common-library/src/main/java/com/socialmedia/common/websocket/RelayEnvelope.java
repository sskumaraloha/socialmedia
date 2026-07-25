package com.socialmedia.common.websocket;

/**
 * Wire format for RedisWebSocketRelay - carries a user-targeted STOMP notification
 * produced on one node to every node in the cluster, so a client connected to a
 * different node than the one that produced the event still receives it.
 */
public record RelayEnvelope(String targetUserId, String destination, Object payload) {
}
