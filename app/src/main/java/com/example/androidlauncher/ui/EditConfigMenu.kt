package com.example.androidlauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled

@Composable
fun EditConfigMenu(
    onClose: () -> Unit
) {
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Bearbeiten",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = mainTextColor)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Platzhalter für zukünftige Funktionen
        Text(
            "Hier können bald weitere Bearbeitungsfunktionen hinzugefügt werden.",
            color = mainTextColor.copy(alpha = 0.7f),
            fontSize = 16.sp
        )
    }
}
