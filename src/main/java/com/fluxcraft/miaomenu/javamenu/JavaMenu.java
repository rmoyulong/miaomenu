package com.fluxcraft.MiaoMenu.javamenu;

import com.fluxcraft.MiaoMenu.constants.Constants;
import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.integration.CraftEngineIntegration;
import com.fluxcraft.MiaoMenu.utils.PlaceholderUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class JavaMenu {
    private final String title;
    private final int size;
    private final MiaoMenu plugin;
    private final CraftEngineIntegration craftEngineIntegration;
    private final List<MenuItem> items;
    public JavaMenu(FileConfiguration config, MiaoMenu plugin, CraftEngineIntegration craftEngineIntegration) {
        this.plugin = plugin;
        this.craftEngineIntegration = craftEngineIntegration;
        this.title = config.getString("menu_title", config.getString("title", "&6Menu"));
        this.size = Math.min(
                Math.max(config.getInt("rows", Constants.Config.DEFAULT_MENU_ROWS), Constants.Config.INVENTORY_MIN_ROWS),
                Constants.Config.INVENTORY_MAX_ROWS
        ) * Constants.Config.INVENTORY_ROW_SIZE;
        this.items = new ArrayList<>();
        loadItems(config);
    }
    private void loadItems(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("items");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String path = "items." + key;
            items.add(new MenuItem(
                    config.getInt(path + ".slot", 0),
                    config.getString(path + ".material", "STONE"),
                    config.getString(path + ".display_name", "&fItem"),
                    config.getStringList(path + ".lore"),
                    config.getStringList(path + ".left_click_commands")
            ));
        }
    }
    public void open(Player player) {
        String parsedTitleString = PlaceholderUtils.parse(player, title, plugin);
        if (parsedTitleString.length() > Constants.Config.TITLE_MAX_LENGTH) {
            parsedTitleString = parsedTitleString.substring(0, Constants.Config.TITLE_MAX_LENGTH);
        }
        Component titleComponent = LegacyComponentSerializer.legacySection().deserialize(parsedTitleString);
        MenuHolder holder = new MenuHolder(this);
        Inventory inventory = Bukkit.createInventory(holder, size, titleComponent);
        holder.setInventory(inventory);
        items.forEach(item -> {
            ItemStack stack = item.createItemStack(player, plugin, craftEngineIntegration);
            if (stack != null) inventory.setItem(item.getSlot(), stack);
        });
        player.openInventory(inventory);
    }
    public MenuItem getItem(int slot) {
        return items.stream().filter(i -> i.getSlot() == slot).findFirst().orElse(null);
    }
    public static class MenuHolder implements org.bukkit.inventory.InventoryHolder {
        private Inventory inventory;
        private final JavaMenu menu;
        public MenuHolder(JavaMenu menu) { this.menu = menu; }
        @Override @NotNull public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
        public JavaMenu getMenu() { return menu; }
    }
    public static class MenuItem {
        private final int slot;
        private final String material;
        private final String name;
        private final List<String> lore;
        private final List<String> commands;
        public MenuItem(int slot, String material, String name, List<String> lore, List<String> commands) {
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.commands = commands;
        }
        public ItemStack createItemStack(Player player, MiaoMenu plugin, @Nullable CraftEngineIntegration craftEngineIntegration) {
            ItemStack item = craftEngineIntegration != null ? craftEngineIntegration.getItemStack(material) : getVanillaItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String displayNameParsed = PlaceholderUtils.parse(player, name, plugin);
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize(displayNameParsed));
                List<Component> loreComponents = new ArrayList<>();
                lore.forEach(line -> loreComponents.add(
                        LegacyComponentSerializer.legacySection().deserialize(PlaceholderUtils.parse(player, line, plugin))
                ));
                meta.lore(loreComponents);
                item.setItemMeta(meta);
            }
            return item;
        }
        @NotNull
        private ItemStack getVanillaItemStack(@NotNull String materialString) {
            Material mat = Material.matchMaterial(materialString);
            return new ItemStack(mat != null ? mat : Material.STONE);
        }
        public int getSlot() { return slot; }
        public List<String> getCommands() { return commands; }
    }
}