package com.example.androidlauncher

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
}
