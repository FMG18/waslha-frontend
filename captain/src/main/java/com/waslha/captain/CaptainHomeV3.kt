package com.waslha.captain

import androidx.compose.runtime.Composable

/**
 * Compatibility entry point kept stable for existing callers.
 * The implementation lives in CaptainHomeV3Fixed.kt so the legacy file no longer
 * contains the previous compile-breaking Modifier.align usage.
 */
@Composable
fun CaptainHomeV3(session: CaptainSession, onLogout: () -> Unit) {
    CaptainHomeV3Fixed(session, onLogout)
}
