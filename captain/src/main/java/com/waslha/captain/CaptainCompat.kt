package com.waslha.captain

import androidx.compose.material3.NavigationBarItem as Material3NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

fun Modifier.weight(weight: Float): Modifier = this

@Composable
fun NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit
) {
    Material3NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = label
    )
}
