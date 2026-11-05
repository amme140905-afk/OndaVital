package com.example.ondavital

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ondavital.data.DatosUsuarioLocal
import com.example.ondavital.seguridad.SeguridadLocal
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class ArranqueAccesoTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun arranqueConPinNoMuestraDiarioYConservaBorradorAlVolver() {
        // Esta prueba de arranque es exclusiva de emulador; no altera un teléfono personal.
        assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"))
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        DatosUsuarioLocal(c).borrarTodo()
        assertTrue(SeguridadLocal(c).crear("2468"))
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            compose.waitUntil(20000) { compose.onAllNodesWithText("Hola, qué bonito verte").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Inicio").assertDoesNotExist()
            listOf("1", "1", "1", "1").forEach { compose.onNodeWithText(it).performClick() }
            compose.waitUntil(10000) { compose.onAllNodesWithText("Ese PIN no coincide. Inténtalo de nuevo.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Inicio").assertDoesNotExist()
            listOf("2", "4", "6", "8").forEach { compose.onNodeWithText(it).performClick() }
            compose.waitUntil(10000) { compose.onAllNodesWithText("Inicio").fetchSemanticsNodes().isNotEmpty() }
            escenario.moveToState(Lifecycle.State.CREATED)
            escenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithText("Un momento para ti").assertDoesNotExist()
            compose.onNodeWithText("＋ Contar mi día").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Nota personal").performScrollTo().performTextInput("Una nota de prueba que todavía no guardo")
            escenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithText("＋ Contar mi día").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Nota personal").assertTextContains("Una nota de prueba que todavía no guardo")
            compose.onNodeWithText("Alegre").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Nivel de energía: sin elegir").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(7f) }
            compose.onNodeWithText("Guardar en mi diario").performScrollTo().performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("Tu momento ya tiene una página").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Seguir con mi diario").performClick()
            compose.onNodeWithText("Perfil").performClick()
            compose.onNodeWithText("Borrar mis datos de la app").performScrollTo().performClick()
            compose.onNodeWithText("Conservar mi diario").performClick()
            assertTrue(com.example.ondavital.data.DiarioLocal(c).leer().isNotEmpty())
            compose.onNodeWithText("Borrar mis datos de la app").performScrollTo().performClick()
            compose.onNodeWithText("Sí, borrar mis datos").performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("Crear mi PIN").fetchSemanticsNodes().isNotEmpty() }
            assertTrue(com.example.ondavital.data.DiarioLocal(c).leer().isEmpty())
            assertTrue(!SeguridadLocal(c).hayPin)
        }
        DatosUsuarioLocal(c).borrarTodo()
    }
}
