package ru.delimobil.deli_mapbox.extensions

import android.location.Location
import ru.delimobil.deli_mapbox.data.MapBoxCoordinates

fun Location.toMapBoxCoordinates(): MapBoxCoordinates {
    return MapBoxCoordinates(
        lat = latitude,
        lon = longitude
    )
}