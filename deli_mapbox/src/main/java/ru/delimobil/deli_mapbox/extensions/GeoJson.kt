package ru.delimobil.deli_mapbox.extensions

import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.GeoJson
import com.mapbox.geojson.Point
import ru.delimobil.deli_mapbox.data.MapBoxCoordinates

fun mapToZoneBounds(geoJson: String): List<MapBoxCoordinates> {
    val coordinates = mutableListOf<MapBoxCoordinates>()
    FeatureCollection.fromJson(geoJson).bbox()?.let { bbox ->
        coordinates.addAll(
            listOf(
                bbox.northeast().toMapBoxCoordinates(),
                bbox.southwest().toMapBoxCoordinates()
            )
        )
    }
    return coordinates
}

fun GeoJson.mapToZoneBounds(): List<MapBoxCoordinates> {
    return mapToZoneBounds(this.toJson())
}

private fun Point.toMapBoxCoordinates() = MapBoxCoordinates(latitude(), longitude())