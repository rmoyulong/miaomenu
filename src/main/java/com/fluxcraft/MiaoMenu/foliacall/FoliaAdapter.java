package com.fluxcraft.MiaoMenu.foliacall;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public interface FoliaAdapter {
    void runTaskLaterForEntity(Plugin plugin, Entity entity, Runnable task, long delay);

    // 對 Folia 把工作排到該實體（玩家）擁有的 region 緒；Paper 走 Bukkit 主緒。
    // 用於：選單動作派發、Floodgate 表單回呼後操作 player API 等場景，避免跨 region 緒違規。
    void runForEntity(Plugin plugin, Entity entity, Runnable task);
}
