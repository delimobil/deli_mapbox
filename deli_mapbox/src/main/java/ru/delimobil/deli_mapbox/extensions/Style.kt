package ru.delimobil.deli_mapbox.extensions

import com.mapbox.mapboxsdk.maps.Style
import ru.delimobil.deli_mapbox.data.MapBoxMarkers

fun Style.fetchIcon(icon: MapBoxMarkers.Options.Icon) {
    if (getImage(icon.tag) == null && icon.bitmap != null) {
        addImage(icon.tag, icon.bitmap)
    }
}