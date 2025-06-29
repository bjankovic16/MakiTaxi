package com.makitaxi.passenger;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.osmdroid.util.GeoPoint;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationService {

    public interface ReverseGeocodeListener {
        void onReverseGeocodeSuccess(String address);
        void onReverseGeocodeError(String error);
    }

    private Context context;
    private ExecutorService executorService;

    private Handler handler;

    private final android.location.LocationManager androidLocationManager;

    public LocationService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());

        androidLocationManager = (android.location.LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void reverseGeocode(GeoPoint location, LocationService.ReverseGeocodeListener listener) {
        if (location == null || listener == null) {
            return;
        }

        if (Geocoder.isPresent()) {
            new Thread(() -> {
                try {
                    Geocoder geocoder = new Geocoder(context, java.util.Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(
                            location.getLatitude(),
                            location.getLongitude(),
                            1
                    );

                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String addressText = formatAddress(address);

                        // Return result on main thread
                        ((Activity) context).runOnUiThread(() -> {
                            listener.onReverseGeocodeSuccess(addressText);
                        });

                    } else {
                        ((Activity) context).runOnUiThread(() -> {
                            listener.onReverseGeocodeError("No address found");
                        });
                    }
                } catch (Exception e) {
                    ((Activity) context).runOnUiThread(() -> {
                        listener.onReverseGeocodeError("Geocoding failed: " + e.getMessage());

                    });
                }
            }).start();
        } else {
            listener.onReverseGeocodeError("Geocoder not available");
        }
    }

    private String formatAddress(Address address) {
        StringBuilder addressText = new StringBuilder();

        if (address.getThoroughfare() != null) {
            addressText.append(address.getThoroughfare());
            if (address.getSubThoroughfare() != null) {
                addressText.append(" ").append(address.getSubThoroughfare());
            }
        }

        if (address.getLocality() != null) {
            if (addressText.length() > 0) {
                addressText.append(", ");
            }
            addressText.append(address.getLocality());
        }

        if (address.getCountryName() != null &&
                !address.getCountryName().equalsIgnoreCase("Serbia") &&
                !address.getCountryName().equalsIgnoreCase("RS")) {
            if (addressText.length() > 0) {
                addressText.append(", ");
            }
            addressText.append(address.getCountryName());
        }

        if (addressText.length() == 0) {
            addressText.append(String.format("%.4f, %.4f", address.getLatitude(), address.getLongitude()));
        }

        return addressText.toString();
    }


}
