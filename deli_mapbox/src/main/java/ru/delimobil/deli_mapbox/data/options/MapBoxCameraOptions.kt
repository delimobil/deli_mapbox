package ru.delimobil.deli_mapbox.data.options

data class MapBoxCameraOptions(
    val animate: Boolean,
    val padding: Int = 0,
    val zoom: Double? = null,
    val tilt: Double? = null,
    val bearing: Double? = null
)