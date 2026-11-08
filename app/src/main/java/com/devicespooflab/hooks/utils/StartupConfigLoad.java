package com.devicespooflab.hooks.utils;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** A late/failed local cache never creates an unbounded application-startup wait. */
public final class StartupConfigLoad {
    public static final long MAX_WAIT_MILLIS = 100;
    private StartupConfigLoad() { }
    public static boolean await(Future<Boolean> load) {
        try { return Boolean.TRUE.equals(load.get(MAX_WAIT_MILLIS, TimeUnit.MILLISECONDS)); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
        catch (Exception unavailable) { return false; }
    }
}
