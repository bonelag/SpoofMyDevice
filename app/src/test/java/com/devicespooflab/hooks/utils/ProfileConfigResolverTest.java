package com.devicespooflab.hooks.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class ProfileConfigResolverTest {
    private static final String PACKAGE = "com.example.app";

    @Test
    public void explicitAssignmentOverridesDefaultAndIncludesGlobalOptions() throws Exception {
        JSONObject state = state("assigned", "default");
        state.getJSONObject("globalProperties").put("safe_mode.packages", PACKAGE);
        RuntimeConfigStore.Snapshot result = ProfileConfigResolver.resolveJson(state.toString(), PACKAGE, true);
        assertNotNull(result);
        assertEquals("Assigned model", result.properties.get("ro.product.model"));
        assertEquals(PACKAGE, result.properties.get("safe_mode.packages"));
    }

    @Test
    public void defaultAppliesOnlyWhenAllowedAndNoAssignmentExists() throws Exception {
        String json = state(null, "default").toString();
        assertEquals("Default model", ProfileConfigResolver.resolveJson(json, PACKAGE, true)
            .properties.get("ro.product.model"));
        assertFalse(ProfileConfigResolver.resolveJson(json, PACKAGE, false).hasAssignedProfile());
    }

    @Test
    public void removalInCompletePreferencesStateDoesNotUseAStalePackageEntry() throws Exception {
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("json_state", state(null, null).toString());
        preferences.put("pkg_" + PACKAGE, "ro.product.model=Stale model");
        RuntimeConfigStore store = new RuntimeConfigStore(ProfileConfigResolver.resolveContent("ro.product.model=Before"));
        assertTrue(store.reload(() -> ProfileConfigResolver.resolvePreferences(preferences, PACKAGE, true)));
        assertFalse(store.current().hasAssignedProfile());
        assertTrue(store.current().properties.isEmpty());
    }

    @Test
    public void unavailableOrPartialStateKeepsLastProfileButValidRemovalClearsIt() throws Exception {
        RuntimeConfigStore store = new RuntimeConfigStore(ProfileConfigResolver.resolveContent("ro.product.model=Before"));
        assertFalse(store.reload(() -> ProfileConfigResolver.resolveJson("{\"version\":1,", PACKAGE, true)));
        assertEquals("Before", store.current().properties.get("ro.product.model"));
        assertNull(ProfileConfigResolver.resolveJson(null, PACKAGE, true));
        assertNull(ProfileConfigResolver.resolveJson("{}", PACKAGE, true));
        assertNull(ProfileConfigResolver.resolveJson(state(null, null).put("version", 2).toString(), PACKAGE, true));
        String removalJson = state(null, null).toString();
        assertTrue(store.reload(() -> ProfileConfigResolver.resolveJson(removalJson, PACKAGE, true)));
        assertFalse(store.current().hasAssignedProfile());
    }

    @Test
    public void bootControlOverridesAreRemovedFromProfileAndGlobalOptions() throws Exception {
        JSONObject state = state("assigned", null);
        state.getJSONArray("profiles").getJSONObject(0).put("content",
            "ro.product.model=Assigned model\nsys.boot_completed=1\ninit.svc.bootanim=stopped\n");
        state.getJSONObject("globalProperties").put("sys.powerctl", "reboot");
        RuntimeConfigStore.Snapshot result = ProfileConfigResolver.resolveJson(state.toString(), PACKAGE, true);
        assertEquals("Assigned model", result.properties.get("ro.product.model"));
        assertFalse(result.properties.containsKey("sys.boot_completed"));
        assertFalse(result.properties.containsKey("init.svc.bootanim"));
        assertFalse(result.properties.containsKey("sys.powerctl"));
    }

    private static JSONObject state(String assignment, String defaultProfile) throws Exception {
        JSONArray profiles = new JSONArray()
            .put(new JSONObject().put("id", "assigned").put("content", "ro.product.model=Assigned model\n"))
            .put(new JSONObject().put("id", "default").put("content", "ro.product.model=Default model\n"));
        JSONObject assignments = new JSONObject();
        if (assignment != null) {
            assignments.put(PACKAGE, assignment);
        }
        JSONObject globals = new JSONObject();
        if (defaultProfile != null) {
            globals.put("default_profile_id", defaultProfile);
        }
        return new JSONObject().put("version", 1).put("profiles", profiles)
            .put("assignments", assignments).put("globalProperties", globals);
    }
}
