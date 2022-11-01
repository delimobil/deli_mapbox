package ru.delimobil.deli_mapbox.data

import ru.delimobil.deli_mapbox.alias.MapBoxPolygonsHoles
import ru.delimobil.deli_mapbox.alias.MapBoxPolygonsOutlines
import ru.delimobil.deli_mapbox.data.options.MapBoxFillOptions
import ru.delimobil.deli_mapbox.data.options.MapBoxStrokeOptions

data class MapBoxPolygon(
    val groupId: String,
    val options: Options
) {

    data class Options(
        val data: DataSource,
        val fillOptions: MapBoxFillOptions? = null,
        val strokeOptions: MapBoxStrokeOptions? = null
    ) {

        sealed class DataSource {
            data class Coordinates(
                val lines: List<Lines>
            ) : DataSource() {

                data class Lines(
                    val outlines: MapBoxPolygonsOutlines,
                    val holes: MapBoxPolygonsHoles
                )
            }

            data class GeoJson(
                val json: String
            ) : DataSource()
        }
    }

}