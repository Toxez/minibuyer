package ua.vdev.minibuyer.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;

@Getter
@AllArgsConstructor
public class SellableItem {
    private final Material material;
    private final String translation;
    private final int price;
    private final int amount;
}