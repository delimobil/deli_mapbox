package ru.delimobil.deli_mapbox.extensions

import com.mapbox.mapboxsdk.geometry.LatLng
import ru.delimobil.deli_mapbox.data.MapBoxCoordinates

fun LatLng.toMapBoxCoordinates(): MapBoxCoordinates {
    return MapBoxCoordinates(
        lat = latitude,
        lon = longitude
    )
}