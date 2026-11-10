package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AutoCleanFrequency
import com.example.model.CleanOutcome
import com.example.model.ShizukuStatus
import com.example.model.ThemeMode
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Cache Cleaner", appName)
  }

  @Test
  fun `format bytes correctly`() {
    assertEquals("0 B", FormatUtils.formatBytes(0L))
    assertEquals("1.0 KB", FormatUtils.formatBytes(1024L))
    assertEquals("1.0 MB", FormatUtils.formatBytes(1024L * 1024L))
    assertEquals("2.5 GB", FormatUtils.formatBytes((2.5 * 1024 * 1024 * 1024).toLong()))
  }

  @Test
  fun `verify theme and frequency enum consistency`() {
    assertEquals(ThemeMode.SYSTEM, ThemeMode.valueOf("SYSTEM"))
    assertEquals(ThemeMode.OLED_BLACK, ThemeMode.valueOf("OLED_BLACK"))
    assertEquals("Diariamente", AutoCleanFrequency.DAILY.label)
    assertEquals(1, AutoCleanFrequency.DAILY.days)
    assertEquals(CleanOutcome.SUCCESS, CleanOutcome.valueOf("SUCCESS"))
    assertTrue(ShizukuStatus.Authorized(13).isAvailable)
  }
}
