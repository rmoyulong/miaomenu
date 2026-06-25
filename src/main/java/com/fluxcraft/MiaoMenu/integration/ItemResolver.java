package com.fluxcraft.MiaoMenu.integration;

import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.Base64;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.fluxcraft.MiaoMenu.MiaoMenu;

public class ItemResolver {
    private final MiaoMenu plugin;
    private final Material fallbackMaterial;
    private Boolean craftEngineAvailable;
    private Boolean itemsAdderAvailable;
    private Boolean mmoItemsAvailable;
    private Boolean headDatabaseAvailable;

    public ItemResolver(MiaoMenu plugin, Material fallbackMaterial) {
        this.plugin = plugin;
        this.fallbackMaterial = fallbackMaterial;
    }

    @NotNull
    public ItemStack resolve(String materialString, int customModelData) {
        if (materialString == null || materialString.isBlank()) {
            return new ItemStack(fallbackMaterial);
        }

        String lower = materialString.toLowerCase();

        if (lower.startsWith("craftengine:")) {
            ItemStack item = resolveCraftEngine(materialString.substring(12));
            if (item != null) return item;
        } else if (lower.startsWith("itemsadder:")) {
            ItemStack item = resolveItemsAdder(materialString.substring(11));
            if (item != null) return item;
        } else if (lower.startsWith("mmoitems:")) {
            ItemStack item = resolveMMOItems(materialString.substring(9));
            if (item != null) return item;
        } else if (lower.startsWith("headdb:")) {
            ItemStack item = resolveHeadDatabase(materialString.substring(7));
            if (item != null) return item;
        } else if (lower.startsWith("base64head:")) {
			ItemStack item = getCustomHead(materialString.substring(11)); 
            if (item != null) return item;
        }

        ItemStack vanillaItem = resolveVanilla(materialString);
        applyCustomModelData(vanillaItem, customModelData);
        return vanillaItem;
    }

    @SuppressWarnings("deprecation")
    public static void applyCustomModelData(ItemStack item, int customModelData) {
        if (customModelData <= 0 || item.getType() == Material.AIR) {
            return;
        }
        var meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
    }

    @NotNull
    private ItemStack resolveVanilla(String materialString) {
        Material mat = Material.matchMaterial(materialString);
        return new ItemStack(mat != null ? mat : fallbackMaterial);
    }

    private ItemStack resolveCraftEngine(String id) {
        if (isPluginUnavailable("CraftEngine", ref -> craftEngineAvailable = ref)) {
            return null;
        }
        try {
            var keyClass = Class.forName("net.momirealms.craftengine.core.util.Key");
            Object key = keyClass.getMethod("of", String.class).invoke(null, id);
            var itemsClass = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            Object customItem = invokeById(itemsClass, keyClass, key);
            if (customItem != null) {
                return (ItemStack) customItem.getClass().getMethod("buildItemStack").invoke(customItem);
            }
        } catch (Exception e) {
            plugin.getLogger().fine("CraftEngine item not found: " + id);
        }
        return null;
    }

    private static Object invokeById(Class<?> itemsClass, Class<?> keyClass, Object key) throws ReflectiveOperationException {
        return itemsClass.getMethod("byId", keyClass).invoke(null, key);
    }

    private ItemStack resolveItemsAdder(String id) {
        if (isPluginUnavailable("ItemsAdder", ref -> itemsAdderAvailable = ref)) {
            return null;
        }
        try {
            var customStack = Class.forName("dev.lone.itemsadder.api.CustomStack")
                    .getMethod("getInstance", String.class).invoke(null, id);
            if (customStack != null) {
                return (ItemStack) customStack.getClass().getMethod("getItemStack").invoke(customStack);
            }
        } catch (Exception e) {
            plugin.getLogger().fine("ItemsAdder item not found: " + id);
        }
        return null;
    }

    private ItemStack resolveMMOItems(String id) {
        if (isPluginUnavailable("MMOItems", ref -> mmoItemsAvailable = ref)) {
            return null;
        }
        try {
            String[] parts = id.split(":", 2);
            if (parts.length != 2) return null;
            var pluginObj = org.bukkit.Bukkit.getPluginManager().getPlugin("MMOItems");
            var mmoItemsClass = Class.forName("net.Indyuce.mmoitems.MMOItems");
            var getItemMethod = mmoItemsClass.getMethod("getItem", String.class, String.class);
            Object itemStack = getItemMethod.invoke(pluginObj, parts[0], parts[1]);
            return (ItemStack) itemStack;
        } catch (Exception e) {
            plugin.getLogger().fine("MMOItems item not found: " + id);
        }
        return null;
    }

    private ItemStack resolveHeadDatabase(String id) {
        if (isPluginUnavailable("HeadDatabase", ref -> headDatabaseAvailable = ref)) {
            return null;
        }
        try {
            var apiClass = Class.forName("com.arcaniax.headdatabase.Main");
            var apiMethod = apiClass.getMethod("getHead", String.class);
            var pluginObj = org.bukkit.Bukkit.getPluginManager().getPlugin("HeadDatabase");
            return (ItemStack) apiMethod.invoke(pluginObj, id);
        } catch (Exception e) {
            plugin.getLogger().fine("HeadDatabase head not found: " + id);
        }
        return null;
    }

    @FunctionalInterface
    private interface AvailabilitySetter {
        void set(Boolean value);
    }

    private boolean isPluginUnavailable(String pluginName, AvailabilitySetter setter) {
        try {
            Boolean cached = switch (pluginName) {
                case "CraftEngine" -> craftEngineAvailable;
                case "ItemsAdder" -> itemsAdderAvailable;
                case "MMOItems" -> mmoItemsAvailable;
                case "HeadDatabase" -> headDatabaseAvailable;
                default -> null;
            };
            if (cached == null) {
                cached = plugin.getServer().getPluginManager().isPluginEnabled(pluginName);
                setter.set(cached);
            }
            return !cached;
        } catch (Exception e) {
            return true;
        }
    }

    ///////////////////////////////////////////
    public ItemStack getCustomHead(String base64Texture) {
		ItemStack head = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) head.getItemMeta();
		
		if (meta != null) {
			// 1. 创建一个新的 PlayerProfile
			PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "CustomHead");
			
			// 2. 获取纹理对象并设置 URL
			// 注意：base64Texture 需要转换为 Mojang 纹理 URL
			// 通常 base64 解码后包含 {"textures":{"SKIN":{"url":"http://..."}}}
			// 这里假设你已经解析出了具体的 texture URL
			String textureUrl = decodeBase64ToUrl(base64Texture); // 你需要自己实现这个解码逻辑
			
			try {
				PlayerTextures textures = profile.getTextures();
				textures.setSkin(new URL(textureUrl));
				profile.setTextures(textures);
				
				// 3. 应用 Profile 到 SkullMeta
				meta.setPlayerProfile(profile);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			head.setItemMeta(meta);
		}
		
		return head;
    }

    // 辅助方法：将 Base64 纹理字符串解析为 URL
    private String decodeBase64ToUrl(String base64Texture) {
		try {
			byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Texture);
			String json = new String(decodedBytes);
			// 简单解析 JSON 获取 URL，建议使用 Gson 或 Jackson
			// 格式通常为: {"textures":{"SKIN":{"url":"http://textures.minecraft.net/..."}}}
			int start = json.indexOf("\"url\":\"") + 7;
			int end = json.indexOf("\"", start);
			return json.substring(start, end);
		} catch (Exception e) {
			throw new RuntimeException("Invalid base64 texture", e);
		}
    }
	
}	