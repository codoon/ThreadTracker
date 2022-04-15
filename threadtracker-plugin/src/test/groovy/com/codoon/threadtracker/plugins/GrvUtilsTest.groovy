package com.codoon.threadtracker.plugins

class GrvUtilsTest extends GroovyTestCase {
    void testExtractVersion() {
        def version = GrvUtils.extractVersion("threadtracker-1.1.3-runtime.jar")
        assertEquals("1.1.3", version)

        version = GrvUtils.extractVersion("jetified-threadtracker-0.2.3-runtime_93e37613")
        assertEquals("0.2.3", version)
    }
}
