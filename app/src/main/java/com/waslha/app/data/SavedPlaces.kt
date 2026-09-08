package com.waslha.app.data

enum class SavedPlaceType { HOME, WORK, FAVORITE }

data class SavedPlace(
    val id: String,
    val type: SavedPlaceType,
    val title: String,
    val address: String,
    val note: String = ""
)

object SavedPlacesDefaults {
    val items = listOf(
        SavedPlace("home", SavedPlaceType.HOME, "المنزل", "لم يتم تحديد العنوان"),
        SavedPlace("work", SavedPlaceType.WORK, "العمل", "لم يتم تحديد العنوان")
    )
}
