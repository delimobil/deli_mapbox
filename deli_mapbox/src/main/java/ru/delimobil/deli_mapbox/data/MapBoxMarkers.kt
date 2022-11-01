package ru.delimobil.deli_mapbox.data

import android.graphics.Bitmap

data class MapBoxMarkers(
    val groupId: String,
    val options: List<Options>
) {

    data class Options(
        val id: String,
        val visible: Boolean = true,
        val position: MapBoxCoordinates,
        val image: Icon?,
        val verticalOffset: Float = 0f,
        val rotation: Float? = null,
        val alpha: Float? = null
    ) {

        data class Icon(
            val tag: String,
            val bitmap: Bitmap?
        )
    }
}