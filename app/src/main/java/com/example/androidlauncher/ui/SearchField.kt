package com.example.androidlauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Recomposition-stabiles Suchfeld (Lupe + Placeholder + [BasicTextField]),
 * geteilt von App-Drawer und Hybrid-Suche.
 */
@Composable
fun StableSearchFieldContent(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    textColor: Color,
    placeholderColor: Color,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    leadingIconTint: Color = placeholderColor,
    leadingIconSize: Dp = 18.dp,
    spacing: Dp = 12.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = leadingIconTint,
            modifier = Modifier.size(leadingIconSize)
        )
        Spacer(modifier = Modifier.width(spacing))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = textStyle.copy(color = placeholderColor),
                    maxLines = 1
                )
            }
            val textFieldModifier = if (focusRequester != null) {
                Modifier.fillMaxWidth().focusRequester(focusRequester)
            } else {
                Modifier.fillMaxWidth()
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = textFieldModifier,
                textStyle = textStyle.copy(color = textColor),
                cursorBrush = SolidColor(textColor),
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions
            )
        }
    }
}
