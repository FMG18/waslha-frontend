package com.waslha.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Compatibility overload for the saved-places screen's older map call shape.
 * It delegates to the current WaslhaRideMap API without duplicating map logic.
 */
@Composable
fun WaslhaRideMap(
    initialCenter: Coordinates,
    initialDestination: Coordinates?,
    modifier: Modifier = Modifier,
    onDestinationSelected: (Coordinates) -> Unit = {},
    onClose: () -> Unit = {}
) {
    WaslhaRideMap(
        pickup = initialCenter,
        destination = initialDestination,
        modifier = modifier,
        onDestinationPicked = onDestinationSelected
    )
}
