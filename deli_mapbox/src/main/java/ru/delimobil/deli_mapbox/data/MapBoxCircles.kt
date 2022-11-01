package ru.delimobil.deli_mapbox.data

import ru.delimobil.deli_mapbox.data.options.MapBoxFillOptions
import ru.delimobil.deli_mapbox.data.options.MapBoxSingleStrokeOptions

data class MapBoxCircles(
    val groupId: String,
    val linePattern: Pattern? = null,
    val options: List<Options>
) {

    data class Pattern(
        val gap: Float,
        val dash: Float
    )

    data class Options(
        val center: MapBoxCoordinates,
        val radius: Float,
        val stroke: MapBoxSingleStrokeOptions? = null,
        val fill: MapBoxFillOptions
    )
}