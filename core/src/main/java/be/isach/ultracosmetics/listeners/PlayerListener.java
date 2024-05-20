package be.isach.ultracosmetics.listeners;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.cosmetics.suits.ArmorSlot;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.player.UltraPlayerManager;
import be.isach.ultracosmetics.player.profile.CosmeticsProfile;
import be.isach.ultracosmetics.run.FallDamageManager;
import be.isach.ultracosmetics.util.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * Player listeners.
 *
 * @author iSach
 * @since 08-03-2015
 */
public class PlayerListener implements Listener {

    private final UltraCosmetics ultraCosmetics;
    private final UltraPlayerManager pm;
    private final ItemStack menuItem;
    private final boolean menuItemEnabled = SettingsManager.getConfig().getBoolean("Menu-Item.Enabled");
    private final int menuItemSlot = SettingsManager.getConfig().getInt("Menu-Item.Slot");
    private final long joinItemDelay = SettingsManager.getConfig().getLong("Item-Delay.Join", 1);
    private final long respawnItemDelay = SettingsManager.getConfig().getLong("Item-Delay.World-Change-Or-Respawn", 0);

    public PlayerListener(UltraCosmetics ultraCosmetics) {
        this.ultraCosmetics = ultraCosmetics;
        this.pm = ultraCosmetics.getPlayerManager();
        this.menuItem = ItemFactory.getMenuItem();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreJoin(final PlayerJoinEvent event) {
        // Ready UltraPlayer as early as possible so it can be ready for other plugins that might also run code on join
        pm.createUltraPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        UltraPlayer ultraPlayer = pm.getUltraPlayer(event.getPlayer());
        if (SettingsManager.isAllowedWorld(event.getPlayer().getWorld())) {
            // Delay in case other plugins clear inventory on join
            Bukkit.getScheduler().runTaskLater(ultraCosmetics, () -> {
                if (menuItemEnabled && event.getPlayer().hasPermission("ultracosmetics.receivechest")) {
                    ultraPlayer.giveMenuItem();
                }
                if (UltraCosmeticsData.get().areCosmeticsProfilesEnabled()) {
                    ultraPlayer.getProfile().onLoad(CosmeticsProfile::equip);
                }
            }, joinItemDelay);
        }

        if (ultraCosmetics.getUpdateChecker() != null && ultraCosmetics.getUpdateChecker().isOutdated()) {
            if (event.getPlayer().hasPermission("ultracosmetics.updatenotify")) {
                Component prefix = MessageManager.getMessage("Prefix");
                ultraPlayer.sendMessage(Component.empty().append(prefix).append(Component.text("An update is available: "
                        + ultraCosmetics.getUpdateChecker().getSpigotVersion(), NamedTextColor.RED, TextDecoration.BOLD)));
                Component use = Component.text("Use ", NamedTextColor.RED, TextDecoration.BOLD);
                Component command = Component.text("/uc update", NamedTextColor.YELLOW);
                Component toInstall = Component.text(" to install the update.", NamedTextColor.RED, TextDecoration.BOLD);
                ultraPlayer.sendMessage(Component.empty().append(prefix).append(use).append(command).append(toInstall));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        if (SettingsManager.isAllowedWorld(event.getPlayer().getWorld())) {
            UltraPlayer up = pm.getUltraPlayer(event.getPlayer());
            if (menuItemEnabled && event.getPlayer().hasPermission("ultracosmetics.receivechest")) {
                Bukkit.getScheduler().runTaskLater(ultraCosmetics, up::giveMenuItem, respawnItemDelay);
            }
            // If the player joined an allowed world from a non-allowed world, re-equip their cosmetics.
            if (!SettingsManager.isAllowedWorld(event.getFrom())) {
                Bukkit.getScheduler().runTaskLater(ultraCosmetics, () -> up.getProfile().equip(), respawnItemDelay);
            }
        }
    }

    // run this as early as possible for compatibility with MV-inventories
    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldChangeEarly(final PlayerChangedWorldEvent event) {
        UltraPlayer ultraPlayer = pm.getUltraPlayer(event.getPlayer());
        if (!SettingsManager.isAllowedWorld(event.getPlayer().getWorld())) {
            // Disable cosmetics when joining a bad world.
            ultraPlayer.removeMenuItem();
            ultraPlayer.withPreserveEquipped(() -> {
                if (ultraPlayer.clear()) {
                    MessageManager.send(ultraPlayer.getBukkitPlayer(), "World-Disabled");
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        // When PlayerRespawnEvent is being called, the player may or may not be at
        // the final respawn location, so wait one tick before re-equipping.
        Bukkit.getScheduler().runTaskLater(ultraCosmetics, () -> {
            if (!SettingsManager.isAllowedWorld(event.getPlayer().getWorld())) return;
            UltraPlayer ultraPlayer = pm.getUltraPlayer(event.getPlayer());
            if (menuItemEnabled) {
                ultraPlayer.giveMenuItem();
            }
            ultraPlayer.getProfile().equip();
        }, Math.max(1, respawnItemDelay));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pm.getUltraPlayer(event.getPlayer()).dispose();
        // workaround plugins calling events after player quit
        Bukkit.getScheduler().runTaskLater(ultraCosmetics, () -> pm.remove(event.getPlayer()), 1);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        // Ignore NPC deaths as per iSach#467
        if (Bukkit.getPlayer(event.getEntity().getUniqueId()) == null) return;
        if (isMenuItem(event.getEntity().getInventory().getItem(menuItemSlot))) {
            event.getDrops().remove(event.getEntity().getInventory().getItem(menuItemSlot));
            event.getEntity().getInventory().setItem(menuItemSlot, null);
        }
        UltraPlayer ultraPlayer = pm.getUltraPlayer(event.getEntity());
        if (ultraPlayer.getCurrentGadget() != null) {
            event.getDrops().remove(ultraPlayer.getCurrentGadget().getItemStack());
        }
        if (ultraPlayer.getCurrentHat() != null) event.getDrops().remove(ultraPlayer.getCurrentHat().getItemStack());
        Arrays.asList(ArmorSlot.values()).forEach(armorSlot -> {
            if (ultraPlayer.getCurrentSuit(armorSlot) != null) {
                event.getDrops().remove(ultraPlayer.getCurrentSuit(armorSlot).getItemStack());
            }
        });
        if (ultraPlayer.getCurrentEmote() != null) {
            event.getDrops().remove(ultraPlayer.getCurrentEmote().getItemStack());
        }

        ultraPlayer.withPreserveEquipped(() -> {
            for (Category cat : Category.values()) {
                if (cat.isClearOnDeath()) {
                    ultraPlayer.removeCosmetic(cat);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && FallDamageManager.shouldBeProtected(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework && event.getDamager().hasMetadata("uc_firework")) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickUpItem(org.bukkit.event.player.PlayerPickupItemEvent event) {
        if (isMenuItem(event.getItem().getItemStack())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().hasPermission("ultracosmetics.bypass.disabledcommands")) return;
        String strippedCommand = event.getMessage().split(" ")[0].replace("/", "").toLowerCase();
        if (!SettingsManager.getConfig().getList("Disabled-Commands").contains(strippedCommand)) return;
        UltraPlayer player = pm.getUltraPlayer(event.getPlayer());
        if (player.hasCosmeticsEquipped()) {
            event.setCancelled(true);
            MessageManager.send(event.getPlayer(), "Disabled-Command-Message");
        }
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        UltraPlayer ultraPlayer = pm.getUltraPlayer(event.getPlayer());
        if (ultraPlayer.getCurrentGadget() != null && ultraPlayer.getCurrentGadget().getItemStack().equals(event.getItem())) {
            event.setCancelled(true);
            return;
        }
        for (ArmorSlot armorSlot : ArmorSlot.values()) {
            if (ultraPlayer.getCurrentSuit(armorSlot) != null) {
                if (event.getItem().equals(ultraPlayer.getCurrentSuit(armorSlot).getItemStack())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private boolean isMenuItem(ItemStack item) {
        return menuItem != null && item != null && menuItem.isSimilar(item);
    }
}
