package com.makitaxi.driver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.makitaxi.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapDriver {
    private static final double DEFAULT_ZOOM = 16;
    private static final double MIN_ZOOM = 3;
    private static final double MAX_ZOOM = 21;
    private static final GeoPoint BELGRADE_CENTER = new GeoPoint(44.7866, 20.4489);

    private Context context;
    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;

    public MapDriver(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        setupMapView();
    }

    private void setupMapView() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        mapController = mapView.getController();
        mapController.setZoom(DEFAULT_ZOOM);
        mapController.setCenter(BELGRADE_CENTER);

        setupLocationOverlay();
    }

    private void setupLocationOverlay() {
        // Create and configure the location overlay
        GpsMyLocationProvider locationProvider = new GpsMyLocationProvider(context);
        locationProvider.setLocationUpdateMinDistance(10); // Update if moved by 10 meters
        locationProvider.setLocationUpdateMinTime(2000); // Update every 2 seconds

        myLocationOverlay = new MyLocationNewOverlay(locationProvider, mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        
        // Customize the location indicator
        Drawable locationIcon = ContextCompat.getDrawable(context, R.drawable.ic_my_location);
        if (locationIcon != null) {
            Bitmap locationBitmap = drawableToBitmap(locationIcon);
            myLocationOverlay.setDirectionArrow(locationBitmap, locationBitmap);
            myLocationOverlay.setPersonIcon(locationBitmap);
        }

        mapView.getOverlays().add(myLocationOverlay);
        
        // Add scale bar
        org.osmdroid.views.overlay.ScaleBarOverlay scaleBarOverlay = new org.osmdroid.views.overlay.ScaleBarOverlay(mapView);
        scaleBarOverlay.setCentred(true);
        scaleBarOverlay.setScaleBarOffset(context.getResources().getDisplayMetrics().widthPixels / 2, 10);
        mapView.getOverlays().add(scaleBarOverlay);

        // Add compass
        org.osmdroid.views.overlay.compass.CompassOverlay compassOverlay = new org.osmdroid.views.overlay.compass.CompassOverlay(
            context, mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 48; // Default size if intrinsic width not available
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 48; // Default size if intrinsic height not available

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public void zoomIn() {
        mapController.zoomIn();
    }

    public void zoomOut() {
        mapController.zoomOut();
    }

    public void centerOnCurrentLocation() {
        GeoPoint myLocation = myLocationOverlay.getMyLocation();
        if (myLocation != null) {
            mapController.animateTo(myLocation);
            mapController.setZoom(DEFAULT_ZOOM);
        }
    }

    public GeoPoint getCurrentLocation() {
        return myLocationOverlay.getMyLocation();
    }

    public void onResume() {
        mapView.onResume();
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        
        // Clear old tiles and reload
        mapView.getTileProvider().clearTileCache();
        mapView.invalidate();
    }

    public void onPause() {
        mapView.onPause();
        myLocationOverlay.disableMyLocation();
        myLocationOverlay.disableFollowLocation();
    }

    public void onDestroy() {
        if (mapView != null) {
            mapView.onDetach();
            mapView.getTileProvider().clearTileCache();
        }
    }
}
