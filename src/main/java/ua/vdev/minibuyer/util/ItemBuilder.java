package ua.vdev.minibuyer.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemBuilder {

    public static ItemStack build(Material material, int amount, String name, List<String> lore, Map<String, String> placeholders) {
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();

        if (name != null && !name.isEmpty()) {
            meta.displayName(TextUtil.mm("<reset>" + name, placeholders)
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream()
                    .map(line -> {
                        if (!placeholders.containsKey("amount") && amount > 1) {
                            Map<String, String> newPlaceholders = new java.util.HashMap<>(placeholders);
                            newPlaceholders.put("amount", String.valueOf(amount));
                            return TextUtil.mm("<reset>" + line, newPlaceholders)
                                    .decoration(TextDecoration.ITALIC, false);
                        }
                        return TextUtil.mm("<reset>" + line, placeholders)
                                .decoration(TextDecoration.ITALIC, false);
                    })
                    .collect(Collectors.toList()));
        }

        stack.setItemMeta(meta);
        return stack;
    }
}