package ru.delimobil.deli_mapbox

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import by.kirich1409.viewbindingdelegate.viewBinding
import com.mapbox.mapboxsdk.maps.Style
import ru.delimobil.deli_mapbox.data.MapBoxLayer
import ru.delimobil.deli_mapbox.databinding.ViewMapMapboxBinding

class MapBoxMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val viewBinding by viewBinding(
        viewBindingRootId = R.id.vMapBoxMap,
        lifecycleAware = false,
        vbFactory = ViewMapMapboxBinding::bind
    )

    init {
        View.inflate(context, R.layout.view_map_mapbox, this)
    }

    fun initialize(
        styleUrl: String,
        sourceIdList: List<String>,
        layerList: List<MapBoxLayer>,
        onMapReady: (MapBoxMapHelper) -> Unit
    ) {
        viewBinding.vMapBoxMap.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(styleUrl)) {
                onMapReady.invoke(
                    MapBoxMapHelper(
                        map = map,
                        mapView = viewBinding.vMapBoxMap,
                        sourceIdList = sourceIdList,
                        layerList = layerList
                    )
                )
            }
        }
    }

    fun onCreate(bundle: Bundle?) {
        viewBinding.vMapBoxMap.onCreate(bundle)
    }

    fun onStart() {
        viewBinding.vMapBoxMap.onStart()
    }

    fun onResume() {
        viewBinding.vMapBoxMap.onResume()
    }

    fun onPause() {
        viewBinding.vMapBoxMap.onPause()
    }

    fun onStop() {
        viewBinding.vMapBoxMap.onStop()
    }

    fun onDestroy() {
        viewBinding.vMapBoxMap.onDestroy()
    }

    fun onLowMemory() {
        viewBinding.vMapBoxMap.onLowMemory()
    }

    fun onSaveInstanceState(bundle: Bundle?) {
        bundle?.let(viewBinding.vMapBoxMap::onSaveInstanceState)
    }
}