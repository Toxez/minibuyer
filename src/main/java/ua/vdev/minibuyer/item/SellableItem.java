package ua.vdev.minibuyer.item;

import org.bukkit.Material;

public record SellableItem(
        Material material,
        String name,
        int price,
        int amount
) {}