package ua.vdev.minibuyer.config.model;

import org.bukkit.Material;

import java.util.List;

public record Decoration(
        List<Integer> slots,
        Material material,
        String name,
        List<String> lore,
        List<String> leftClickCommands,
        List<String> rightClickCommands
) {}