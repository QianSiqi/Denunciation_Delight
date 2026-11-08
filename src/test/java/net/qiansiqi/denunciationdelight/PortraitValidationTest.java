package net.qiansiqi.denunciationdelight;

import net.qiansiqi.denunciationdelight.portrait.ServerPortraitManager;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortraitValidationTest {

    @Test
    void acceptsGeneratedPortraitId() {
        assertTrue(ServerPortraitManager.isValidPortraitId("0123456789abcdef"));
    }

    @Test
    void rejectsTraversalAndMalformedIds() {
        assertFalse(ServerPortraitManager.isValidPortraitId("../outside"));
        assertFalse(ServerPortraitManager.isValidPortraitId("..\\..\\outside"));
        assertFalse(ServerPortraitManager.isValidPortraitId("0123456789ABCDEF"));
        assertFalse(ServerPortraitManager.isValidPortraitId("short"));
        assertFalse(ServerPortraitManager.isValidPortraitId(null));
    }

    @Test
    void acceptsSmallPngAndRejectsNonPng() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        assertTrue(ServerPortraitManager.isValidImageData(out.toByteArray()));

        assertFalse(ServerPortraitManager.isValidImageData(new byte[]{1, 2, 3, 4}));
        assertFalse(ServerPortraitManager.isValidImageData("not a png".getBytes()));
    }
}