package com.fluxcraft.MiaoMenu.bedrockmenu;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.utils.Lang;
import com.fluxcraft.MiaoMenu.utils.PlaceholderUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import java.util.ArrayList;
import java.util.List;

public class BedrockMenu {

    private static class ConfigKeys {
        public static final String MENU_ITEMS = "menu.items";
        public static final String MENU_TITLE = "menu.title";
        public static final String DEFAULT_TITLE = "菜单";
        public static final String TEXT = "text";
        public static final String ICON = "icon";
        public static final String ICON_TYPE = "icon_type";
        public static final String COMMAND = "command";
        public static final String DEFAULT_TEXT = Lang.get("default-item-name");
        public static final String DEFAULT_ICON_TYPE = "path";
        public static final String ICON_TYPE_URL = "url";
    }
    private final String name;
    private final FileConfiguration config;
    private final List<BedrockMenuItem> menuItems = new ArrayList<>();
    private final MiaoMenu plugin;
    public BedrockMenu(String name, FileConfiguration config, MiaoMenu plugin) {
        this.name = name;
        this.config = config;
        this.plugin = plugin;
        loadMenuItems();
    }
    private void loadMenuItems() {
        menuItems.clear();
        if (!config.contains(ConfigKeys.MENU_ITEMS)) {
            return;
        }
        List<?> items = config.getList(ConfigKeys.MENU_ITEMS);
        if (items == null) {
            return;
        }
        for (Object itemObj : items) {
            if (itemObj instanceof ConfigurationSection section) {
                String text = section.getString(ConfigKeys.TEXT, ConfigKeys.DEFAULT_TEXT);
                String icon = section.getString(ConfigKeys.ICON, "");
                String iconType = section.getString(ConfigKeys.ICON_TYPE, ConfigKeys.DEFAULT_ICON_TYPE);
                String command = section.getString(ConfigKeys.COMMAND, "");
                menuItems.add(new BedrockMenuItem(text, icon, iconType, command));
            }
        }
    }
    public SimpleForm.Builder buildForm(org.bukkit.entity.Player player) {
        String title = PlaceholderUtils.parse(player, getMenuTitle(), plugin);
        SimpleForm.Builder form = SimpleForm.builder()
                .title(title)
                .content("");
        for (BedrockMenuItem item : menuItems) {
            String buttonText = PlaceholderUtils.parse(player, item.text(), plugin);
            if (item.hasIcon()) {
                FormImage.Type type = parseImageType(item.iconType());
                form.button(buttonText, FormImage.of(type, item.icon()));
            } else {
                form.button(buttonText);
            }
        }
        return form;
    }
    private FormImage.Type parseImageType(String typeString) {
        if (ConfigKeys.ICON_TYPE_URL.equalsIgnoreCase(typeString)) {
            return FormImage.Type.URL;
        }
        return FormImage.Type.PATH;
    }
    private String getMenuTitle() {
        return config.getString(ConfigKeys.MENU_TITLE, ConfigKeys.DEFAULT_TITLE);
    }
    public String getName() {
        return name;
    }
    public List<BedrockMenuItem> getMenuItems() {
        return new ArrayList<>(menuItems);
    }
    public record BedrockMenuItem(String text, String icon, String iconType, String command) {
        public BedrockMenuItem {
            if (text == null) text = ConfigKeys.DEFAULT_TEXT;
            if (icon == null) icon = "";
            if (iconType == null) iconType = ConfigKeys.DEFAULT_ICON_TYPE;
            if (command == null) command = "";
        }
        public String getCommand() { return command; }
        public boolean hasIcon() {
            return !icon.isEmpty();
        }
    }
}
