package com.xz.vibenote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.xz.vibenote.data.NoteDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NoteFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = NoteDatabase.getDatabase(context)
        runBlocking { db.clearAllTables() }
        // Wait for UI to reflect the cleared state
        composeRule.waitForIdle()
    }

    @Test
    fun fullNoteLifecycle_createEditDelete() {
        val noteText = "Test note ${System.currentTimeMillis()}"
        val editedText = "Edited note ${System.currentTimeMillis()}"

        // 1. Verify empty state
        composeRule.onNodeWithText("No notes yet").assertIsDisplayed()
        composeRule.onNodeWithText("VibeNote").assertIsDisplayed()

        // 2. Type a note and save
        composeRule.onNodeWithTag("noteInput").performTextInput(noteText)
        composeRule.onNodeWithContentDescription("Save note").performClick()
        composeRule.waitForIdle()

        // 3. Verify the note appears and empty state is gone
        composeRule.onNodeWithText(noteText).assertIsDisplayed()
        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onNodeWithText("No notes yet").assertDoesNotExist()

        // 4. Verify input field was cleared
        composeRule.onNodeWithTag("noteInput").assertTextContains("")

        // 5. Tap edit on the note
        composeRule.onNodeWithContentDescription("Edit").performClick()
        composeRule.waitForIdle()

        // 6. Verify input field is populated with the note text
        composeRule.onNodeWithTag("noteInput").assertTextContains(noteText)

        // 7. Cancel button should be visible in edit mode
        composeRule.onNodeWithContentDescription("Cancel").assertIsDisplayed()

        // 8. Clear and type new text, then save
        composeRule.onNodeWithTag("noteInput").performTextClearance()
        composeRule.onNodeWithTag("noteInput").performTextInput(editedText)
        composeRule.onNodeWithContentDescription("Save note").performClick()
        composeRule.waitForIdle()

        // 9. Verify the edited note appears and old text is gone
        composeRule.onNodeWithText(editedText).assertIsDisplayed()
        composeRule.onNodeWithText(noteText).assertDoesNotExist()

        // 10. Delete the note
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.waitForIdle()

        // 11. Verify note is removed and empty state returns
        composeRule.onNodeWithText(editedText).assertDoesNotExist()
        composeRule.onNodeWithText("No notes yet").assertIsDisplayed()
    }

    @Test
    fun createMultipleNotes_allDisplayed() {
        val note1 = "First note ${System.currentTimeMillis()}"
        val note2 = "Second note ${System.currentTimeMillis()}"

        // Create first note
        composeRule.onNodeWithTag("noteInput").performTextInput(note1)
        composeRule.onNodeWithContentDescription("Save note").performClick()
        composeRule.waitForIdle()

        // Create second note
        composeRule.onNodeWithTag("noteInput").performTextInput(note2)
        composeRule.onNodeWithContentDescription("Save note").performClick()
        composeRule.waitForIdle()

        // Both notes should be visible
        composeRule.onNodeWithText(note1).assertIsDisplayed()
        composeRule.onNodeWithText(note2).assertIsDisplayed()
        composeRule.onNodeWithText("Today").assertIsDisplayed()

        // Clean up — delete both notes
        composeRule.onAllNodesWithContentDescription("Delete")[0].performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithContentDescription("Delete")[0].performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun editMode_cancelRestoresEmptyInput() {
        val noteText = "Cancel test ${System.currentTimeMillis()}"

        // Create a note
        composeRule.onNodeWithTag("noteInput").performTextInput(noteText)
        composeRule.onNodeWithContentDescription("Save note").performClick()
        composeRule.waitForIdle()

        // Tap edit — only one note so single Edit button
        composeRule.onNodeWithContentDescription("Edit").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("noteInput").assertTextContains(noteText)

        // Tap cancel
        composeRule.onNodeWithContentDescription("Cancel").performClick()
        composeRule.waitForIdle()

        // Input should be cleared, original note unchanged
        composeRule.onNodeWithTag("noteInput").assertTextContains("")
        composeRule.onNodeWithText(noteText).assertIsDisplayed()

        // Clean up
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.waitForIdle()
    }
}
