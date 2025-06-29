package com.makitaxi.passenger;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationService {

    public interface ReverseGeocodeListener {
        void onReverseGeocodeSuccess(String address);
        void onReverseGeocodeError(String error);
    }

    private final Context context;
    private final ExecutorService executorService;
    private final Handler handler;

    public LocationService(Context context) {
        // Use application context to prevent memory leaks
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void reverseGeocode(GeoPoint location, ReverseGeocodeListener listener) {
        if (location == null || listener == null) return;

        if (!Geocoder.isPresent()) {
            handler.post(() -> listener.onReverseGeocodeError("Geocoder not available"));
            return;
        }

        executorService.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                if (addresses != null && !addresses.isEmpty()) {
                    String addressText = formatAddress(addresses.get(0));
                    handler.post(() -> listener.onReverseGeocodeSuccess(addressText));
                } else {
                    handler.post(() -> listener.onReverseGeocodeError("No address found"));
                }

            } catch (IOException e) {
                handler.post(() -> listener.onReverseGeocodeError("Network error: " + e.getMessage()));
            } catch (Exception e) {
                handler.post(() -> listener.onReverseGeocodeError("Unexpected error: " + e.getMessage()));
            }
        });
    }

    private String formatAddress(Address address) {
        StringBuilder addressText = new StringBuilder();

        // Include house number even without street name
        if (address.getSubThoroughfare() != null) {
            addressText.append(address.getSubThoroughfare()).append(" ");
        }

        if (address.getThoroughfare() != null) {
            addressText.append(address.getThoroughfare());
        }

        if (address.getLocality() != null) {
            if (addressText.length() > 0) addressText.append(", ");
            addressText.append(address.getLocality());
        }

        if (address.getCountryName() != null &&
                !address.getCountryName().equalsIgnoreCase("Serbia") &&
                !address.getCountryName().equalsIgnoreCase("RS")) {
            if (addressText.length() > 0) addressText.append(", ");
            addressText.append(address.getCountryName());
        }

        if (addressText.length() == 0) {
            addressText.append(String.format(Locale.getDefault(), "%.4f, %.4f",
                    address.getLatitude(), address.getLongitude()));
        }

        return addressText.toString();
    }
    // Optional: call when your app no longer needs this service
    public void shutdown() {
        executorService.shutdown();
    }
}
