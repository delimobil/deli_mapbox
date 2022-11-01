package ru.delimobil.deli_mapbox.data

data class MapBoxPolyline(
    val groupId: String,
    val options: Options
) {

    data class Options(
        val points: List<MapBoxCoordinates>,
        val color: Int,
        val width: Float
    )
}