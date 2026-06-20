package com.fluxcraft.MiaoMenu.security;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class RateLimiter {
    private static final long SWEEP_INTERVAL_MILLIS = 60_000L;
    private static final int EXPIRY_MULTIPLIER = 5;

    private final long windowMillis;
    private final int maxEvents;
    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastSweep = new AtomicLong(0L);

    public RateLimiter(Duration window, int maxEvents) {
        this.windowMillis = window.toMillis();
        this.maxEvents = maxEvents;
    }

    public boolean allow(UUID uuid) {
        long now = System.currentTimeMillis();
        sweepExpiredIfDue(now);
        // 原本是 get→put 兩步、非原子。Folia 多 region 緒（或 Paper 非主緒事件）並發點擊時，
        // 兩個緒會各自讀到舊 count、各自 put 覆蓋對方，造成 rate limit 被穩定繞過。
        // 改用 compute 把「讀舊值 → 判超限 → 寫新值」三步收進一個原子段落。
        // ConcurrentHashMap.compute 的 lambda 對同一 key 保證序列化執行。
        boolean[] allowed = new boolean[]{true};
        windows.compute(uuid, (k, current) -> {
            if (current == null || now - current.windowStart() >= windowMillis) {
                return new Window(now, 1);
            }
            if (current.count() >= maxEvents) {
                allowed[0] = false;
                return current;
            }
            return new Window(current.windowStart(), current.count() + 1);
        });
        return allowed[0];
    }

    public void remove(UUID uuid) {
        windows.remove(uuid);
    }

    public void clearAll() {
        windows.clear();
        lastSweep.set(0L);
    }

    // 被動清理：每分鐘最多掃一次，移除已超過 5 倍視窗時間的舊條目，避免閒置玩家累積記憶體。
    private void sweepExpiredIfDue(long now) {
        long previous = lastSweep.get();
        if (now - previous < SWEEP_INTERVAL_MILLIS) {
            return;
        }
        if (!lastSweep.compareAndSet(previous, now)) {
            return;
        }
        long threshold = windowMillis * EXPIRY_MULTIPLIER;
        Iterator<Map.Entry<UUID, Window>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Window> entry = it.next();
            if (now - entry.getValue().windowStart() >= threshold) {
                it.remove();
            }
        }
    }

    private record Window(long windowStart, int count) {
    }
}
