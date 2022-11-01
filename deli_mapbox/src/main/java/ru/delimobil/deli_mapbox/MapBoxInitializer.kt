package ru.delimobil.deli_mapbox

import android.content.Context
import com.mapbox.mapboxsdk.Mapbox

object MapBoxInitializer {

    fun init(appContext: Context, mapBoxToken: String) {
        Mapbox.getInstance(appContext, mapBoxToken)
    }
}