package ru.delimobil.deli_mapbox.data

data class MapBoxLayer(
    val layerId: String,
    val sourceId: String,
    val type: Type,
    val position: Position = Position.Default,
    val isClickable: Boolean = false
) {

    sealed class Type {
        object Fill : Type()
        object Line : Type()
        object Symbol : Type()
    }

    sealed class Position {
        data class Below(val layerId: String) : Position()
        data class Above(val layerId: String) : Position()
        object Default : Position()
    }

}