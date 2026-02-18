package com.fluxcraft.MiaoMenu.javamenu;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.menu.action.ActionRegistry;
import com.fluxcraft.MiaoMenu.utils.PlaceholderUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import java.util.List;

public class JavaMenuListener implements Listener {
    private final MiaoMenu plugin;
    private final ActionRegistry actionRegistry;
    public JavaMenuListener(MiaoMenu plugin, ActionRegistry actionRegistry) {
        this.plugin = plugin;
        this.actionRegistry = actionRegistry;
    }
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && event.getClickedInventory() != null
                && event.getClickedInventory().getHolder() instanceof JavaMenu.MenuHolder holder) {
            event.setCancelled(true);
            JavaMenu.MenuItem item = holder.getMenu().getItem(event.getSlot());
            if (item == null) return;
            if (event.isLeftClick()) {
                List<String> commands = item.getLeftClickCommands();
                if (!commands.isEmpty()) {
                    executeActions(player, commands);
                }
            } else if (event.isRightClick()) {
                List<String> commands = item.getRightClickCommands();
                if (!commands.isEmpty()) {
                    executeActions(player, commands);
                }
            }
        }
    }
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof JavaMenu.MenuHolder) {
            event.setCancelled(true);
        }
    }
    private void executeActions(Player player, List<String> rawCommands) {
        for (String rawCmd : rawCommands) {
            if (rawCmd == null || rawCmd.trim().isEmpty()) continue;
            String parsed = PlaceholderUtils.parse(player, rawCmd, plugin);
            actionRegistry.dispatch(player, parsed);
        }
    }
}