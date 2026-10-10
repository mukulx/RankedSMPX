package dev.mukulx.rankedsmpx.api;

import java.util.UUID;

/** A player together with the rank they hold. */
public record RankedPlayer(UUID uuid, int rank) {}
