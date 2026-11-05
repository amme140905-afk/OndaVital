package com.example.ondavital

import android.graphics.Bitmap
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ondavital.ui.PantallaAnalisis
import com.example.ondavital.ui.OndaVitalApp
import com.example.ondavital.analisis.hoyDiario
import com.example.ondavital.data.DiarioLocal
import androidx.compose.ui.platform.LocalContext
import com.example.ondavital.ui.components.FondoPapel
import com.example.ondavital.ui.theme.OndaVitalTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class AnalisisIntegracionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun exportacionDesdePerfilEscribeElArchivoCompleto() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val id = java.util.UUID.randomUUID().toString()
        val directorio = File(base.cacheDir, "export-ui-$id").apply { mkdirs() }
        val salida = File(directorio, "exportacion.json")
        val contexto = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = directorio
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences("export-ui-$id-$name", mode)
        }
        val propietario = object : androidx.activity.result.ActivityResultRegistryOwner {
            override val activityResultRegistry = object : androidx.activity.result.ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int,
                    contract: androidx.activity.result.contract.ActivityResultContract<I, O>, input: I,
                    options: androidx.core.app.ActivityOptionsCompat?) {
                    dispatchResult(requestCode, android.app.Activity.RESULT_OK, android.content.Intent().setData(android.net.Uri.fromFile(salida)))
                }
            }
        }
        compose.setContent {
            CompositionLocalProvider(LocalContext provides contexto,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides propietario) {
                OndaVitalTheme { OndaVitalApp(false, false, false, {}) }
            }
        }
        compose.waitUntil(15000) { compose.onAllNodesWithText("Perfil").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Perfil").performClick()
        compose.onNodeWithText("Exportar todos mis datos").performScrollTo().performClick()
        compose.waitUntil(15000) {
            runCatching { org.json.JSONObject(salida.readText()).getJSONObject("diario").getJSONArray("registros").length() == 90 }.getOrDefault(false)
        }
        val json = org.json.JSONObject(salida.readText())
        org.junit.Assert.assertTrue(json.getJSONArray("actividadesPersonales").length() >= 8)
        org.junit.Assert.assertFalse(json.has("seguridad"))
        org.junit.Assert.assertTrue(json.getJSONObject("diario").getJSONArray("registros").getJSONObject(0).getString("nota").length > 20)
    }

    @Test fun calendarioYAnalisisUsanElMismoDiarioDemo() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val id = java.util.UUID.randomUUID().toString()
        val directorio = File(base.cacheDir, "ui-demo-$id").apply { mkdirs() }
        val contexto = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = directorio
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences("ui-$id-$name", mode)
        }
        compose.setContent {
            val propietario = requireNotNull(androidx.activity.compose.LocalActivityResultRegistryOwner.current)
            CompositionLocalProvider(LocalContext provides contexto,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides propietario) {
                OndaVitalTheme { OndaVitalApp(false, false, false, {}) }
            }
        }
        compose.waitUntil(15000) { compose.onAllNodesWithText("Diario de demostración · datos de ejemplo").fetchSemanticsNodes().isNotEmpty() }
        org.junit.Assert.assertEquals(90, DiarioLocal(contexto).leer().size)
        compose.onNodeWithText("Calendario").performClick()
        compose.onNodeWithContentDescription("${hoyDiario()}, 1 registros").assertExists()
        captura("calendario-demo-global.png")
        compose.onNodeWithText("Análisis").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("tu energía fue 3 puntos mayor", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Media ", substring = true).assertCountEquals(0)
        captura("analisis-natural.png")
        compose.onNodeWithText("Mi mes en PDF").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Guardar mi mes en PDF").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("‹ Mes anterior").performClick()
        compose.onNodeWithText("‹  Volver a Análisis").performScrollTo().performClick()
        compose.onNodeWithText("Perfil").performClick()
        compose.onNodeWithText("Crear PIN para usar mi huella").performScrollTo().assertIsDisplayed()
        captura("perfil-huella.png")
    }

    @Test fun demoCambiaRangoYRegresaAlDiarioVacio() {
        compose.setContent {
            var dias by remember { mutableIntStateOf(7) }
            OndaVitalTheme {
                FondoPapel {
                    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        PantallaAnalisis(emptyList(), dias, { dias = it }, {})
                    }
                }
            }
        }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Tus mareas aún están por dibujarse").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Explorar datos de demostración").performScrollTo().performClick()
        compose.onNodeWithText("90 días").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("90 días · 90 registros").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("90 días · 90 registros").performScrollTo().assertIsDisplayed()
        captura("analisis-demo.png")
        compose.onNodeWithText("Ver cómo fueron mis días").performScrollTo().performClick()
        compose.onNodeWithText("Violeta:", substring = true).performScrollTo()
        compose.waitForIdle()
        captura("tendencias-demo.png")
        compose.onAllNodesWithText("¿Por qué veo esto?").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Ver días sin actividad").assertExists()
        compose.onNodeWithText("Salir de demostración").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Tus mareas aún están por dibujarse").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Abrir mi reporte semanal").assertExists()
    }

    private fun captura(nombre: String) {
        val i = InstrumentationRegistry.getInstrumentation()
        // Esperar también la presentación de SurfaceFlinger, después del estado de Compose.
        i.waitForIdleSync()
        android.os.SystemClock.sleep(300)
        val bitmap = i.uiAutomation.takeScreenshot()
        File(i.targetContext.getExternalFilesDir(null), nombre).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
