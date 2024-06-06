package be.isach.ultracosmetics.menu.buttons;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.cosmetics.type.CosmeticType;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.util.ItemFactory;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CosmeticNoPermissionButton extends CosmeticButton {
    private final String name;
    private final List<String> lore = new ArrayList<>();
    private final ItemStack stack = ItemFactory.getItemStackFromConfig("No-Permission.Custom-Item.Type");

    public CosmeticNoPermissionButton(UltraCosmetics ultraCosmetics, CosmeticType<?> cosmeticType) {
        super(ultraCosmetics, cosmeticType, true);
        MiniMessage mm = MessageManager.getMiniMessage();
        String rawName = SettingsManager.getConfig().getString("No-Permission.Custom-Item.Name", "");
        this.name = MessageManager.toLegacy(mm.deserialize(rawName, Placeholder.component("cosmetic", cosmeticType.getName())));
        List<String> rawLore;
        if (SettingsManager.getConfig().isList("No-Permission.Custom-Item.Lore")) {
            rawLore = SettingsManager.getConfig().getStringList("No-Permission.Custom-Item.Lore");
        } else {
            rawLore = Arrays.asList(SettingsManager.getConfig().getString("No-Permission.Custom-Item.Lore", "").split("\n"));
        }
        for (String item : rawLore) {
            lore.add(MessageManager.toLegacy(mm.deserialize(item)));
        }
    }

    @Override
    public ItemStack getBaseItem(UltraPlayer ultraPlayer) {
        ItemStack stack = this.stack.clone();
        ItemFactory.rename(stack, name);
        ItemMeta meta = stack.getItemMeta();
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
