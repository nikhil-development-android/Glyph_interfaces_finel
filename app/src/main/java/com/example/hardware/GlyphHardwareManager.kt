package com.example.hardware

import android.os.Build
import com.example.model.PortingDocSection
import com.example.model.SysfsNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

/**
 * Hardware Manager & Porting Bridge for Nothing Phone (2a) Glyph Interface.
 * Handles Root (su) execution, Sysfs node discovery, and provides the complete
 * HyperOS / AOSP porting blueprints.
 */
object GlyphHardwareManager {

    val isPhone2a: Boolean = isTargetDevice()

    private fun isTargetDevice(): Boolean {
        val dev = (Build.DEVICE + " " + Build.MODEL + " " + Build.PRODUCT + " " + Build.BOARD).lowercase()
        return dev.contains("pacman") || dev.contains("a065") || dev.contains("nothing")
    }

    val knownSysfsNodes = listOf(
        SysfsNodeInfo(
            name = "All LEDs Master Brightness",
            path = "/sys/class/leds/aw210xx_led/all_white_leds_br",
            channelRange = "All Channels (0-25)",
            description = "Controls global brightness multiplier across all 3 strips (0 - 4095).",
            testCommand = "echo 2048 > /sys/class/leds/aw210xx_led/all_white_leds_br"
        ),
        SysfsNodeInfo(
            name = "Single Channel Direct PWM",
            path = "/sys/class/leds/aw210xx_led/single_led_br",
            channelRange = "Channel <id> <val>",
            description = "Directly sets single LED brightness. e.g., '0 4095' for Strip 1 segment 0.",
            testCommand = "echo '0 4095' > /sys/class/leds/aw210xx_led/single_led_br"
        ),
        SysfsNodeInfo(
            name = "Strip 1 (24-Segment Arc)",
            path = "/sys/class/leds/glyph-led-1/brightness",
            channelRange = "Index 0 - 23 (24 Segments)",
            description = "Top-Left Arc around the camera module. Used for progress, timer & music.",
            testCommand = "echo 255 > /sys/class/leds/glyph-led-1/brightness"
        ),
        SysfsNodeInfo(
            name = "Strip 2 (Top-Right Accent)",
            path = "/sys/class/leds/glyph-led-2/brightness",
            channelRange = "Index 24 (1 Zone)",
            description = "Top-Right slant accent light adjacent to the dual cameras.",
            testCommand = "echo 255 > /sys/class/leds/glyph-led-2/brightness"
        ),
        SysfsNodeInfo(
            name = "Strip 3 (Bottom Ribbon)",
            path = "/sys/class/leds/glyph-led-3/brightness",
            channelRange = "Index 25 (1 Zone)",
            description = "Bottom slanted/vertical stripe accent below the battery coil area.",
            testCommand = "echo 255 > /sys/class/leds/glyph-led-3/brightness"
        ),
        SysfsNodeInfo(
            name = "Alternative Platform I2C Path",
            path = "/sys/devices/platform/soc/11d00000.i2c/i2c-5/5-0034/leds",
            channelRange = "Direct I2C AW96103 Driver",
            description = "Direct platform device bus path on MediaTek Dimensity 7200 Pro soc.",
            testCommand = "ls -l /sys/devices/platform/soc/11d00000.i2c/i2c-5/5-0034/leds"
        )
    )

    /**
     * Executes shell command via su (root) or regular sh.
     */
    suspend fun executeCommand(command: String, useRoot: Boolean = true): ExecutionResult = withContext(Dispatchers.IO) {
        val output = StringBuilder()
        val error = StringBuilder()
        var exitCode = -1

        try {
            val process = if (useRoot) {
                Runtime.getRuntime().exec("su")
            } else {
                Runtime.getRuntime().exec("sh")
            }

            val os = DataOutputStream(process.outputStream)
            os.writeBytes(command + "\n")
            os.writeBytes("exit\n")
            os.flush()

            val stdReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (stdReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            while (errReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }

            exitCode = process.waitFor()
            os.close()
        } catch (e: Exception) {
            error.append("Execution Exception: ").append(e.localizedMessage ?: e.toString())
        }

        ExecutionResult(
            command = command,
            output = output.toString().trim(),
            error = error.toString().trim(),
            exitCode = exitCode,
            isSuccess = (exitCode == 0 && error.isEmpty())
        )
    }

    /**
     * Scans the device /sys/class/leds directory to find real nodes on this ROM.
     */
    suspend fun scanDeviceLeds(): List<String> = withContext(Dispatchers.IO) {
        val found = mutableListOf<String>()
        try {
            val dir = File("/sys/class/leds")
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    found.add(file.absolutePath)
                }
            }
        } catch (e: Exception) {
            // Ignore if restricted
        }
        if (found.isEmpty()) {
            val res = executeCommand("ls -1 /sys/class/leds", useRoot = true)
            if (res.isSuccess && res.output.isNotBlank()) {
                found.addAll(res.output.split("\n").map { "/sys/class/leds/$it" })
            }
        }
        found
    }

    /**
     * HyperOS Porting Documentation & Guides for Nothing Phone (2a)
     */
    val portingGuides = listOf(
        PortingDocSection(
            title = "1. Phone (2a) Glyph Hardware Architecture",
            summary = "Hardware Specs: MediaTek Dimensity 7200 Pro (MT6886), Awinic AW96103 / AW21036 I2C LED Controller, 26 channels in 3 distinct zones.",
            codeSnippet = """
# Nothing Phone (2a) "pacman" / "pacmanpro" Glyph Matrix:
- Controller IC: Awinic AW21036 / AW96103 on I2C-5 (address 0x34)
- PWM Resolution: 12-bit (0 - 4095) or 8-bit (0 - 255)
- Zone Breakdown:
  * Zone 1 (Top-Left Camera Arc): 24 addressable Segment LEDs (CH 0..23)
  * Zone 2 (Top-Right Camera Slant): 1 LED Channel (CH 24)
  * Zone 3 (Bottom Ribbon Accent): 1 LED Channel (CH 25)
  Total: 26 physical addressable LED lines
            """.trimIndent(),
            language = "yaml"
        ),
        PortingDocSection(
            title = "2. Device Tree (DTS) Kernel Nodes",
            summary = "Mediatek DTS configuration required in kernel tree (arch/arm64/boot/dts/mediatek/).",
            codeSnippet = """
&i2c5 {
    status = "okay";
    clock-frequency = <400000>;

    aw21036_led: aw21036@34 {
        compatible = "awinic,aw210xx_led";
        reg = <0x34>;
        reset-gpio = <&pio 45 0>;
        vled-supply = <&mt_pmic_vldo28_ldo_reg>;
        awinic,max-current = <25>; /* mA */
        awinic,channels = <26>;
        status = "okay";
    };
};
            """.trimIndent(),
            language = "c"
        ),
        PortingDocSection(
            title = "3. HyperOS / AOSP init.rc Permissions",
            summary = "Must grant read/write access to sysfs nodes on boot so SystemUI and Glyph service can control LEDs.",
            codeSnippet = """
# /vendor/etc/init/init.glyph.rc
on boot
    # Permissions for Nothing Phone (2a) Glyph LEDs
    chmod 0666 /sys/class/leds/aw210xx_led/all_white_leds_br
    chown system system /sys/class/leds/aw210xx_led/all_white_leds_br
    chmod 0666 /sys/class/leds/aw210xx_led/single_led_br
    chown system system /sys/class/leds/aw210xx_led/single_led_br
    chmod 0666 /sys/class/leds/glyph-led-1/brightness
    chown system system /sys/class/leds/glyph-led-1/brightness
    chmod 0666 /sys/class/leds/glyph-led-2/brightness
    chown system system /sys/class/leds/glyph-led-2/brightness
    chmod 0666 /sys/class/leds/glyph-led-3/brightness
    chown system system /sys/class/leds/glyph-led-3/brightness
            """.trimIndent(),
            language = "bash"
        ),
        PortingDocSection(
            title = "4. SELinux Policies (sepolicy/glyph.te)",
            summary = "Allow system_server, hal_glyph, and apps to read/write the sysfs leds directory without denials.",
            codeSnippet = """
# /vendor/etc/selinux/vendor_sepolicy/glyph.te
type vendor_sysfs_glyph, sysfs_type, fs_type;

# Allow SystemUI & AIDL service to access glyph nodes
allow system_server sysfs_leds:file rw_file_perms;
allow system_server sysfs_leds:dir r_dir_perms;
allow platform_app sysfs_leds:file rw_file_perms;
allow hal_glyph_default sysfs_leds:file rw_file_perms;
            """.trimIndent(),
            language = "text"
        ),
        PortingDocSection(
            title = "5. AIDL HAL Interface (IGlyph.aidl)",
            summary = "Standard Android AIDL HAL definition for HyperOS framework integration.",
            codeSnippet = """
// vendor/nothing/hardware/glyph/IGlyph.aidl
package vendor.nothing.hardware.glyph;

@VintfStability
interface IGlyph {
    void setAllBrightness(int brightness);
    void setChannelBrightness(int channelId, int brightness);
    void setFrame(in int[] channelValues);
    void setProgress(int channelGroup, float progress); // 0.0 to 1.0 (for 24-arc)
    void turnOff();
}
            """.trimIndent(),
            language = "java"
        ),
        PortingDocSection(
            title = "6. HyperOS Control Center (Quick Settings Tile)",
            summary = "TileService implementation to toggle Glyph Torch or Flip to Glyph directly from HyperOS status bar.",
            codeSnippet = """
// GlyphTileService.kt for HyperOS SystemUI
class GlyphTileService : TileService() {
    override fun onClick() {
        val active = (qsTile.state == Tile.STATE_ACTIVE)
        if (active) {
            GlyphNativeBridge.turnOff()
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            GlyphNativeBridge.setAllBrightness(4095)
            qsTile.state = Tile.STATE_ACTIVE
        }
        qsTile.updateTile()
    }
}
            """.trimIndent(),
            language = "kotlin"
        )
    )
}

data class ExecutionResult(
    val command: String,
    val output: String,
    val error: String,
    val exitCode: Int,
    val isSuccess: Boolean
)
