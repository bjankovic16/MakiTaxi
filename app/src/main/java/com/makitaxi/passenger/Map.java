package com.makitaxi.passenger;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Map {

    public interface CallbackMapTap {
        public void onTap(GeoPoint p);
    }

    private CallbackMapTap callbackMapTap;
    private Context context;
    private MapView mapView;

    private ExecutorService executorService;

    private Handler mainHandler;

    private IMapController mapController;

    private MyLocationNewOverlay myLocationOverlay;

    private MapEventsOverlay mapEventsOverlay;

    public Map(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        // thread for executing
        this.executorService = Executors.newSingleThreadExecutor();
        // results are propagated to main thread here
        this.mainHandler = new Handler(Looper.getMainLooper());
        setupMapView();
    }

    private void setupMapView() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        mapController = mapView.getController();
        mapController.setZoom(15.0);
        GeoPoint belgradeCenter = new GeoPoint(44.7866, 20.4489);
        mapController.setCenter(belgradeCenter);

        setupLocationOverlay();
        setupMapEvents();
    }

    public void initCallbackMapTap(Map.CallbackMapTap callbackMapTap) {
        this.callbackMapTap = callbackMapTap;
    }

    private void setupMapEvents() {
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if(callbackMapTap != null) {
                    callbackMapTap.onTap(p);
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                if(callbackMapTap != null) {
                    callbackMapTap.onTap(p);
                }
                return true;
            }
        };

        mapEventsOverlay = new MapEventsOverlay(mReceive);
        mapView.getOverlays().add(mapEventsOverlay);
    }

    private void setupLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        mapView.getOverlays().add(myLocationOverlay);
    }

    public void zoomIn() {
        mapController.zoomIn();
    }

    public void zoomOut() {
        mapController.zoomOut();
    }

    public void centerOnCurrentLocation() {
        mapController.animateTo(myLocationOverlay.getMyLocation());
    }

    public GeoPoint getCurrentLocation() {
        return myLocationOverlay.getMyLocation();
    }

    public GeoPoint getPressedLocation() {
        return null;
    }

}
