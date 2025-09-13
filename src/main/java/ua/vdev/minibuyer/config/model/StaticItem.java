package ua.vdev.minibuyer.config.model;

import ua.vdev.minibuyer.item.SellableItem;

import java.util.List;

public record StaticItem(
        int slot,
        SellableItem item,
        List<String> lore
) {}