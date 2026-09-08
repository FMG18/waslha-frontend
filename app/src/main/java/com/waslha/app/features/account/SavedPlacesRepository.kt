package com.waslha.app.features.account

class SavedPlacesRepository {
    private val places = mutableListOf(
        SavedPlace("home", "المنزل", "لم يتم تحديد العنوان بعد"),
        SavedPlace("work", "العمل", "لم يتم تحديد العنوان بعد")
    )

    fun getAll(): List<SavedPlace> = places.toList()

    fun save(place: SavedPlace) {
        places.removeAll { it.id == place.id }
        places.add(place)
    }

    fun remove(id: String) {
        places.removeAll { it.id == id }
    }
}
