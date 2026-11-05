package com.example.ondavital

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ondavital.ui.BienvenidaIlustrada
import com.example.ondavital.ui.theme.OndaVitalTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class BienvenidaVisualTest {
    @get:Rule val compose = createComposeRule()
    @Test fun ilustracionCompletaYTextoReal() {
        compose.setContent { OndaVitalTheme { BienvenidaIlustrada() } }
        compose.onNodeWithText("OndaVital").assertDoesNotExist()
        compose.onNodeWithText("Un momento para ti").assertDoesNotExist()
        compose.onNodeWithContentDescription("La lunita lee su diario bajo un cielo de estrellas").assertIsDisplayed()
        val i = InstrumentationRegistry.getInstrumentation()
        i.waitForIdleSync()
        android.os.SystemClock.sleep(300)
        i.uiAutomation.takeScreenshot().let { b ->
            File(i.targetContext.getExternalFilesDir(null), "bienvenida.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
            b.recycle()
        }
    }
}
