package ru.delimobil.deli_mapbox.extensions

import com.mapbox.mapboxsdk.geometry.LatLng
import ru.delimobil.deli_mapbox.data.MapBoxCoordinates

fun MapBoxCoordinates.toLatLng(): LatLng {
    return LatLng(lat, lon)
}