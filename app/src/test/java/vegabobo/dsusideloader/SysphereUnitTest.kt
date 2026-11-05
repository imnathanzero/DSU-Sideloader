package vegabobo.dsusideloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vegabobo.dsusideloader.ui.screen.partitions.PartitionInfo
import vegabobo.dsusideloader.ui.theme.ThemeMode
import vegabobo.dsusideloader.util.OperationMode

class SysphereUnitTest {

    @Test
    fun testThemeModeEnumValues() {
        val modes = ThemeMode.values().map { it.name }
        assertTrue(modes.contains("SYSTEM"))
        assertTrue(modes.contains("LIGHT"))
        assertTrue(modes.contains("DARK"))
        assertEquals(3, modes.size)
    }

    @Test
    fun testPartitionInfoSizeFormatting() {
        val unknown = PartitionInfo(name = "test", path = "/dev/test", sizeBytes = 0L)
        assertEquals("Unknown size", unknown.formattedSize)

        val kb = PartitionInfo(name = "test", path = "/dev/test", sizeBytes = 500 * 1024L)
        assertEquals("500 KB", kb.formattedSize)

        val mb = PartitionInfo(name = "test", path = "/dev/test", sizeBytes = 150 * 1024 * 1024L)
        assertEquals("150.0 MB", mb.formattedSize)

        val gb = PartitionInfo(name = "test", path = "/dev/test", sizeBytes = (2.5 * 1024 * 1024 * 1024).toLong())
        assertEquals("2.50 GB", gb.formattedSize)
    }

    @Test
    fun testGsiSupportedExtensions() {
        val supportedExtensions = setOf("img", "gz", "xz", "zip")
        assertTrue(supportedExtensions.contains("img"))
        assertTrue(supportedExtensions.contains("gz"))
        assertTrue(supportedExtensions.contains("xz"))
        assertTrue(supportedExtensions.contains("zip"))
        assertFalse(supportedExtensions.contains("tar"))
        assertFalse(supportedExtensions.contains("apk"))
        assertFalse(supportedExtensions.contains("iso"))
    }

    @Test
    fun testOperationModesAvailability() {
        val modes = OperationMode.values()
        assertTrue(modes.contains(OperationMode.ROOT))
        assertTrue(modes.contains(OperationMode.SHIZUKU))
        assertTrue(modes.contains(OperationMode.ADB))
        assertTrue(modes.contains(OperationMode.SYSTEM))
        assertTrue(modes.contains(OperationMode.SYSTEM_AND_ROOT))
    }
}
