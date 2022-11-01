package ru.delimobil.deli_mapbox.extensions

import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style

fun MapboxMap.onStyle(onReady: Style.() -> Unit) {
    getStyle { onReady.invoke(it) }
}