package com.example.androidlauncher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidlauncher.data.AppAccessMode
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AutoIconRuleMode
import com.example.androidlauncher.ui.AppDrawer
import com.example.androidlauncher.ui.EditConfigMenu
import com.example.androidlauncher.ui.HomeScreen
import com.example.androidlauncher.ui.IconConfigMenu
import com.example.androidlauncher.ui.theme.AndroidLauncherTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LauncherComposableUiTest {

    // Hilt-Rule vor der Compose-Rule: macht den DI-Graphen verfuegbar, bevor die
    // @AndroidEntryPoint-Host-Activity startet (AppDrawer nutzt hiltViewModel()).
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<DebugComposeHostActivity>()

    @Test
    fun homeScreen_rendersCoreControls() {
        composeRule.setContent {
            AndroidLauncherTheme {
                HomeScreen(
                    favorites = emptyList(),
                    isSettingsOpen = false,
                    isSearchOpen = false,
                    onToggleEditMode = {},
                    onOpenDrawer = {},
                    onOpenSearch = {},
                    onToggleSettings = {},
                    onOpenFavoritesConfig = {},
                    onOpenColorConfig = {},
                    onOpenSizeConfig = {},
                    onOpenSystemSettings = {},
                    onOpenInfo = {},
                    onSaveHomeLayout = { },
                    onLaunchApp = { _, _, _ -> },
                    returnIconPackage = null
                )
            }
        }

        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("home_search_button").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_button").assertIsDisplayed()
    }

    @Test
    fun appDrawer_rendersRootAndSearchField() {
        composeRule.setContent {
            AndroidLauncherTheme {
                AppDrawer(
                    apps = listOf(AppInfo(label = "Firefox", packageName = "org.mozilla.firefox")),
                    folders = emptyList(),
                    onToggleFavorite = {},
                    isFavorite = { false },
                    onUpdateFolders = {},
                    onOpenFolderConfig = {},
                    onClose = {},
                    onLaunchApp = { _, _, _ -> },
                    returnIconPackage = null
                )
            }
        }

        composeRule.onNodeWithTag("app_drawer").assertIsDisplayed()
        composeRule.onNodeWithTag("app_drawer_search_field").assertIsDisplayed()
    }

    @Test
    fun iconConfigMenu_exposesActionDialogAndRuleCallback() {
        var selectedRule: AutoIconRuleMode? = null
        var reanalyzedPackage: String? = null

        composeRule.setContent {
            AndroidLauncherTheme {
                IconConfigMenu(
                    apps = listOf(AppInfo(label = "Firefox", packageName = "org.mozilla.firefox")),
                    customIcons = emptyMap(),
                    iconRules = emptyMap(),
                    onIconSelected = { _, _ -> },
                    onAutoRuleSelected = { _, mode -> selectedRule = mode },
                    onReanalyzeRequested = { pkg -> reanalyzedPackage = pkg },
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithTag("icon_config_item_org.mozilla.firefox").performClick()
        composeRule.onNodeWithTag("icon_action_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("icon_action_force_fallback").assertIsDisplayed().performClick()

        assertEquals(AutoIconRuleMode.FORCE_FALLBACK, selectedRule)

        composeRule.onNodeWithTag("icon_config_item_org.mozilla.firefox").performClick()
        composeRule.onNodeWithTag("icon_action_reanalyze").assertIsDisplayed().performClick()

        assertEquals("org.mozilla.firefox", reanalyzedPackage)
    }

    @Test
    fun editConfigMenu_showsHomeLayoutResetWhenCustomLayoutIsSet() {
        var wasHomeLayoutReset = false

        composeRule.setContent {
            AndroidLauncherTheme {
                EditConfigMenu(
                    onOpenHomeLayoutEdit = {},
                    onResetHomeLayout = { wasHomeLayoutReset = true },
                    onOpenIconConfig = {},
                    onOpenUninstallApps = {},
                    onOpenHiddenApps = {},
                    onOpenAppLock = {},
                    onOpenDefaultLauncher = {},
                    onChangeWallpaper = {},
                    onResetWallpaper = {},
                    onOpenWallpaperAdjust = {},
                    isCustomHomeLayoutSet = true,
                    isCustomWallpaperSet = false,
                    onOpenGesturesConfig = {},
                    isSmartSuggestionsEnabled = false,
                    onSmartSuggestionsToggled = {},
                    isAnimationsEnabled = true,
                    onAnimationsToggled = {},
                    onOpenAnimationsConfig = {},
                    isWeatherWidgetEnabled = false,
                    onWeatherWidgetToggled = {},
                    isClockWidgetEnabled = true,
                    onClockWidgetToggled = {},
                    isCalendarWidgetEnabled = false,
                    onCalendarWidgetToggled = {},
                    appAccessMode = AppAccessMode.HOME_LIST,
                    onAppAccessModeChange = {},
                    onClearSearchHistory = {},
                    isHapticFeedbackEnabled = true,
                    onHapticFeedbackToggled = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithTag("edit_home_layout_reset").assertIsDisplayed().performClick()
        assertEquals(true, wasHomeLayoutReset)
    }

    @Test
    fun editConfigMenu_hidesHomeLayoutResetWhenLayoutIsDefault() {
        composeRule.setContent {
            AndroidLauncherTheme {
                EditConfigMenu(
                    onOpenHomeLayoutEdit = {},
                    onResetHomeLayout = {},
                    onOpenIconConfig = {},
                    onOpenUninstallApps = {},
                    onOpenHiddenApps = {},
                    onOpenAppLock = {},
                    onOpenDefaultLauncher = {},
                    onChangeWallpaper = {},
                    onResetWallpaper = {},
                    onOpenWallpaperAdjust = {},
                    isCustomHomeLayoutSet = false,
                    isCustomWallpaperSet = false,
                    onOpenGesturesConfig = {},
                    isSmartSuggestionsEnabled = false,
                    onSmartSuggestionsToggled = {},
                    isAnimationsEnabled = true,
                    onAnimationsToggled = {},
                    onOpenAnimationsConfig = {},
                    isWeatherWidgetEnabled = false,
                    onWeatherWidgetToggled = {},
                    isClockWidgetEnabled = true,
                    onClockWidgetToggled = {},
                    isCalendarWidgetEnabled = false,
                    onCalendarWidgetToggled = {},
                    appAccessMode = AppAccessMode.HOME_LIST,
                    onAppAccessModeChange = {},
                    onClearSearchHistory = {},
                    isHapticFeedbackEnabled = true,
                    onHapticFeedbackToggled = {},
                    onClose = {}
                )
            }
        }

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithTag("edit_home_layout_reset").fetchSemanticsNodes().isEmpty()
        }
    }
}
