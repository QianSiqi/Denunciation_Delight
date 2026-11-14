package net.qiansiqi.denunciationdelight;

import net.qiansiqi.denunciationdelight.flag.FlagRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagRegistryTest {

    @Test
    void loadsAllFlagsAndLegacyAliases() {
        assertTrue(FlagRegistry.all().size() >= 249,
                "expected at least 249 flags, got " + FlagRegistry.all().size());
        assertEquals("cn", FlagRegistry.canonicalize("china"));
        assertEquals("us", FlagRegistry.canonicalize("usa"));
        assertEquals("gb", FlagRegistry.canonicalize("uk"));
        assertEquals("ru", FlagRegistry.canonicalize("russia"));
        assertEquals("fr", FlagRegistry.canonicalize("france"));
        assertEquals("de", FlagRegistry.canonicalize("germany"));
    }

    @Test
    void everyFlagHasATexture() {
        for (FlagRegistry.FlagDef flag : FlagRegistry.all()) {
            String path = "/assets/denunciationdelight/textures/flag/" + flag.id() + ".png";
            assertNotNull(FlagRegistry.class.getResourceAsStream(path),
                    "missing texture: " + path);
        }
    }
}