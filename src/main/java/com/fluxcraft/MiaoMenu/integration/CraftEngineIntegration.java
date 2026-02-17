package com.fluxcraft.MiaoMenu.integration;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CraftEngineIntegration {

    private final MiaoMenu plugin;
    private final boolean enabled;
    private final Material fallbackMaterial;
    private Boolean craftEngineAvailable;

    public CraftEngineIntegration(MiaoMenu plugin, boolean enabled, Material fallbackMaterial) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.fallbackMaterial = fallbackMaterial;
        this.craftEngineAvailable = null;
    }

    @NotNull
    public ItemStack getItemStack(@NotNull String materialString) {
        if (!enabled) {
            return getVanillaItemStack(materialString);
        }

        if (isCraftEngineItem(materialString)) {
            ItemStack ceItem = getCraftEngineItem(materialString);
            if (ceItem != null) {
                return ceItem;
            }
        }

        return getVanillaItemStack(materialString);
    }

    @Nullable
    private ItemStack getCraftEngineItem(@NotNull String materialString) {
        if (!isCraftEngineAvailable()) {
            return null;
        }

        try {
            Key key = Key.of(materialString);
            CustomItem<ItemStack> customItem = CraftEngineItems.byId(key);
            if (customItem != null) {
                return customItem.buildItemStack();
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Failed to get CraftEngine item: " + materialString);
        }
        return null;
    }

    @NotNull
    private ItemStack getVanillaItemStack(@NotNull String materialString) {
        Material material = Material.matchMaterial(materialString);
        if (material != null) {
            return new ItemStack(material);
        }
        return new ItemStack(fallbackMaterial);
    }

    private boolean isCraftEngineItem(@NotNull String materialString) {
        return materialString.contains(":");
    }

    private boolean isCraftEngineAvailable() {
        if (craftEngineAvailable == null) {
            craftEngineAvailable = plugin.getServer().getPluginManager().isPluginEnabled("CraftEngine");
            if (craftEngineAvailable) {
                plugin.getLogger().info("CraftEngine integration enabled.");
            } else {
                plugin.getLogger().fine("CraftEngine not available, falling back to vanilla materials.");
            }
        }
        return craftEngineAvailable;
    }
}
