package com.makitaxi.passenger;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LocationService {

    public interface ReverseGeocodeListener {
        void onReverseGeocodeSuccess(String address);

        void onReverseGeocodeError(String error);
    }

    public interface LocationSuggestionsListener {
        void onSuggestionsFound(List<String> foundResults);

        void onSuggestionsFoundError(String error);
    }

    private final Context context;
    private final ExecutorService executorService;
    private final Handler handler;

    private final OkHttpClient httpClient;

    private static final int PHOTON_TIMEOUT_SECONDS = 15;
    private static final int PHOTON_MAX_RETRIES = 2;

    public LocationService(Context context) {
        // Use application context to prevent memory leaks
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(PHOTON_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(PHOTON_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(PHOTON_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    private String cyrillicToSerbianLatin(String text) {
        if (text == null) return null;
        String[][] map = {
                {"А", "A"}, {"а", "a"}, {"Б", "B"}, {"б", "b"}, {"В", "V"}, {"в", "v"}, {"Г", "G"}, {"г", "g"}, {"Д", "D"}, {"д", "d"},
                {"Ђ", "Đ"}, {"ђ", "đ"}, {"Е", "E"}, {"е", "e"}, {"Ж", "Ž"}, {"ж", "ž"}, {"З", "Z"}, {"з", "z"}, {"И", "I"}, {"и", "i"},
                {"Ј", "J"}, {"ј", "j"}, {"К", "K"}, {"к", "k"}, {"Л", "L"}, {"л", "l"}, {"Љ", "Lj"}, {"љ", "lj"}, {"М", "M"}, {"м", "m"},
                {"Н", "N"}, {"н", "n"}, {"Њ", "Nj"}, {"њ", "nj"}, {"О", "O"}, {"о", "o"}, {"П", "P"}, {"п", "p"}, {"Р", "R"}, {"р", "r"},
                {"С", "S"}, {"с", "s"}, {"Т", "T"}, {"т", "t"}, {"Ћ", "Ć"}, {"ћ", "ć"}, {"У", "U"}, {"у", "u"}, {"Ф", "F"}, {"ф", "f"},
                {"Х", "H"}, {"х", "h"}, {"Ц", "C"}, {"ц", "c"}, {"Ч", "Č"}, {"ч", "č"}, {"Џ", "Dž"}, {"џ", "dž"}, {"Ш", "Š"}, {"ш", "š"}
        };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            boolean replaced = false;
            for (String[] pair : map) {
                if (pair[0].equals(ch)) {
                    sb.append(pair[1]);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) sb.append(ch);
        }
        return sb.toString();
    }

    public void getPhotonSuggestions(String query, LocationSuggestionsListener listener) {
        getPhotonSuggestionsWithRetry(query, listener, 0);
    }

    private void getPhotonSuggestionsWithRetry(String query, LocationSuggestionsListener listener, int attempt) {
        if (query == null || query.trim().length() < 2) {
            listener.onSuggestionsFound(Collections.emptyList());
            return;
        }
        Log.d("PhotonQuery", "Query: " + query + ", attempt: " + attempt);

        String url = Uri.parse("https://photon.komoot.io/api/")
                .buildUpon()
                .appendQueryParameter("q", query)
                .appendQueryParameter("lang", "default")
                .appendQueryParameter("limit", "30")
                .build()
                .toString();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "MakiTaxi/1.0")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("PhotonAPI", "Request failed: " + e.getMessage());
                if (attempt < PHOTON_MAX_RETRIES && e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) {
                    Log.w("PhotonAPI", "Retrying Photon request, attempt " + (attempt + 1));
                    getPhotonSuggestionsWithRetry(query, listener, attempt + 1);
                } else {
                    handler.post(() -> listener.onSuggestionsFound(Collections.emptyList()));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                List<String> suggestions = new ArrayList<>();
                try {
                    String bodyString = response.body() != null ? response.body().string() : null;
                    if (bodyString == null) {
                        throw new JSONException("Empty response body");
                    }
                    JSONObject root = new JSONObject(bodyString);
                    JSONArray features = root.optJSONArray("features");
                    if (features != null) {
                        for (int i = 0; i < features.length(); i++) {
                            JSONObject feature = features.getJSONObject(i);
                            JSONObject props = feature.getJSONObject("properties");
                            String country = props.optString("country", "");
                            String name = props.optString("name", "");
                            String city = props.optString("city", "");
                            String state = props.optString("state", "");
                            String full = name + (city.isEmpty() ? "" : ", " + city) + (state.isEmpty() ? "" : ", " + state);
                            // Convert to Serbian Latin before adding
                            Log.d("PhotonCountry", country + ": " + country);
                            if (country.startsWith("Србија")) {
                                String latinFull = cyrillicToSerbianLatin(full);
                                suggestions.add(latinFull);
                            }
                            if(suggestions.size() == 5) {
                                break;
                            }
                        }
                    } else {
                        Log.w("PhotonAPI", "No features array in response for query: " + query);
                    }
                } catch (Exception e) {
                    Log.e("PhotonParse", "Error parsing suggestions: " + e.getMessage());
                }

                if (suggestions.isEmpty()) {
                    Log.i("PhotonSerbia", "No results for query: " + query);
                }

                handler.post(() -> listener.onSuggestionsFound(suggestions));
            }
        });
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
