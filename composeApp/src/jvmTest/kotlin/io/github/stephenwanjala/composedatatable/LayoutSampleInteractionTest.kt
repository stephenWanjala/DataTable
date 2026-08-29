package io.github.stephenwanjala.composedatatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

/** Walks the Saved Layout sample's controls the way a person testing it would. */
@OptIn(ExperimentalTestApi::class)
class LayoutSampleInteractionTest {

    @Test
    fun `the saved layout sample survives its own controls`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(1200.dp, 800.dp)) { LayoutSample() }
        }

        onNodeWithText("Alice Smith").assertIsDisplayed()

        onNodeWithText("Salary first").performClick()
        onNodeWithText("Save").performClick()
        onNodeWithText("Reset").performClick()
        onNodeWithText("Restore").performClick()
        onNodeWithText("No data available").assertDoesNotExist()
        onNodeWithText("Alice Smith").assertIsDisplayed()

        onNodeWithContentDescription("Show or hide columns").performClick()
        onNode(isToggleable() and hasText("Email")).performClick()
        onNodeWithText("No data available").assertDoesNotExist()
        onNodeWithText("Alice Smith").assertIsDisplayed()
    }
}
