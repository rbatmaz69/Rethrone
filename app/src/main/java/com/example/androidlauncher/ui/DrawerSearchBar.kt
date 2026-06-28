package com.example.androidlauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.R
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import kotlinx.coroutines.delay

/**
 * Die ausgeklappte Suchleiste der App-Drawer (erscheint unter der Kopfleiste, sobald der
 * Such-Button gedrückt wurde). Holt sich beim Erscheinen automatisch Fokus + Tastatur und
 * meldet über [onCollapse], wenn sie den Fokus verliert und leer ist (zum Einklappen).
 */
@Composable
fun DrawerSearchField(
    searchQuery: String,
    onValueChange: (String) -> Unit,
    mainTextColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean,
    fontScale: Float,
    testTag: String,
    onCollapse: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var wasFocused by remember { mutableStateOf(false) }

    // Beim Erscheinen fokussieren (kurze Verzögerung gegen Focus-Race) + Tastatur zeigen.
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { focusRequester.requestFocus() }
        keyboardController?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .designSurface(designStyle, RoundedCornerShape(16.dp), isDarkTextEnabled, surfaceAccent, fillAlpha = 0.1f)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    wasFocused = true
                } else if (wasFocused && searchQuery.isBlank()) {
                    onCollapse()
                }
            }
    ) {
        StableSearchFieldContent(
            value = searchQuery,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.search_apps),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = mainTextColor,
                fontSize = 16.sp * fontScale
            ),
            textColor = mainTextColor,
            placeholderColor = mainTextColor.copy(alpha = 0.4f),
            focusRequester = focusRequester,
            leadingIconTint = mainTextColor.copy(alpha = 0.4f),
            leadingIconSize = 20.dp,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
        )
    }
}
