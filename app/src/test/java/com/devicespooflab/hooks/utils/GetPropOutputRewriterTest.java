package com.devicespooflab.hooks.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class GetPropOutputRewriterTest {
    @Test
    public void dumpPreservesBootStateAndUnrelatedStdoutWhileRewritingDeviceData() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("ro.product.model", "Spoofed model");
        properties.put("sys.boot_completed", "1");
        properties.put("init.svc.bootanim", "stopped");
        properties.put("ro.build.fingerprint", "new fingerprint");
        GetPropOutputRewriter rewriter = new GetPropOutputRewriter(properties, key -> true);
        assertEquals("[sys.boot_completed]: [0]\n", rewriter.rewriteLine("[sys.boot_completed]: [0]\n"));
        assertEquals("[init.svc.bootanim]: [running]\n", rewriter.rewriteLine("[init.svc.bootanim]: [running]\n"));
        assertFalse(rewriter.finish().contains("boot"));

        rewriter = new GetPropOutputRewriter(properties, key -> "ro.vivo.private".equals(key));
        String output = rewriter.rewriteLine("warning on merged stderr\r\n")
            + rewriter.rewriteLine("[ro.product.model]: [Real model]\r\n")
            + rewriter.rewriteLine("[ro.vivo.private]: [host]\n")
            + rewriter.rewriteLine("[persist.example]: [unchanged]\n")
            + rewriter.finish();
        assertTrue(output.startsWith("warning on merged stderr\r\n[ro.product.model]: [Spoofed model]\r\n"));
        assertTrue(output.contains("[persist.example]: [unchanged]\n"));
        assertTrue(output.endsWith("[ro.build.fingerprint]: [new fingerprint]\n"));
        assertFalse(output.contains("ro.vivo.private"));
        assertFalse(output.contains("sys.boot_completed"));
    }

    @Test
    public void missingPropertyIsAddedOnceAfterAnUnterminatedLastLine() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("ro.product.model", "Spoofed model");
        properties.put("ro.build.id", "NEW");
        GetPropOutputRewriter rewriter = new GetPropOutputRewriter(properties, key -> false);
        String output = rewriter.rewriteLine("[ro.product.model]: [Real model]") + rewriter.finish();
        assertEquals("[ro.product.model]: [Spoofed model]\n[ro.build.id]: [NEW]\n", output);
    }
}
