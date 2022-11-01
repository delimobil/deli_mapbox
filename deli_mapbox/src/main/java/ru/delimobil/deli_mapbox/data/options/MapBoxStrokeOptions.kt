package ru.delimobil.deli_mapbox.data.options

data class MapBoxStrokeOptions(
    override val width: Float,
    override val color: Int,
    @Deprecated("MapBox does not support dynamic line-dash-array https://docs.mapbox.com/mapbox-gl-js/style-spec/layers/#paint-line-line-dasharray")
    val pattern: Pattern? = null
) : MapBoxSingleStrokeOptions(width, color) {

    data class Pattern(
        val gap: Float,
        val dash: Float
    )
}