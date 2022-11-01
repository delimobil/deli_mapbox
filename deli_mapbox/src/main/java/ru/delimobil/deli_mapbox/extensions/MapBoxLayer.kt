package ru.delimobil.deli_mapbox.extensions

import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import ru.delimobil.deli_mapbox.data.MapBoxLayer

fun MapBoxLayer.toLayer(): Layer {
    return when (type) {
        MapBoxLayer.Type.Fill -> FillLayer(layerId, sourceId)
        MapBoxLayer.Type.Line -> LineLayer(layerId, sourceId)
        MapBoxLayer.Type.Symbol -> SymbolLayer(layerId, sourceId)
    }
}