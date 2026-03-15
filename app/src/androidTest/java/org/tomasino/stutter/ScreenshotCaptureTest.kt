package org.tomasino.stutter

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.tomasino.stutter.settings.AppearanceOptions
import org.tomasino.stutter.settings.DEFAULT_COLOR_SCHEME_ID
import org.tomasino.stutter.settings.LanguageOptions
import org.tomasino.stutter.settings.PlaybackOptions
import org.tomasino.stutter.settings.SettingsRepository
import org.tomasino.stutter.settings.TextHandlingOptions
import org.tomasino.stutter.settings.applyColorScheme
import org.tomasino.stutter.settings.settingsDataStore
import java.io.File

@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {
    private val timeoutMs = 5_000L

    @Test
    fun captureScreenshots() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val context = instrumentation.targetContext
        val requestedMode = InstrumentationRegistry.getArguments().getString("uiMode")?.lowercase()

        val outputDir = File("/sdcard/Pictures/StutterScreenshots").also { dir ->
            device.executeShellCommand("mkdir -p ${dir.absolutePath}")
            device.executeShellCommand("rm -f ${dir.absolutePath}/*.png")
        }

        when (requestedMode) {
            "light" -> captureSet(device, context, outputDir, isDark = false)
            "dark" -> captureSet(device, context, outputDir, isDark = true)
            else -> {
                captureSet(device, context, outputDir, isDark = false)
                captureSet(device, context, outputDir, isDark = true)
            }
        }
    }

    private fun captureSet(device: UiDevice, context: Context, outputDir: File, isDark: Boolean) {
        setNightMode(device, isDark)
        resetSettings(context, isDark)

        val intent = Intent(context, ReaderActivity::class.java).apply {
            putExtra(ReaderActivity.EXTRA_SCREENSHOT_DARK_THEME, isDark)
        }

        ActivityScenario.launch<ReaderActivity>(intent).use {
            device.wait(Until.hasObject(By.desc(context.getString(R.string.action_play))), timeoutMs)

            ensureInputShelfExpanded(device, context)
            device.waitForIdle()
            Thread.sleep(500)
            takeScreenshot(device, outputDir, if (isDark) "3_dark_home" else "1_light_home")

            startPlayback(device, context)
            Thread.sleep(500)
            takeScreenshot(device, outputDir, if (isDark) "4_dark_play" else "2_light_play")
        }
    }

    private fun setNightMode(device: UiDevice, isDark: Boolean) {
        val mode = if (isDark) "yes" else "no"
        val modeValue = if (isDark) "2" else "1"
        device.executeShellCommand("cmd uimode night $mode")
        device.executeShellCommand("settings put secure ui_night_mode $modeValue")
        val deadline = System.currentTimeMillis() + 3_000
        while (System.currentTimeMillis() < deadline) {
            val current = device.executeShellCommand("settings get secure ui_night_mode").trim()
            if (current == modeValue) {
                break
            }
            Thread.sleep(200)
        }
        device.waitForIdle()
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
        device.executeShellCommand("screencap -p ${file.absolutePath}")
        repeat(5) { attempt ->
            Thread.sleep(200L * (attempt + 1))
            val listing = device.executeShellCommand("ls -l ${file.absolutePath}").trim()
            if (listing.contains(file.name) && !listing.contains("No such")) {
                return
            }
        }
        throw AssertionError("Failed to take screenshot via screencap: ${file.absolutePath}")
    }

    private fun resetSettings(context: Context, isDarkTheme: Boolean) {
        val repository = SettingsRepository(context.settingsDataStore, CoroutineScope(Dispatchers.IO))
        runBlocking {
            repository.setPlaybackOptions(PlaybackOptions.DEFAULT)
            repository.setTextHandlingOptions(TextHandlingOptions.DEFAULT)
            repository.setLanguageOptions(LanguageOptions.DEFAULT)
            repository.setAppearanceOptions(
                applyColorScheme(AppearanceOptions.DEFAULT, DEFAULT_COLOR_SCHEME_ID, isDarkTheme)
            )
        }
    }
}
