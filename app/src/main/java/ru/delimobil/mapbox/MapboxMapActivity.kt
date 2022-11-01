package ru.delimobil.mapbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import ru.delimobil.deli_mapbox.data.*
import ru.delimobil.deli_mapbox.data.options.*
import ru.delimobil.deli_mapbox.data.polygon.*
import ru.delimobil.mapbox.databinding.ActivityMapboxMapBinding

class MapboxMapActivity : AppCompatActivity(R.layout.activity_mapbox_map) {

    private val viewBinding by viewBinding(ActivityMapboxMapBinding::bind)

    private val sources by lazy {
        listOf(
            "source_polygons"
        )
    }

    private val layers by lazy {
        listOf(
            MapBoxLayer(
                layerId = "layer_polygons",
                sourceId = "source_polygons",
                type = MapBoxLayer.Type.Fill
            ),
            MapBoxLayer(
                layerId = "layer_polygons_stroke",
                sourceId = "source_polygons",
                type = MapBoxLayer.Type.Line
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding.vMap.onCreate(savedInstanceState)

        viewBinding.vMap.initialize(
            styleUrl = "mapbox://styles/delimobil-carsharing/ck7bzz4bz00vj1ipax8pz3q3s",
            sourceIdList = sources,
            layerList = layers,
            onMapReady = { mapBoxMapHelper ->
                viewBinding.vMapLocationToggle.setOnClickListener {
                    mapBoxMapHelper.isMyLocationEnabled = !mapBoxMapHelper.isMyLocationEnabled
                }

                mapBoxMapHelper.setLocationButtonStyle(
                    elevation = 0f,
                    accuracyColorResId = R.color.colorAccent,
                    backgroundTintColorResId = 0,
                    foregroundTintColorResId = 0,
                    bearingDrawableResId = R.drawable.ic_checkmark_24_base,
                    layerBelowName = "layer_polygons_stroke"
                )

                mapBoxMapHelper.moveCamera(
                    coordinates = listOf(MapBoxCoordinates(55.758684, 37.619928)),
                    cameraOptions = MapBoxCameraOptions(
                        animate = false,
                        zoom = 10.5
                    )
                )

                mapBoxMapHelper.addPolygons(
                    sourceId = "source_polygons",
                    fillLayerId = "layer_polygons",
                    strokeLayerId = "layer_polygons_stroke",
                    data = listOf(
                        MapBoxPolygonData(
                            outlines = listOf(
                                MapBoxCoordinates(55.760809, 37.582334),
                                MapBoxCoordinates(55.765475, 37.596472),
                                MapBoxCoordinates(55.757629, 37.607611),
                                MapBoxCoordinates(55.750105, 37.602172),
                                MapBoxCoordinates(55.760809, 37.582334)
                            ),
                            holes = emptyList(),
                            fill = MapBoxFillOptions(
                                color = ContextCompat.getColor(this, R.color.azure)
                            ),
                            stroke = MapBoxStrokeOptions(
                                width = 4f,
                                color = ContextCompat.getColor(this, R.color.black)
                            )
                        ),
                        MapBoxPolygonData(
                            outlines = listOf(
                                MapBoxCoordinates(55.791736, 37.705464),
                                MapBoxCoordinates(55.791760, 37.707438),
                                MapBoxCoordinates(55.789637, 37.706687),
                                MapBoxCoordinates(55.790355, 37.702889),
                                MapBoxCoordinates(55.791736, 37.705464)
                            ),
                            holes = emptyList(),
                            fill = MapBoxFillOptions(
                                color = ContextCompat.getColor(this, R.color.candy_brick)
                            ),
                            stroke = null
                        )
                    ),
                    strokePattern = MapBoxStrokeOptions.Pattern(
                        dash = 1.5f,
                        gap = 0.75f
                    )
                )
            }
        )
    }

    override fun onStart() {
        super.onStart()
        viewBinding.vMap.onStart()
    }

    override fun onStop() {
        super.onStop()
        viewBinding.vMap.onStop()
    }

    override fun onPause() {
        super.onPause()
        viewBinding.vMap.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewBinding.vMap.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewBinding.vMap.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        viewBinding.vMap.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewBinding.vMap.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }
}