package com.fluxcraft.MiaoMenu.managers;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.utils.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.Map;

public class MenuClockManager {
    private final MiaoMenu plugin;
    private final NamespacedKey clockKey;
    public MenuClockManager(MiaoMenu plugin, NamespacedKey clockKey) {
        this.plugin = plugin;
        this.clockKey = clockKey;
    }
    public ItemStack createClock() {
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta = clock.getItemMeta();
        if (meta != null) {
            String rawName = Lang.get("menu.clock.name");
			List<String> list_lore = new ArrayList<>();
			list_lore.add("&e==================");
			list_lore.add("&b=====小森监制=====");
			list_lore.add("&e==================");
            Component nameComponent = Component.text(rawName)
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(nameComponent);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
			meta.setLore(list_lore);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(clockKey, PersistentDataType.BYTE, (byte) 1);
            clock.setItemMeta(meta);
        }
        return clock;
    }
    public boolean isMenuClock(ItemStack item) {
        if (item == null || item.getType() != Material.CLOCK) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(clockKey, PersistentDataType.BYTE);
    }
    public boolean playerHasClock(Player player) {
        // 同時掃主物品欄、副手與盔甲槽，避免玩家把選單時鐘放到副手仍被誤判為「沒有」而重複給予。
        // Paper 1.21+ getContents() 已含 41 槽（含 armor + offhand），但顯式分開查詢更穩，
        // 避免日後 API 變動或非 Paper 衍生 server 行為差異。
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (isMenuClock(item)) return true;
        }
        if (isMenuClock(player.getInventory().getItemInOffHand())) return true;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (isMenuClock(item)) return true;
        }
        return false;
    }
    public boolean playerHasNoClock(Player player) {
        return !playerHasClock(player);
    }
    public void giveClockToPlayer(Player player) {
        if (playerHasNoClock(player)) {
            ItemStack clock = createClock();
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(clock);
            if (!leftovers.isEmpty()) {
                Location loc = player.getLocation();
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(loc, leftover);
                }
            }
            player.sendMessage(Lang.get("menu.clock.given"));
        }
    }
    public void openMenuWithClock(Player player) {
        String defaultMenu = plugin.getConfig().getString("settings.default-menu", "test");
        plugin.openSmartMenu(player, defaultMenu);
    }
    public void removeClockFromDrops(java.util.List<ItemStack> drops) {
        drops.removeIf(this::isMenuClock);
    }
    public void ensureClock(Player player) {
        giveClockToPlayer(player);
    }
}
