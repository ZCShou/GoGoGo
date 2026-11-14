package com.zcshou.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VersionUtilsTest {
    @Test
    public void extractVersion_fromReleaseNameAndTag() {
        assertEquals("1.12.3", VersionUtils.extractVersion("ReleaseV1.12.3"));
        assertEquals("2.1.0", VersionUtils.extractVersion("v2.1.0"));
        assertEquals("2.5.2", VersionUtils.extractVersion("2.5.2"));
        assertNull(VersionUtils.extractVersion("master"));
        assertNull(VersionUtils.extractVersion(null));
    }

    @Test
    public void isNewerVersion_comparesEachComponentNumerically() {
        assertTrue(VersionUtils.isNewerVersion("v2.1.0", "1.12.3"));
        assertTrue(VersionUtils.isNewerVersion("v2.5.2", "1.12.3"));
        assertTrue(VersionUtils.isNewerVersion("1.12.10", "1.12.3"));
        assertFalse(VersionUtils.isNewerVersion("ReleaseV1.12.3", "1.12.3"));
        assertFalse(VersionUtils.isNewerVersion("v2.1.0", "2.1.0"));
        assertFalse(VersionUtils.isNewerVersion("1.12.3", "2.5.2"));
    }

    @Test
    public void isNewerVersion_handlesUnknownVersions() {
        assertFalse(VersionUtils.isNewerVersion("master", "1.12.3"));
        assertFalse(VersionUtils.isNewerVersion(null, "1.12.3"));
        assertFalse(VersionUtils.isNewerVersion("v2.1.0", null));
    }
}
