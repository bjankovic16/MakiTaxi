package com.makitaxi.passenger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

public class Map {

    public interface RoutingCallback {
        void onRouteFound(List<GeoPoint> routePoints, double distance, double duration);

        void onRoutingError(String error);
    }

    public interface CallbackMapTap {
        public void onTap(GeoPoint p);
    }

    private static final String OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving/";

    private CallbackMapTap callbackMapTap;
    private Context context;
    private MapView mapView;

    private Marker startMarker;

    private Marker destinationMarker;

    private Marker mapTapMarker;

    private ExecutorService executorService;

    private Handler mainHandler;

    private IMapController mapController;

    private MyLocationNewOverlay myLocationOverlay;

    private MapEventsOverlay mapEventsOverlay;

    private Polyline routePolyline;

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

    public void drawRouteBetweenPoints(GeoPoint startPoint, GeoPoint endPoint, RoutingCallback externalCallback) {
        clearMarkers();
        clearRoute();
        getRouteFromOSRM(startPoint, endPoint, new Map.RoutingCallback() {
            @Override
            public void onRouteFound(List<GeoPoint> routePoints, double distance, double duration) {
                mainHandler.post(() -> {
                    displayRealRoute(routePoints);
                    externalCallback.onRouteFound(routePoints, distance, duration);
                });
            }

            @Override
            public void onRoutingError(String error) {
            }
        });
    }

    private void getRouteFromOSRM(GeoPoint start, GeoPoint end, RoutingCallback callback) {
        if(start == null || end == null) {
            return;
        }
        executorService.execute(() -> {
            try {
                String urlString = OSRM_BASE_URL +
                        start.getLongitude() + "," + start.getLatitude() + ";" +
                        end.getLongitude() + "," + end.getLatitude() +
                        "?overview=full&geometries=geojson&steps=true";

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("User-Agent", "MakiTaxi/1.0");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    parseOSRMResponse(response.toString(), callback);

                } else {
                    callback.onRoutingError("HTTP error: " + responseCode);
                }

                connection.disconnect();

            } catch (Exception e) {
                callback.onRoutingError("Network error: " + e.getMessage());
            }
        });
    }

    private void parseOSRMResponse(String jsonResponse, RoutingCallback callback) {
        try {
            JSONObject response = new JSONObject(jsonResponse);
            String code = response.getString("code");

            if (!"Ok".equals(code)) {
                callback.onRoutingError("OSRM returned: " + code);
                return;
            }

            JSONArray routes = response.getJSONArray("routes");
            if (routes.length() == 0) {
                callback.onRoutingError("No routes found");
                return;
            }

            JSONObject route = routes.getJSONObject(0);
            JSONObject geometry = route.getJSONObject("geometry");
            JSONArray coordinates = geometry.getJSONArray("coordinates");

            double distance = route.getDouble("distance") / 1000.0;
            double duration = route.getDouble("duration") / 60.0;

            List<GeoPoint> routePoints = new ArrayList<>();
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray coord = coordinates.getJSONArray(i);
                double lon = coord.getDouble(0);
                double lat = coord.getDouble(1);
                routePoints.add(new GeoPoint(lat, lon));
            }

            callback.onRouteFound(routePoints, distance, duration);

        } catch (JSONException e) {
            callback.onRoutingError("Failed to parse route data");
        }
    }


    private void displayRealRoute(List<GeoPoint> routePoints) {
        if (routePoints == null || routePoints.isEmpty()) {
            return;
        }

        routePolyline = new Polyline();
        routePolyline.setPoints(routePoints);
        routePolyline.setColor(Color.parseColor("#343B71"));
        routePolyline.setWidth(10.0f);
        routePolyline.setGeodesic(false);

        mapView.getOverlays().add(routePolyline);
        mapView.getOverlays().remove(startMarker);
        mapView.getOverlays().remove(destinationMarker);
        this.addStartMarker(routePoints.get(0));
        this.addDestinationMarker(routePoints.get(routePoints.size() - 1));

        zoomToShowRoute(routePoints);

        mapView.invalidate();
    }

    private void zoomToShowRoute(List<GeoPoint> routePoints) {
        try {
            if (routePoints == null || routePoints.size() < 2) return;

            double minLat = routePoints.get(0).getLatitude();
            double maxLat = routePoints.get(0).getLatitude();
            double minLon = routePoints.get(0).getLongitude();
            double maxLon = routePoints.get(0).getLongitude();

            for (GeoPoint point : routePoints) {
                minLat = Math.min(minLat, point.getLatitude());
                maxLat = Math.max(maxLat, point.getLatitude());
                minLon = Math.min(minLon, point.getLongitude());
                maxLon = Math.max(maxLon, point.getLongitude());
            }

            double latPadding = Math.max((maxLat - minLat) * 0.15, 0.002);
            double lonPadding = Math.max((maxLon - minLon) * 0.15, 0.002);

            BoundingBox boundingBox = new BoundingBox(
                    maxLat + latPadding, maxLon + lonPadding,
                    minLat - latPadding, minLon - lonPadding
            );

            mapView.zoomToBoundingBox(boundingBox, true, 100);

            double centerLat = (minLat + maxLat) / 2;
            double centerLon = (minLon + maxLon) / 2;
            GeoPoint center = new GeoPoint(centerLat, centerLon);

            mainHandler.postDelayed(() -> {
                double zoomLevel = mapView.getZoomLevelDouble();
                IMapController controller = mapView.getController();
                controller.setZoom(zoomLevel - 3);
                controller.setCenter(center);
            }, 300);

        } catch (Exception e) {
            Log.e("zoomToShowRoute", "Error zooming to route", e);
        }
    }

    public void clearRoute() {
        try {
            if (routePolyline != null) {
                mapView.getOverlays().remove(routePolyline);
                routePolyline = null;
            }
        } catch (Exception e) {
        }
    }


    public void initCallbackMapTap(Map.CallbackMapTap callbackMapTap) {
        this.callbackMapTap = callbackMapTap;
    }

    private void setupMapEvents() {
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (callbackMapTap != null) {
                    callbackMapTap.onTap(p);
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                if (callbackMapTap != null) {
                    callbackMapTap.onTap(p);
                }
                return true;
            }
        };

        mapEventsOverlay = new MapEventsOverlay(mReceive);
        mapView.getOverlays().add(mapEventsOverlay);
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                double zoomLevel = event.getZoomLevel();
                updateMarkerScale(zoomLevel);
                return true;
            }
        });
    }

    private void updateMarkerScale(double zoomLevel) {
        float scaleFactor = getScaleFactor(zoomLevel);
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_location_red);
        if (drawable != null) {
            int width = (int) (drawable.getIntrinsicWidth() * scaleFactor);
            int height = (int) (drawable.getIntrinsicHeight() * scaleFactor);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);

            destinationMarker.setIcon(new BitmapDrawable(context.getResources(), bitmap));
        }
        Drawable drawable1 = ContextCompat.getDrawable(context, R.drawable.ic_location_green);
        if (drawable1 != null) {
            int width = (int) (drawable1.getIntrinsicWidth() * scaleFactor);
            int height = (int) (drawable1.getIntrinsicHeight() * scaleFactor);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable1.setBounds(0, 0, width, height);
            drawable1.draw(canvas);

            startMarker.setIcon(new BitmapDrawable(context.getResources(), bitmap));
        }
    }

    private float getScaleFactor(double zoomLevel) {
        // Customize scaling based on zoom level
        if (zoomLevel >= 18) return 1.8f;
        if (zoomLevel >= 15) return 1.5f;
        return 1.3f; // default
    }

    private void setupLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        mapView.getOverlays().add(myLocationOverlay);
    }

    public void addStartMarker(GeoPoint point) {
        if (startMarker != null) {
            mapView.getOverlays().remove(startMarker);
        }

        startMarker = new Marker(mapView);
        startMarker.setPosition(point);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        float scaleFactor = getScaleFactor(mapView.getZoomLevelDouble());
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_location_green);
        assert drawable != null;
        int width = (int) (drawable.getIntrinsicWidth() * scaleFactor);
        int height = (int) (drawable.getIntrinsicHeight() * scaleFactor);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);

        startMarker.setIcon(new BitmapDrawable(context.getResources(), bitmap));
        mapView.getOverlays().add(startMarker);
        mapView.invalidate();
    }

    public void addDestinationMarker(GeoPoint point) {
        if (destinationMarker != null) {
            mapView.getOverlays().remove(destinationMarker);
        }
        destinationMarker = new Marker(mapView);
        destinationMarker.setPosition(point);
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        float scaleFactor = getScaleFactor(mapView.getZoomLevelDouble());
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_location_red);
        assert drawable != null;
        int width = (int) (drawable.getIntrinsicWidth() * scaleFactor);
        int height = (int) (drawable.getIntrinsicHeight() * scaleFactor);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        destinationMarker.setIcon(new BitmapDrawable(context.getResources(), bitmap));

        mapView.getOverlays().add(destinationMarker);
        mapView.invalidate();
    }

    public void clearMarkerTap() {
        if (mapTapMarker != null) {
            mapView.getOverlays().remove(mapTapMarker);
        }
    }

    public void setMapTapMarker(GeoPoint point) {
        if (mapTapMarker != null) {
            mapView.getOverlays().remove(mapTapMarker);
        }
        mapTapMarker = new Marker(mapView);
        mapTapMarker.setPosition(point);
        mapTapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        float scaleFactor = getScaleFactor(mapView.getZoomLevelDouble());
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_location_blue);
        assert drawable != null;
        int width = (int) (drawable.getIntrinsicWidth() * scaleFactor);
        int height = (int) (drawable.getIntrinsicHeight() * scaleFactor);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        mapTapMarker.setIcon(new BitmapDrawable(context.getResources(), bitmap));

        mapView.getOverlays().add(mapTapMarker);
        mapView.invalidate();
    }

    public void clearMarkers() {
        try {
            if (startMarker != null) {
                mapView.getOverlays().remove(startMarker);
                startMarker = null;
            }

            if (destinationMarker != null) {
                mapView.getOverlays().remove(destinationMarker);
                destinationMarker = null;
            }

            mapView.invalidate();
        } catch (Exception e) {
        }
    }

    public void clearMap() {
        clearMarkerTap();
        clearMarkers();
        clearRoute();
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

}
