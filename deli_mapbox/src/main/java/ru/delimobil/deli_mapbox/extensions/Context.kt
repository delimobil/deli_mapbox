package ru.delimobil.deli_mapbox.extensions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun Context.hasLocationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        applicationContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else true
}