package com.devicespooflab.hooks.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.Consumer;

/** Readers never wait for the preference, file, or provider operation of a reload. */
public final class RuntimeConfigStore {

    private final Object reloadLock = new Object();
    private volatile Snapshot snapshot;

    public RuntimeConfigStore(Snapshot initialSnapshot) {
        snapshot = Objects.requireNonNull(initialSnapshot);
    }

    public Snapshot current() {
        return snapshot;
    }

    public boolean reload(Supplier<Snapshot> loader) {
        synchronized (reloadLock) {
            // Publish only a complete snapshot. A failed loader leaves the old one intact.
            Snapshot loaded = loader.get();
            if (loaded == null) {
                return false;
            }
            snapshot = loaded;
            return true;
        }
    }

    /** Only the initial cache may publish before disk views are prepared. Live reloads use reload(). */
    public boolean reloadForStartup(Supplier<Snapshot> loader, Consumer<Snapshot> prepare) {
        Snapshot loaded;
        synchronized (reloadLock) {
            if (!snapshot.fromEmbeddedDefaults) return false;
            loaded = loader.get();
            if (loaded == null) return false;
            snapshot = loaded;
        }
        prepare.accept(loaded);
        return true;
    }

    public static final class Snapshot {
        public final Map<String, String> properties;
        public final boolean fromEmbeddedDefaults;
        public final HardwareIdentity hardware;

        public Snapshot(Map<String, String> properties, boolean fromEmbeddedDefaults) {
            this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            this.fromEmbeddedDefaults = fromEmbeddedDefaults;
            this.hardware = new HardwareIdentity(this.properties, !fromEmbeddedDefaults && !this.properties.isEmpty());
        }

        public boolean hasAssignedProfile() {
            return !fromEmbeddedDefaults && !properties.isEmpty();
        }
    }
}
