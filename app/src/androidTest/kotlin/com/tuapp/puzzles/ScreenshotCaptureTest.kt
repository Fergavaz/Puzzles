package com.tuapp.puzzles

import android.graphics.Bitmap
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test

class ScreenshotCaptureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun capture(filename: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    @Test
    fun captureAllQaFlows() {
        // 1. Home
        Thread.sleep(1000)
        capture("qa_01_home.png")

        // 2. Tango Campaign
        rule.onNode(hasText("Tango")).performScrollTo()
        val campaignButtons = rule.onAllNodes(hasText("Jugar campaña"))
        // Index 3 is Tango (Zip, Patches, Mini Sudoku, Tango)
        campaignButtons[3].performClick()
        rule.waitForIdle()
        Thread.sleep(800)
        capture("qa_02_campaign_tango.png")

        // 3. Play Tango Level 1
        rule.onAllNodesWithText("1")[0].performClick()
        rule.waitForIdle()
        Thread.sleep(800)
        // Click Hint to test hint highlight
        rule.onNodeWithContentDescription("Pista").performClick()
        Thread.sleep(300)
        capture("qa_03_tango.png")

        // 4. Queens
        rule.onNodeWithContentDescription("Volver").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Volver").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Queens")).performScrollTo()
        val campaignButtonsQueens = rule.onAllNodes(hasText("Jugar campaña"))
        campaignButtonsQueens[4].performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("1")[0].performClick()
        rule.waitForIdle()
        Thread.sleep(800)
        rule.onNodeWithContentDescription("Pista").performClick()
        Thread.sleep(300)
        capture("qa_04_queens.png")

        // 5. Mini Sudoku
        rule.onNodeWithContentDescription("Volver").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Volver").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Mini Sudoku")).performScrollTo()
        val campaignButtonsSudoku = rule.onAllNodes(hasText("Jugar campaña"))
        campaignButtonsSudoku[2].performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("1")[0].performClick()
        rule.waitForIdle()
        Thread.sleep(800)
        rule.onNodeWithContentDescription("Pista").performClick()
        Thread.sleep(300)
        capture("qa_05_sudoku.png")

        // 6. Zip
        rule.onNodeWithContentDescription("Volver").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Volver").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Zip")).performScrollTo()
        val campaignButtonsZip = rule.onAllNodes(hasText("Jugar campaña"))
        campaignButtonsZip[0].performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("1")[0].performClick()
        rule.waitForIdle()
        Thread.sleep(800)
        rule.onNodeWithContentDescription("Pista").performClick()
        Thread.sleep(300)
        capture("qa_06_zip.png")

        // 7. Patches
        rule.onNodeWithContentDescription("Volver").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Volver").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Patches")).performScrollTo()
        val campaignButtonsPatches = rule.onAllNodes(hasText("Jugar campaña"))
        campaignButtonsPatches[1].performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("1")[0].performClick()
        rule.waitForIdle()
        Thread.sleep(800)
        rule.onNodeWithContentDescription("Pista").performClick()
        Thread.sleep(300)
        capture("qa_07_patches.png")

        // 8. Test Victory Dialog by completing with hints
        repeat(15) {
            try {
                rule.onNodeWithContentDescription("Pista").performClick()
                Thread.sleep(100)
            } catch (_: Exception) {}
        }
        Thread.sleep(1000)
        capture("qa_08_victory_dialog.png")
    }
}
