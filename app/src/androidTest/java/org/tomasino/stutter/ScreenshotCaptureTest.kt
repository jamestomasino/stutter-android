package org.tomasino.stutter

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {
    private val timeoutMs = 5_000L

    @Test
    fun captureScreenshots() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val context = instrumentation.targetContext

        val outputDir = File("/sdcard/Pictures/StutterScreenshots").also { dir ->
            device.executeShellCommand("mkdir -p ${dir.absolutePath}")
        }

        captureSet(device, context, outputDir, isDark = false)
        captureSet(device, context, outputDir, isDark = true)
    }

    private fun captureSet(device: UiDevice, context: Context, outputDir: File, isDark: Boolean) {
        val mode = if (isDark) "yes" else "no"
        device.executeShellCommand("cmd uimode night $mode")
        device.waitForIdle()

        ActivityScenario.launch(ReaderActivity::class.java).use {
            device.wait(Until.hasObject(By.desc(context.getString(R.string.action_play))), timeoutMs)

            ensureInputShelfExpanded(device, context)
            takeScreenshot(device, outputDir, if (isDark) "3_dark_home" else "1_light_home")

            startPlayback(device, context)
            takeScreenshot(device, outputDir, if (isDark) "4_dark_play" else "2_light_play")
        }
    }

    private fun ensureInputShelfExpanded(device: UiDevice, context: Context) {
        val expandLabel = context.getString(R.string.action_expand_input_shelf)
        val expand = device.findObject(By.desc(expandLabel))
        if (expand != null) {
            expand.click()
            device.waitForIdle()
        }
    }

    private fun startPlayback(device: UiDevice, context: Context) {
        val playLabel = context.getString(R.string.action_play)
        val play = device.findObject(By.desc(playLabel))
        play?.click()
        device.waitForIdle()
    }

    private fun takeScreenshot(device: UiDevice, outputDir: File, name: String) {
        val file = File(outputDir, "$name.png")
        device.takeScreenshot(file)
    }
}
