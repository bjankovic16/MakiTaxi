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
    private static final String OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving/";

    private Context context;
    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;

    private Marker startMarker;
    private Marker destinationMarker;
    private Polyline routePolyline;
    private ExecutorService executorService;
    private Handler mainHandler;

    public interface RoutingCallback {
        void onRouteFound(List<GeoPoint> routePoints, double distance, double duration);
        void onRoutingError(String error);
    }

    public MapDriver(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
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
        
        // Set initial taxi icon
        updateTaxiIconSize();
        
        // Add zoom listener to update icon size
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                updateTaxiIconSize();
                return false;
            }
        });

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

    private void updateTaxiIconSize() {
        if (myLocationOverlay == null) return;
        
        double zoomLevel = mapView.getZoomLevelDouble();
        
        // Calculate icon size based on zoom level - SIMPLIFIED AND VISIBLE
        int iconSize;
        if (zoomLevel >= 18) {
            iconSize = 100; // Large icon for close zoom
        } else if (zoomLevel >= 15) {
            iconSize = 84; // Medium-large icon for medium zoom
        } else if (zoomLevel >= 12) {
            iconSize = 68; // Medium icon for far zoom
        } else {
            iconSize = 52; // Small but visible icon for very far zoom
        }
        
        // Create scaled taxi icon - SIMPLE APPROACH
        Drawable taxiIcon = ContextCompat.getDrawable(context, R.drawable.taxi);
        if (taxiIcon != null) {
            Bitmap taxiBitmap = drawableToBitmap(taxiIcon);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(taxiBitmap, iconSize, iconSize, true);
            
            myLocationOverlay.setDirectionArrow(scaledBitmap, scaledBitmap);
            myLocationOverlay.setPersonIcon(scaledBitmap);
        }
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

    private void getRouteFromOSRM(GeoPoint start, GeoPoint end, RoutingCallback callback) {
        if(start == null || end == null) {
            callback.onRoutingError("Invalid coordinates");
            return;
        }

        String url = OSRM_BASE_URL + start.getLongitude() + "," + start.getLatitude() + ";" +
                end.getLongitude() + "," + end.getLatitude() + "?overview=full&geometries=geojson";

        executorService.execute(() -> {
            try {
                URL urlObj = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.has("routes") && jsonResponse.getJSONArray("routes").length() > 0) {
                        JSONObject route = jsonResponse.getJSONArray("routes").getJSONObject(0);
                        JSONObject geometry = route.getJSONObject("geometry");
                        JSONArray coordinates = geometry.getJSONArray("coordinates");

                        List<GeoPoint> routePoints = new ArrayList<>();
                        for (int i = 0; i < coordinates.length(); i++) {
                            JSONArray coord = coordinates.getJSONArray(i);
                            double lon = coord.getDouble(0);
                            double lat = coord.getDouble(1);
                            routePoints.add(new GeoPoint(lat, lon));
                        }

                        double distance = route.getDouble("distance") / 1000.0; // Convert to km
                        double duration = route.getDouble("duration") / 60.0; // Convert to minutes

                        callback.onRouteFound(routePoints, distance, duration);
                    } else {
                        callback.onRoutingError("No route found");
                    }
                } else {
                    callback.onRoutingError("HTTP Error: " + responseCode);
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e("MapDriver", "Error getting route", e);
                callback.onRoutingError("Network error: " + e.getMessage());
            }
        });
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

    private void addStartMarker(GeoPoint point) {
        if (startMarker != null) {
            mapView.getOverlays().remove(startMarker);
        }
        startMarker = new Marker(mapView);
        startMarker.setPosition(point);
        startMarker.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_location_green));
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(startMarker);
    }

    private void addDestinationMarker(GeoPoint point) {
        if (destinationMarker != null) {
            mapView.getOverlays().remove(destinationMarker);
        }
        destinationMarker = new Marker(mapView);
        destinationMarker.setPosition(point);
        destinationMarker.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_location_red));
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(destinationMarker);
    }

    public void clearRoute() {
        try {
            if (routePolyline != null) {
                mapView.getOverlays().remove(routePolyline);
                routePolyline = null;
            }
            List<org.osmdroid.views.overlay.Overlay> overlays = new ArrayList<>(mapView.getOverlays());
            for (org.osmdroid.views.overlay.Overlay overlay : overlays) {
                if (overlay instanceof Polyline) {
                    mapView.getOverlays().remove(overlay);
                }
            }
        } catch (Exception e) {
            Log.e("MapDriver", "Error clearing route", e);
        }
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
        } catch (Exception e) {
            Log.e("MapDriver", "Error clearing markers", e);
        }
    }

    public void clearMap() {
        clearRoute();
        clearMarkers();
        mapView.invalidate();
    }

    public void drawDriverRouteToPickup(GeoPoint driverLocation, GeoPoint pickupPoint, RoutingCallback callback) {
        clearMarkers();
        clearRoute();
        getRouteFromOSRM(driverLocation, pickupPoint, new MapDriver.RoutingCallback() {
            @Override
            public void onRouteFound(List<GeoPoint> routePoints, double distance, double duration) {
                mainHandler.post(() -> {
                    displayDriverToPickupRoute(routePoints);
                    callback.onRouteFound(routePoints, distance, duration);
                });
            }

            @Override
            public void onRoutingError(String error) {
                mainHandler.post(() -> callback.onRoutingError(error));
            }
        });
    }

    public void drawPickupToDestinationRoute(GeoPoint pickupPoint, GeoPoint destinationPoint, RoutingCallback callback) {
        getRouteFromOSRM(pickupPoint, destinationPoint, new MapDriver.RoutingCallback() {
            @Override
            public void onRouteFound(List<GeoPoint> routePoints, double distance, double duration) {
                mainHandler.post(() -> {
                    displayPickupToDestinationRoute(routePoints);
                    callback.onRouteFound(routePoints, distance, duration);
                });
            }

            @Override
            public void onRoutingError(String error) {
                mainHandler.post(() -> callback.onRoutingError(error));
            }
        });
    }

    private void displayDriverToPickupRoute(List<GeoPoint> routePoints) {
        if (routePoints == null || routePoints.isEmpty()) {
            return;
        }

        Polyline driverRoute = new Polyline();
        driverRoute.setPoints(routePoints);
        driverRoute.setColor(Color.parseColor("#343B71"));
        driverRoute.setWidth(8.0f);
        driverRoute.setGeodesic(false);

        mapView.getOverlays().add(driverRoute);

        addStartMarker(routePoints.get(routePoints.size() - 1));

        zoomToShowRoute(routePoints);
        mapView.invalidate();
    }

    private void displayPickupToDestinationRoute(List<GeoPoint> routePoints) {
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
        addStartMarker(routePoints.get(0));
        addDestinationMarker(routePoints.get(routePoints.size() - 1));

        zoomToShowRoute(routePoints);
        mapView.invalidate();
    }
}
