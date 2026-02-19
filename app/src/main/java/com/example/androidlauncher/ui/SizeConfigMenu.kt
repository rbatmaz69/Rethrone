package com.example.androidlauncher.ui

import androidx.compose.foundation.background
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

@Composable
fun SizeConfigMenu(
    onClose: () -> Unit
) {
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
            Column {
                Text(
                    text = "Größe & Skalierung",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Schließen",
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Placeholder für die kommenden Kriterien
        Text(
            text = "Inhalt für Größenanpassung folgt...",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp
        )
    }
}
