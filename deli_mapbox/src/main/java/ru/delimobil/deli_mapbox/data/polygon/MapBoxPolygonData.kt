package ru.delimobil.deli_mapbox.data.polygon

import ru.delimobil.deli_mapbox.alias.MapBoxPolygonsHoles
import ru.delimobil.deli_mapbox.alias.MapBoxPolygonsOutlines
import ru.delimobil.deli_mapbox.data.options.MapBoxFillOptions
import ru.delimobil.deli_mapbox.data.options.MapBoxStrokeOptions

data class MapBoxPolygonData(
    val outlines: MapBoxPolygonsOutlines,
    val holes: MapBoxPolygonsHoles,
    val fill: MapBoxFillOptions?,
    val stroke: MapBoxStrokeOptions?
)