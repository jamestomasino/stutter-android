package org.tomasino.stutter

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.tomasino.stutter.reader.ReaderView
import org.tomasino.stutter.settings.AppearanceOptions

@RunWith(AndroidJUnit4::class)
class ReaderViewInstrumentedTest {
    @Test
    fun readerViewDrawsWithoutCrash() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = ReaderView(context)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            view.setAppearance(AppearanceOptions.DEFAULT)
            view.setShowFlankers(true)
            view.setWord("hello", "world")
            view.measure(
                ViewMeasureSpec.makeMeasureSpec(1080, ViewMeasureSpec.EXACTLY),
                ViewMeasureSpec.makeMeasureSpec(1920, ViewMeasureSpec.EXACTLY),
            )
            view.layout(0, 0, 1080, 1920)
            val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
        }
    }
}

private object ViewMeasureSpec {
    fun makeMeasureSpec(size: Int, mode: Int): Int = android.view.View.MeasureSpec.makeMeasureSpec(size, mode)
    const val EXACTLY: Int = android.view.View.MeasureSpec.EXACTLY
}
