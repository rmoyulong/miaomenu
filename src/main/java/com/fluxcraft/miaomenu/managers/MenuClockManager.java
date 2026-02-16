package com.fluxcraft.MiaoMenu.managers;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.utils.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuClockManager {
    private final MiaoMenu plugin;
    private final NamespacedKey clockKey;
    private final Map<UUID, Boolean> pendingRestore = new HashMap<>();
    public MenuClockManager(MiaoMenu plugin, NamespacedKey clockKey) {
        this.plugin = plugin;
        this.clockKey = clockKey;
    }
    public ItemStack createClock() {
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta = clock.getItemMeta();
        if (meta != null) {
            String rawName = Lang.get("menu.clock.name");
            Component nameComponent = Component.text(rawName)
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(nameComponent);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(clockKey, PersistentDataType.BYTE, (byte) 1);
            clock.setItemMeta(meta);
        }
        return clock;
    }
    public boolean isMenuClock(ItemStack item) {
        if (item == null || item.getType() != Material.CLOCK) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(clockKey, PersistentDataType.BYTE);
    }
    public boolean playerHasClock(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMenuClock(item)) return true;
        }
        return false;
    }
    public boolean playerHasNoClock(Player player) {
        return !playerHasClock(player);
    }
    public void giveClockToPlayer(Player player) {
        if (playerHasNoClock(player)) {
            player.getInventory().addItem(createClock());
            player.sendMessage(Lang.get("menu.clock.given"));
        }
    }
    public void openMenuWithClock(Player player) {
        String defaultMenu = plugin.getConfig().getString("settings.default-menu", "test");
        plugin.openSmartMenu(player, defaultMenu);
    }
    public void removeClockFromDrops(Player player, java.util.List<ItemStack> drops) {
        if (drops != null) {
            for (int i = 0; i < drops.size(); i++) {
                if (isMenuClock(drops.get(i))) {
                    drops.remove(i);
                    i--;
                    pendingRestore.put(player.getUniqueId(), true);
                }
            }
        }
    }
    public void ensureClock(Player player) {
        boolean restoredFromDeath = pendingRestore.remove(player.getUniqueId()) != null;
        if (restoredFromDeath && playerHasNoClock(player)) {
            player.getInventory().addItem(createClock());
            player.sendMessage(Lang.get("menu.clock.restored"));
        } else if (playerHasNoClock(player)) {
            player.getInventory().addItem(createClock());
        }
    }
}