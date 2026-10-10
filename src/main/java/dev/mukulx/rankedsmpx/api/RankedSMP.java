package dev.mukulx.rankedsmpx.api;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for other plugins.
 *
 * <pre>{@code
 * RankedSMPApi api = RankedSMP.get();
 * OptionalInt rank = api.getRank(player.getUniqueId());
 * }</pre>
 *
 * Declare {@code depend: [RankedSMPX]} or {@code softdepend: [RankedSMPX]} in your plugin so it
 * loads after RankedSMPX.
 */
public final class RankedSMP {

  private RankedSMP() {}

  /**
   * @throws IllegalStateException if RankedSMPX is not installed or not enabled
   */
  @NotNull
  public static RankedSMPApi get() {
    return find().orElseThrow(() -> new IllegalStateException("RankedSMPX is not enabled"));
  }

  /** The API, or empty if RankedSMPX is not installed or not enabled. */
  @NotNull
  public static Optional<RankedSMPApi> find() {
    RegisteredServiceProvider<RankedSMPApi> provider =
        Bukkit.getServicesManager().getRegistration(RankedSMPApi.class);
    return provider == null ? Optional.empty() : Optional.of(provider.getProvider());
  }
}
