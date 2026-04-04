package com.example.androidlauncher

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appDrawerOpensCorrectly() {
        // Check if the home screen is displayed
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()

        // Perform a swipe up gesture to open the app drawer
        composeTestRule.onNodeWithTag("home_screen").performTouchInput {
            swipeUp()
        }

        // Check if the app drawer is displayed
        composeTestRule.onNodeWithTag("app_drawer").assertIsDisplayed()
    }

    @Test
    fun searchFunctionWorks() {
        // Open the app drawer
        composeTestRule.onNodeWithTag("home_screen").performTouchInput {
            swipeUp()
        }

        // Type a common string into the search field (most devices have "Settings" or "System")
        val searchText = "Sett"
        composeTestRule.onNodeWithTag("search_field").performTextInput(searchText)

        // Check if an app with "Sett" in its name is displayed
        // We use wait because of the cascade animation in the drawer
        composeTestRule.waitUntil(3000) {
            composeTestRule.onAllNodesWithText(searchText, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onAllNodesWithText(searchText, substring = true, ignoreCase = true)[0].assertIsDisplayed()
    }

    @Test
    fun favoritesAreDisplayed() {
        // Initially, we check if either the add button or some favorites are there.
        // For a clean test, we assume a state, but here we just try to add one.
        
        // Click the settings button
        composeTestRule.onNodeWithTag("settings_button").performClick()

        // Click on "Favoriten konfigurieren"
        composeTestRule.onNodeWithText("Favoriten konfigurieren").performClick()
        
        // Wait for the favorites config menu to appear
        composeTestRule.onNodeWithTag("favorites_config_menu").assertIsDisplayed()

        // Select the first app in the list to be sure we find one
        composeTestRule.onAllNodes(hasTestTag("config_app_item_"))[0].performClick()

        // Confirm the selection
        composeTestRule.onNodeWithTag("confirm_favorites").performClick()

        // Check if at least one favorite item is now displayed on the home screen
        composeTestRule.onAllNodes(hasTestTag("favorite_item_"))[0].assertIsDisplayed()
    }

    @Test
    fun homeSearchButtonOpensBottomSearchField() {
        composeTestRule.onNodeWithTag("home_search_button").assertIsDisplayed().performClick()

        composeTestRule.waitUntil(3_000) {
            composeTestRule.onAllNodesWithTag("bottom_search_field").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("bottom_search_field").assertIsDisplayed().assertIsFocused()
    }

    @Test
    fun shakeGesturesSwitchIsReachableFromEditMenu() {
        composeTestRule.onNodeWithTag("settings_button").assertIsDisplayed().performClick()

        composeTestRule.waitUntil(3_000) {
            composeTestRule.onAllNodesWithTag("settings_palette_item_edit").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("settings_palette_item_edit").performClick()

        composeTestRule.waitUntil(3_000) {
            composeTestRule.onAllNodesWithTag("shake_gestures_switch").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("shake_gestures_switch").assertIsDisplayed()
    }

    @Test
    fun homeEditModeOpensOnlyThroughEditSubmenuGeneralOption() {
        composeTestRule.onNodeWithTag("settings_button").assertIsDisplayed().performClick()

        composeTestRule.waitUntil(3_000) {
            composeTestRule.onAllNodesWithTag("settings_palette_item_edit").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_palette_item_edit").performClick()

        composeTestRule.waitUntil(3_000) {
            composeTestRule.onAllNodesWithTag("edit_home_layout_item").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("edit_home_layout_item").performClick()

        composeTestRule.waitUntil(3_000) {
            composeTestRule.onAllNodesWithTag("home_edit_controls").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("home_edit_controls").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_edit_move_up").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_edit_move_down").assertDoesNotExist()

        composeTestRule.onNodeWithTag("home_edit_target_clock").performTouchInput { click() }
        composeTestRule.onNodeWithTag("home_edit_move_up").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_edit_move_down").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_edit_target_favorites").performTouchInput { click() }
        composeTestRule.onNodeWithTag("home_edit_move_up").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_edit_move_down").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_screen").performTouchInput {
            click(Offset(x = width - 8f, y = 8f))
        }

        composeTestRule.waitUntil(3_000) {
            composeTestRule.onAllNodesWithTag("home_edit_move_up").fetchSemanticsNodes().isEmpty()
        }
    }
}
