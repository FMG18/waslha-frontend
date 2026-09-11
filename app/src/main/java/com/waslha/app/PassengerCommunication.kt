package com.waslha.app

import android.content.Context
import android.content.Intent
import android.net.Uri

fun callDriver(context: Context, phone: String?): Boolean {
    val normalized = phone?.trim().orEmpty()
    if (normalized.isBlank()) return false
    return runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalized")))
        true
    }.getOrDefault(false)
}
