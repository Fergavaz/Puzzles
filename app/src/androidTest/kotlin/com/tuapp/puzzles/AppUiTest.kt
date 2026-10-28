package com.tuapp.puzzles

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test

class AppUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsAllFiveGames() {
        rule.onNodeWithText("Puzzles").assertExists()
        rule.onNodeWithText("Zip").assertExists()
        rule.onNodeWithText("Patches").assertExists()
        rule.onNodeWithText("Mini Sudoku").assertExists()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Tango"))
        rule.onNodeWithText("Tango").assertExists()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Queens"))
        rule.onNodeWithText("Queens").assertExists()
    }

    @Test
    fun campaignFreePlayShowsLevelsAndAreClickable() {
        // Open campaign for Tango from Home
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Tango"))
        val campaignButtons = rule.onAllNodes(hasText("Jugar campaña"))
        // First is Zip, then Patches, Mini Sudoku, Tango (index 3)
        campaignButtons[3].performClick()

        // Verify Campaign screen opened
        rule.onNodeWithText("Tango · Campaña").assertExists()
        // Verify free play levels are displayed and clickable
        rule.onNodeWithText("1").assertExists().performClick()
    }
}
