package com.waslha.app

data class SavedPlace(val title: String, val address: String, val emoji: String)

data class PaymentMethod(val title: String, val subtitle: String, val emoji: String)

data class NotificationItem(val title: String, val body: String, val time: String, val unread: Boolean)

data class SupportTopic(val title: String, val body: String)
