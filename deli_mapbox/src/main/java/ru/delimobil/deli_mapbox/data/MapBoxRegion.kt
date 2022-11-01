package ru.delimobil.deli_mapbox.data

import kotlin.math.abs

data class MapBoxRegion(
    val southWest: MapBoxCoordinates,
    val northEast: MapBoxCoordinates,
    val center: MapBoxCoordinates
) {

    fun asEffectiveRegionForMarkers(): MapBoxRegion {
        val verticalOffset = abs(northEast.lat - southWest.lat) / 4
        val horizontalOffset = abs(northEast.lon - southWest.lon) / 2

        return MapBoxRegion(
            southWest = MapBoxCoordinates(
                lat = southWest.lat - verticalOffset,
                lon = southWest.lon - horizontalOffset
            ),
            northEast = MapBoxCoordinates(
                lat = northEast.lat + verticalOffset,
                lon = northEast.lon + horizontalOffset
            ),
            center = center
        )

    }

    fun contains(coordinates: MapBoxCoordinates): Boolean {
        return containLat(coordinates.lat) && containLng(coordinates.lon)
    }

    private fun containLat(lat: Double): Boolean {
        return southWest.lat <= lat && lat <= northEast.lat
    }

    private fun containLng(lng: Double): Boolean {
        return if (southWest.lon <= northEast.lon) {
            southWest.lon <= lng && lng <= northEast.lon
        } else {
            southWest.lon <= lng || lng <= northEast.lon
        }
    }
}