package ru.delimobil.deli_mapbox

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import androidx.core.content.ContextCompat
import com.google.gson.JsonArray
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.*
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.ColorUtils
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfTransformation
import kotlinx.coroutines.*
import ru.delimobil.deli_mapbox.data.*
import ru.delimobil.deli_mapbox.data.MapBoxProperty.PROPERTY_MARKER_ALPHA
import ru.delimobil.deli_mapbox.data.MapBoxProperty.PROPERTY_MARKER_ICON
import ru.delimobil.deli_mapbox.data.MapBoxProperty.PROPERTY_MARKER_ID
import ru.delimobil.deli_mapbox.data.MapBoxProperty.PROPERTY_MARKER_ROTATION
import ru.delimobil.deli_mapbox.data.MapBoxProperty.PROPERTY_MARKER_VERTICAL_OFFSET
import ru.delimobil.deli_mapbox.data.MapBoxProperty.PROPERTY_POLYGON_FILL_COLOR
import ru.delimobil.deli_mapbox.data.MapBoxProperty.PROPERTY_POLYGON_STROKE_COLOR
import ru.delimobil.deli_mapbox.data.MapBoxProperty.PROPERTY_POLYGON_STROKE_WIDTH
import ru.delimobil.deli_mapbox.data.options.MapBoxCameraOptions
import ru.delimobil.deli_mapbox.data.options.MapBoxFillOptions
import ru.delimobil.deli_mapbox.data.options.MapBoxStrokeOptions
import ru.delimobil.deli_mapbox.data.polygon.MapBoxPolygonData
import ru.delimobil.deli_mapbox.extensions.*
import timber.log.Timber

class MapBoxMapHelper(
    private val map: MapboxMap,
    private val mapView: MapView,
    private val sourceIdList: List<String>,
    private val layerList: List<MapBoxLayer>
) {

    private var innerIdleListener: () -> Unit = {}
    private var innerMovedListener: () -> Unit = {}
    private var innerMoveStartListener: () -> Unit = {}
    private var innerMarkerClickListener: (MapBoxClickedMarker) -> Unit = {}
    private var innerMapClickListener: (MapBoxCoordinates) -> Unit = {}
    private var innerMapLongClickListener: (MapBoxCoordinates) -> Unit = {}

    private val locationComponent: LocationComponent
        get() = map.locationComponent

    private val markerJobs = mutableMapOf<String, Job>()
    private val polylineJobs = mutableMapOf<String, Job>()
    private val polygonJob = mutableMapOf<String, Job>()
    private val circleJob = mutableMapOf<String, Job>()
    private val mapPaddings = mutableListOf(0.0, 0.0, 0.0, 0.0)

    private val clickableLayerIdList = mutableListOf<String>()
    private var locationComponentOptions: LocationComponentOptions? = null

    init {
        map.run {
            addOnCameraIdleListener { innerIdleListener.invoke() }
            addOnCameraMoveListener { innerMovedListener.invoke() }
            addOnCameraMoveStartedListener { reason ->
                if (reason == MapboxMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                    innerMoveStartListener.invoke()
                }
            }
            addOnMapClickListener {
                val symbolClicked = handleSymbolClick(projection.toScreenLocation(it))
                if (!symbolClicked) {
                    innerMapClickListener.invoke(it.toMapBoxCoordinates())
                }
                return@addOnMapClickListener false
            }
            addOnMapLongClickListener {
                innerMapLongClickListener.invoke(it.toMapBoxCoordinates())
                return@addOnMapLongClickListener false
            }

            onStyle {
                sourceIdList.forEach { addSource(GeoJsonSource(it)) }
                layerList.forEach {
                    val layer = it.toLayer()
                    if (it.isClickable) {
                        clickableLayerIdList.add(it.layerId)
                    }
                    when (val position = it.position) {
                        is MapBoxLayer.Position.Above -> addLayerAbove(layer, position.layerId)
                        is MapBoxLayer.Position.Below -> addLayerBelow(layer, position.layerId)
                        is MapBoxLayer.Position.Default -> addLayer(layer)
                    }
                }
            }
        }
    }

    var isMyLocationEnabled: Boolean
        get() = locationComponent.isLocationComponentEnabled
        @SuppressLint("MissingPermission")
        set(value) {
            activateLocationComponentIfNeeded()
            onLocationAllowed {
                map.onStyle {
                    locationComponent.isLocationComponentEnabled = value
                }
            }
        }

    var maxZoom: Double
        get() = map.maxZoomLevel
        set(value) {
            map.setMaxZoomPreference(value)
        }

    val currentZoom: Double
        get() = map.cameraPosition.zoom

    val currentTarget: MapBoxCoordinates
        get() = map.cameraPosition.target.toMapBoxCoordinates()

    val currentVisibleRegion: MapBoxRegion
        get() {
            val mapViewRect = Rect()
            mapView.getGlobalVisibleRect(mapViewRect)

            val northEast = map.projection.fromScreenLocation(
                PointF(
                    mapViewRect.right.toFloat() - mapPaddings[2].toFloat(),
                    mapViewRect.top.toFloat() + mapPaddings[1].toFloat()
                )
            )

            val southWest = map.projection.fromScreenLocation(
                PointF(
                    mapViewRect.left.toFloat() + mapPaddings[0].toFloat(),
                    mapViewRect.bottom.toFloat() - mapPaddings[3].toFloat()
                )
            )

            val visibleBounds = LatLngBounds.Builder()
                .includes(listOf(northEast, southWest))
                .build()

            return MapBoxRegion(
                southWest.toMapBoxCoordinates(),
                northEast.toMapBoxCoordinates(),
                visibleBounds.center.toMapBoxCoordinates()
            )
        }

    fun setOnCameraIdleListener(listener: () -> Unit) {
        innerIdleListener = listener
    }

    fun setOnCameraMovedListener(listener: () -> Unit) {
        innerMovedListener = listener
    }

    fun setOnCameraMoveStartedListener(listener: () -> Unit) {
        innerMoveStartListener = listener
    }

    fun setOnMarkerClickListener(listener: (MapBoxClickedMarker) -> Unit) {
        innerMarkerClickListener = listener
    }

    fun setOnMapClickListener(listener: (MapBoxCoordinates) -> Unit) {
        innerMapClickListener = listener
    }

    fun setOnMapLongClickListener(listener: (MapBoxCoordinates) -> Unit) {
        innerMapLongClickListener = listener
    }

    fun setMapInteractionsEnabled(enabled: Boolean) {
        map.uiSettings.run {
            setAllGesturesEnabled(enabled)
            if (enabled) {
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isZoomGesturesEnabled = true
            }
        }
    }

    fun setMapUiSetting(
        animationEnabled: Boolean,
        interactionEnabled: Boolean,
        attributionEnabled: Boolean,
        logoEnabled: Boolean,
        compassEnabled: Boolean
    ) {
        map.uiSettings.run {
            setAllVelocityAnimationsEnabled(animationEnabled)
            setMapInteractionsEnabled(interactionEnabled)
            isAttributionEnabled = attributionEnabled
            isLogoEnabled = logoEnabled
            isCompassEnabled = compassEnabled
        }
    }

    fun setLocationButtonStyle(
        elevation: Float,
        accuracyColorResId: Int,
        backgroundTintColorResId: Int,
        foregroundTintColorResId: Int,
        bearingDrawableResId: Int,
        layerBelowName: String
    ) {
        locationComponentOptions = LocationComponentOptions
            .builder(mapView.context)
            .elevation(elevation)
            .accuracyColor(ContextCompat.getColor(mapView.context, accuracyColorResId))
            .backgroundTintColor(backgroundTintColorResId)
            .foregroundTintColor(foregroundTintColorResId)
            .bearingDrawable(bearingDrawableResId)
            .layerBelow(layerBelowName)
            .build()

        activateLocationComponentIfNeeded()
    }

    private fun activateLocationComponentIfNeeded() {
        if (!locationComponent.isLocationComponentActivated) {
            onLocationAllowed {
                locationComponentOptions?.let { options ->
                    map.onStyle {
                        val activateOptions = LocationComponentActivationOptions
                            .builder(mapView.context, this)
                            .locationComponentOptions(options)
                            .useDefaultLocationEngine(true)
                            .build()

                        locationComponent.run {
                            activateLocationComponent(activateOptions)
                            renderMode = RenderMode.COMPASS
                            cameraMode = CameraMode.NONE
                        }
                    }
                } ?: Timber.e("You must call setLocationButtonStyle before!")
            }
        }
    }

    fun addPolyline(
        polyline: MapBoxPolyline,
        sourceId: String,
        layerId: String
    ) {
        polylineJobs[polyline.groupId]?.cancel()
        polylineJobs[polyline.groupId] = CoroutineScope(Dispatchers.IO).launch {
            val lineGeometry = polyline.options.points.map { point ->
                Point.fromLngLat(point.lon, point.lat)
            }.let(LineString::fromLngLats)

            withContext(Dispatchers.Main) {
                map.onStyle {
                    getSourceAs<GeoJsonSource>(sourceId)
                        ?.setGeoJson(Feature.fromGeometry(lineGeometry))

                    getLayer(layerId)?.setProperties(
                        lineColor(ColorUtils.colorToRgbaString(polyline.options.color)),
                        lineWidth(polyline.options.width),
                        lineCap(Property.LINE_CAP_ROUND),
                        lineJoin(Property.LINE_JOIN_ROUND)
                    )
                }
            }
        }
    }

    fun removePolyline(sourceId: String) {
        map.onStyle {
            getSourceAs<GeoJsonSource>(sourceId)
                ?.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(emptyList())))
        }
    }

    fun addMarkers(
        markers: MapBoxMarkers,
        sourceId: String,
        layerId: String
    ) {
        markerJobs[markers.groupId]?.cancel()
        markerJobs[markers.groupId] = CoroutineScope(Dispatchers.IO).launch {
            val icons = mutableSetOf<MapBoxMarkers.Options.Icon>()
            val markersFeatures = markers.options.filter {
                it.visible
            }.map {
                val pos = it.position
                val p = Point.fromLngLat(pos.lon, pos.lat)
                it.image?.let(icons::add)
                Feature.fromGeometry(p).apply {
                    addStringProperty(PROPERTY_MARKER_ID, it.id)
                    addStringProperty(PROPERTY_MARKER_ICON, it.image?.tag)
                    addNumberProperty(PROPERTY_MARKER_ROTATION, it.rotation ?: 0f)
                    addNumberProperty(PROPERTY_MARKER_ALPHA, it.alpha)
                    addProperty(PROPERTY_MARKER_VERTICAL_OFFSET, JsonArray(2)
                        .apply {
                            add(0f)
                            add(it.verticalOffset)
                        }
                    )

                }
            }

            withContext(Dispatchers.Main) {
                map.onStyle {
                    icons.forEach {
                        fetchIcon(it)
                    }

                    getSourceAs<GeoJsonSource>(sourceId)
                        ?.setGeoJson(FeatureCollection.fromFeatures(markersFeatures))

                    getLayer(layerId)?.setProperties(
                        iconImage(Expression.get(PROPERTY_MARKER_ICON)),
                        iconOpacity(Expression.get(PROPERTY_MARKER_ALPHA)),
                        iconRotate(Expression.get(PROPERTY_MARKER_ROTATION)),
                        iconOffset(Expression.get(PROPERTY_MARKER_VERTICAL_OFFSET)),
                        iconIgnorePlacement(true),
                        iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT)
                    )
                }
            }
        }
    }

    fun removeMarkers(sourceId: String) = removeSource(sourceId)

    fun addPolygons(
        sourceId: String,
        fillLayerId: String,
        strokeLayerId: String,
        data: List<MapBoxPolygonData>,
        strokePattern: MapBoxStrokeOptions.Pattern? = null // MapBox does not support dynamic line-dash-array https://docs.mapbox.com/mapbox-gl-js/style-spec/layers/#paint-line-line-dasharray
    ) {
        polygonJob[sourceId]?.cancel()
        polygonJob[sourceId] = CoroutineScope(Dispatchers.IO).launch {
            val dataCollection: List<Feature> = data.mapNotNull { polygon ->
                polygon.outlines.mapToLine()?.let { polygonLine ->
                    Feature.fromGeometry(
                        Polygon.fromOuterInner(
                            polygonLine,
                            polygon.holes.mapNotNull { hole -> hole.mapToLine() }
                        )
                    ).apply {
                        addStringProperty(
                            PROPERTY_POLYGON_FILL_COLOR,
                            ColorUtils.colorToRgbaString(polygon.fill?.color ?: Color.TRANSPARENT)
                        )

                        addStringProperty(
                            PROPERTY_POLYGON_STROKE_COLOR,
                            ColorUtils.colorToRgbaString(polygon.stroke?.color ?: Color.TRANSPARENT)
                        )

                        addNumberProperty(
                            PROPERTY_POLYGON_STROKE_WIDTH,
                            polygon.stroke?.width ?: 0
                        )
                    }
                }
            }

            withContext(Dispatchers.Main) {
                map.onStyle {
                    getSourceAs<GeoJsonSource>(sourceId)
                        ?.setGeoJson(FeatureCollection.fromFeatures(dataCollection))

                    val fillLayer = getLayer(fillLayerId)
                    fillLayer?.setProperties(
                        fillColor(Expression.get(PROPERTY_POLYGON_FILL_COLOR)),
                        fillAntialias(true)
                    )

                    getLayer(strokeLayerId)?.run {
                        strokePattern?.let { pattern ->
                            setProperties(
                                lineDasharray(
                                    arrayOf(pattern.dash, pattern.gap)
                                )
                            )
                        }

                        setProperties(
                            lineColor(Expression.get(PROPERTY_POLYGON_STROKE_COLOR)),
                            lineWidth(Expression.get(PROPERTY_POLYGON_STROKE_WIDTH)),
                            lineJoin(Property.LINE_JOIN_ROUND)
                        )
                    }
                }
            }
        }
    }

    fun addPolygon(
        polygon: MapBoxPolygon,
        sourceId: String,
        fillLayerId: String,
        strokeLayerId: String
    ) {
        polygonJob[polygon.groupId]?.cancel()
        polygonJob[polygon.groupId] = CoroutineScope(Dispatchers.IO).launch {
            when (val data = polygon.options.data) {
                is MapBoxPolygon.Options.DataSource.Coordinates -> {
                    val polygonsData: MutableList<Polygon> = mutableListOf()
                    data.lines.forEach { opt ->
                        val polyOutlines = opt.outlines.mapToLine()

                        val polyHoles = opt.holes.mapNotNull { it.mapToLine() }

                        polyOutlines?.let { outlines ->
                            polygonsData.add(
                                Polygon.fromOuterInner(
                                    outlines,
                                    polyHoles
                                )
                            )
                        }
                    }

                    withContext(Dispatchers.Main) {
                        drawPolygon(
                            sourceId = sourceId,
                            geometry = MultiPolygon.fromPolygons(polygonsData),
                            fillLayerId = fillLayerId,
                            strokeLayerId = strokeLayerId,
                            fillOptions = polygon.options.fillOptions,
                            strokeOptions = polygon.options.strokeOptions
                        )
                    }
                }

                is MapBoxPolygon.Options.DataSource.GeoJson -> {
                    val featureCollection = FeatureCollection.fromJson(data.json)
                    featureCollection?.let {
                        it.features()?.forEach { feature ->
                            feature.geometry()?.let { geometry ->
                                when (geometry) {
                                    is Polygon,
                                    is MultiPolygon -> withContext(Dispatchers.Main) {
                                        drawPolygon(
                                            sourceId = sourceId,
                                            geometry = geometry,
                                            fillLayerId = fillLayerId,
                                            strokeLayerId = strokeLayerId,
                                            fillOptions = polygon.options.fillOptions,
                                            strokeOptions = polygon.options.strokeOptions
                                        )
                                    }
                                    else -> {
                                        // do nothing
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun removePolygon(sourceId: String) = removeSource(sourceId)

    fun addCircles(
        circles: MapBoxCircles,
        sourceId: String,
        fillLayerId: String,
        strokeLayerId: String
    ) {
        circleJob[circles.groupId]?.cancel()
        circleJob[circles.groupId] = CoroutineScope(Dispatchers.IO).launch {
            val circlesFeatures = circles.options.map {
                val centerPoint = Point.fromLngLat(it.center.lon, it.center.lat)
                it to TurfTransformation.circle(
                    centerPoint,
                    it.radius.toDouble(),
                    TurfConstants.UNIT_METERS
                )
            }.map { (options, polygon) ->
                Feature.fromGeometry(polygon).apply {
                    addStringProperty(
                        PROPERTY_POLYGON_FILL_COLOR,
                        ColorUtils.colorToRgbaString(options.fill.color)
                    )
                    options.stroke?.let { stroke ->
                        addStringProperty(
                            PROPERTY_POLYGON_STROKE_COLOR,
                            ColorUtils.colorToRgbaString(stroke.color)
                        )
                        addNumberProperty(PROPERTY_POLYGON_STROKE_WIDTH, stroke.width)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                map.onStyle {
                    getSourceAs<GeoJsonSource>(sourceId)
                        ?.setGeoJson(FeatureCollection.fromFeatures(circlesFeatures))

                    getLayer(fillLayerId)?.setProperties(
                        fillAntialias(true),
                        fillColor(Expression.get(PROPERTY_POLYGON_FILL_COLOR))
                    )

                    getLayer(strokeLayerId)
                        ?.apply {
                            circles.linePattern?.let { pattern ->
                                setProperties(
                                    lineDasharray(
                                        arrayOf(pattern.dash, pattern.gap)
                                    )
                                )
                            }
                        }
                        ?.setProperties(
                            lineColor(Expression.get(PROPERTY_POLYGON_STROKE_COLOR)),
                            lineWidth(Expression.get(PROPERTY_POLYGON_STROKE_WIDTH)),
                            lineJoin(Property.LINE_JOIN_ROUND),
                            lineCap(Property.LINE_CAP_ROUND)
                        )
                }
            }
        }
    }

    fun removeCircles(sourceId: String) = removeSource(sourceId)

    fun doesProjectionContains(coordinates: MapBoxCoordinates): Boolean {
        return map.projection.visibleRegion.latLngBounds.contains(
            coordinates.toLatLng()
        )
    }

    fun switchToNavigationMode(zoom: Double, tilt: Double) {
        locationComponent.setCameraMode(
            CameraMode.TRACKING_COMPASS,
            150,
            zoom,
            null,
            tilt,
            null
        )
    }

    fun moveCamera(coordinates: List<MapBoxCoordinates>, cameraOptions: MapBoxCameraOptions) {
        if (coordinates.isEmpty()) return

        val padding = cameraOptions.padding
        val factory = when (coordinates.size) {
            1 -> CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(coordinates.first().toLatLng())
                .apply {
                    cameraOptions.zoom?.let { zoom(it) }
                    cameraOptions.bearing?.let { bearing(it) }
                    cameraOptions.tilt?.let { tilt(it) }
                }
                .padding(
                    mapPaddings[0] + padding,
                    mapPaddings[1] + padding,
                    mapPaddings[2] + padding,
                    mapPaddings[3] + padding
                )
                .build())
            else -> CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.Builder()
                    .includes(coordinates.map { it.toLatLng() })
                    .build(),
                cameraOptions.bearing ?: 0.0,
                cameraOptions.tilt ?: 0.0,
                mapPaddings[0].toInt() + padding,
                mapPaddings[1].toInt() + padding,
                mapPaddings[2].toInt() + padding,
                mapPaddings[3].toInt() + padding
            )
        }

        if (cameraOptions.animate) map.animateCamera(factory)
        else map.moveCamera(factory)
    }

    fun zoomOut() {
        map.animateCamera(CameraUpdateFactory.zoomOut())
    }

    fun zoomIn() {
        map.animateCamera(CameraUpdateFactory.zoomIn())
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        mapPaddings[0] = left.toDouble()
        mapPaddings[1] = top.toDouble()
        mapPaddings[2] = right.toDouble()
        mapPaddings[3] = bottom.toDouble()
    }

    fun getCoordinatesByXY(x: Int, y: Int): MapBoxCoordinates {
        return map.projection.fromScreenLocation(PointF(x.toFloat(), y.toFloat()))
            .toMapBoxCoordinates()
    }

    private fun drawPolygon(
        sourceId: String,
        geometry: Geometry,
        fillLayerId: String,
        strokeLayerId: String,
        fillOptions: MapBoxFillOptions?,
        strokeOptions: MapBoxStrokeOptions?
    ) {
        map.onStyle {
            val source = getSourceAs<GeoJsonSource>(sourceId)
            Timber.d("drawPolygon source $source")
            source?.setGeoJson(geometry)

            fillOptions?.let { fill ->
                val layer = getLayer(fillLayerId)
                Timber.d("drawPolygon fillLayer $layer")
                layer?.setProperties(
                    fillColor(ColorUtils.colorToRgbaString(fill.color)),
                    fillAntialias(true)
                )
            }

            strokeOptions?.let { stroke ->
                val layer = getLayer(strokeLayerId)
                Timber.d("drawPolygon strokeLayer $layer")
                layer
                    ?.apply {
                        stroke.pattern?.let { strokePattern ->
                            setProperties(
                                lineDasharray(
                                    arrayOf(strokePattern.dash, strokePattern.gap)
                                )
                            )
                        }
                    }
                    ?.setProperties(
                        lineColor(ColorUtils.colorToRgbaString(stroke.color)),
                        lineWidth(stroke.width),
                        lineJoin(Property.LINE_JOIN_ROUND)
                    )
            }
        }
    }

    /**
     * @return true if symbol was clicked, false otherwise
     */
    private fun handleSymbolClick(pointF: PointF): Boolean {
        clickableLayerIdList.forEach { layerId ->
            map.queryRenderedFeatures(pointF, layerId)
                .firstOrNull()
                ?.getStringProperty(PROPERTY_MARKER_ID)
                ?.let { markerId ->
                    innerMarkerClickListener.invoke(
                        MapBoxClickedMarker(
                            markerId = markerId,
                            layerId = layerId
                        )
                    )
                    return true
                }
        }
        return false
    }

    private fun removeSource(sourceId: String) {
        map.onStyle {
            getSourceAs<GeoJsonSource>(sourceId)
                ?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }
    }

    private fun onLocationAllowed(allow: () -> Unit) {
        if (mapView.context.hasLocationPermission()) {
            allow.invoke()
        }
    }

    private fun List<MapBoxCoordinates>.mapToLine(): LineString? {
        return takeIf { outlines -> outlines.firstOrNull() == outlines.lastOrNull() && outlines.size >= 4 }
            ?.map { outlines ->
                Point.fromLngLat(outlines.lon, outlines.lat)
            }
            ?.let { points ->
                LineString.fromLngLats(points)
            }
    }
}