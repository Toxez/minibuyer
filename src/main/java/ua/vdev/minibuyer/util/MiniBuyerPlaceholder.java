package ua.vdev.minibuyer.util;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;

import ua.vdev.minibuyer.MiniBuyer;

public class MiniBuyerPlaceholder extends PlaceholderExpansion {
    private final MiniBuyer plugin;

    public MiniBuyerPlaceholder(MiniBuyer plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "minibuyer";
    }

    @Override
    public String getAuthor() {
        return "Tox_8729";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "--";
        }

        if (identifier.equals("update_seller")) {
            return plugin.getAssortmentUpdater().getTimePlaceholder();
        }

        if (identifier.equals("items_sold")) {
            return String.valueOf(plugin.getLevelManager().getItemsSold(player));
        }

        return null;
    }
}