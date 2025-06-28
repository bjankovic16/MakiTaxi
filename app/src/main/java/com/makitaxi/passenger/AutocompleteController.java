package com.makitaxi.passenger;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AutocompleteController - Handles all autocomplete functionality
 * 
 * Responsibilities:
 * - Managing autocomplete text input
 * - API calls for address suggestions
 * - Dropdown display and styling
 * - Text filtering and suggestion management
 * - Coordinate mapping for suggestions
 */
public class AutocompleteController {
    
    private static final String TAG = "AutocompleteController";
    private static final int MIN_CHARS_FOR_API = 2;
    private static final int SEARCH_DELAY_MS = 500;
    private static final int MAX_SUGGESTIONS = 3;
    
    private Context context;
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> adapter;
    private ExecutorService executorService;
    private Handler searchHandler;
    private Runnable searchRunnable;
    
    // Data storage
    private List<String> suggestions;
    private Map<String, GeoPoint> suggestionCoordinates;
    private List<String> testSuggestions;
    private Map<String, GeoPoint> testCoordinates;
    
    // Callbacks
    private AutocompleteListener autocompleteListener;
    
    /**
     * Interface for autocomplete callbacks
     */
    public interface AutocompleteListener {
        void onSuggestionSelected(String suggestion, GeoPoint coordinates);
        void onTextChanged(String text);
    }
    
    public AutocompleteController(Context context, AutoCompleteTextView autoCompleteTextView) {
        this.context = context;
        this.autoCompleteTextView = autoCompleteTextView;
        this.executorService = Executors.newSingleThreadExecutor();
        this.searchHandler = new Handler(Looper.getMainLooper());
        
        initializeData();
        setupAdapter();
        setupTextWatcher();
        setupItemClickListener();
        setupFocusListener();
        
        Log.d(TAG, "AutocompleteController initialized");
    }
    
    public void setAutocompleteListener(AutocompleteListener listener) {
        this.autocompleteListener = listener;
    }
    
    /**
     * Initialize data structures and test data
     */
    private void initializeData() {
        suggestions = new ArrayList<>();
        suggestionCoordinates = new HashMap<>();
        testSuggestions = new ArrayList<>();
        testCoordinates = new HashMap<>();
        
        // Add more comprehensive test suggestions for Belgrade with exact addresses
        addTestSuggestion("Knez Mihailova 1, Belgrade, Serbia", new GeoPoint(44.8176, 20.4633));
        addTestSuggestion("Nikola Tesla Airport, Belgrade, Serbia", new GeoPoint(44.8184, 20.3091));
        addTestSuggestion("Belgrade Fortress, Kalemegdan, Belgrade, Serbia", new GeoPoint(44.8225, 20.4504));
        addTestSuggestion("Skadarlija Street, Belgrade, Serbia", new GeoPoint(44.8158, 20.4606));
        addTestSuggestion("Zeleni Venac Market, Belgrade, Serbia", new GeoPoint(44.8167, 20.4598));
        addTestSuggestion("Slavija Square, Belgrade, Serbia", new GeoPoint(44.8039, 20.4656));
        addTestSuggestion("Bulevar Mihajla Pupina, New Belgrade, Serbia", new GeoPoint(44.8125, 20.4612));
        addTestSuggestion("Ada Ciganlija Beach, Belgrade, Serbia", new GeoPoint(44.7894, 20.4036));
        addTestSuggestion("Terazije Square, Belgrade, Serbia", new GeoPoint(44.8144, 20.4608));
        addTestSuggestion("Republic Square, Belgrade, Serbia", new GeoPoint(44.8166, 20.4594));
        addTestSuggestion("Belgrade Central Station, Belgrade, Serbia", new GeoPoint(44.8059, 20.4564));
        addTestSuggestion("Tasmajdan Park, Belgrade, Serbia", new GeoPoint(44.8075, 20.4725));
        
        Log.d(TAG, "Enhanced test suggestions initialized: " + testSuggestions.size() + " items");
    }
    
    /**
     * Add test suggestion with coordinates
     */
    private void addTestSuggestion(String suggestion, GeoPoint coordinates) {
        testSuggestions.add(suggestion);
        testCoordinates.put(suggestion, coordinates);
    }
    
    /**
     * Setup custom adapter for dropdown styling
     */
    private void setupAdapter() {
        adapter = new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, suggestions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                
                // Style the dropdown item
                textView.setTextColor(context.getResources().getColor(android.R.color.black));
                textView.setBackgroundColor(context.getResources().getColor(android.R.color.white));
                textView.setPadding(16, 12, 16, 12);
                textView.setTextSize(16);
                
                Log.d(TAG, "Creating dropdown view for: " + getItem(position));
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                
                // Style the dropdown item
                textView.setTextColor(context.getResources().getColor(android.R.color.black));
                textView.setBackgroundColor(context.getResources().getColor(android.R.color.white));
                textView.setPadding(16, 12, 16, 12);
                textView.setTextSize(16);
                
                Log.d(TAG, "Creating dropdown view for: " + getItem(position));
                return view;
            }
        };
        
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setThreshold(1);
        autoCompleteTextView.setDropDownBackgroundResource(android.R.color.white);
        
        Log.d(TAG, "Adapter setup completed");
    }
    
    /**
     * Setup text watcher for input changes
     */
    private void setupTextWatcher() {
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                Log.d(TAG, "Text changed to: '" + query + "'");
                
                // Defer heavy operations to avoid blocking keyboard animations
                searchHandler.post(() -> {
                    handleTextChange(query);
                    
                    // Notify listener
                    if (autocompleteListener != null) {
                        autocompleteListener.onTextChanged(query);
                    }
                });
            }
        });
    }
    
    /**
     * Handle text input changes (optimized for performance)
     */
    private void handleTextChange(String query) {
        // Cancel any pending search
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        
        if (query.isEmpty()) {
            // Defer showing suggestions to avoid blocking UI
            searchHandler.postDelayed(() -> showTestSuggestions(), 50);
        } else if (query.length() == 1) {
            // Defer filtering to avoid blocking UI
            searchHandler.postDelayed(() -> filterTestSuggestions(query), 50);
        } else if (query.length() >= MIN_CHARS_FOR_API) {
            // Show current suggestions with minimal delay
            searchHandler.postDelayed(() -> showCurrentSuggestions(), 50);
            
            // Schedule API search with longer delay to avoid excessive calls
            searchRunnable = () -> searchWithAPI(query);
            searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
        }
    }
    
    /**
     * Show test suggestions
     */
    private void showTestSuggestions() {
        Log.d(TAG, "Showing test suggestions");
        updateSuggestions(testSuggestions, testCoordinates);
    }
    
    /**
     * Filter test suggestions by query
     */
    private void filterTestSuggestions(String query) {
        Log.d(TAG, "Filtering test suggestions for: " + query);
        
        List<String> filtered = new ArrayList<>();
        Map<String, GeoPoint> filteredCoordinates = new HashMap<>();
        
        String lowerQuery = query.toLowerCase();
        for (String suggestion : testSuggestions) {
            if (suggestion.toLowerCase().contains(lowerQuery)) {
                filtered.add(suggestion);
                filteredCoordinates.put(suggestion, testCoordinates.get(suggestion));
            }
        }
        
        updateSuggestions(filtered, filteredCoordinates);
    }
    
    /**
     * Show current suggestions
     */
    private void showCurrentSuggestions() {
        Log.d(TAG, "Showing current suggestions: " + suggestions.size());
        refreshDropdown();
    }
    
    /**
     * Search for destinations using Nominatim API with enhanced parameters
     */
    private void searchWithAPI(String query) {
        if (query.length() < MIN_CHARS_FOR_API) {
            Log.d(TAG, "Query too short for API search: '" + query + "'");
            return;
        }
        
        Log.d(TAG, "Searching with API for: '" + query + "'");
        
        executorService.execute(() -> {
            try {
                // Enhanced query for better Serbia/Belgrade results
                String enhancedQuery = enhanceQueryForSerbia(query);
                
                // Build enhanced Nominatim URL with more specific parameters
                String urlString = "https://nominatim.openstreetmap.org/search?" +
                        "format=json" +
                        "&addressdetails=1" +
                        "&limit=" + MAX_SUGGESTIONS +
                        "&countrycodes=rs" + // Restrict to Serbia
                        "&bounded=1" + // Prefer results within viewbox
                        "&viewbox=19.7,45.4,21.2,44.2" + // Belgrade region bounding box
                        "&dedupe=1" + // Remove duplicate results
                        "&namedetails=1" + // Include name details
                        "&extratags=1" + // Include extra tags for better context
                        "&q=" + java.net.URLEncoder.encode(enhancedQuery, "UTF-8");
                
                Log.d(TAG, "Enhanced Nominatim URL: " + urlString);
                
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", "MakiTaxi/1.0 (contact@makitaxi.com)");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // Parse and display results on main thread
                    searchHandler.post(() -> parseAndDisplayResults(response.toString(), query));
                    
                } else {
                    Log.w(TAG, "Nominatim API returned error code: " + responseCode);
                    searchHandler.post(() -> {
                        Log.d(TAG, "API search failed, showing test suggestions");
                        showTestSuggestions();
                    });
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error during API search", e);
                searchHandler.post(() -> {
                    Log.d(TAG, "API search error, showing test suggestions");
                    showTestSuggestions();
                });
            }
        });
    }
    
    /**
     * Parse API results and display them with enhanced address formatting
     */
    private void parseAndDisplayResults(String jsonResponse, String originalQuery) {
        try {
            Log.d(TAG, "Parsing API response for query: '" + originalQuery + "'");
            
            org.json.JSONArray results = new org.json.JSONArray(jsonResponse);
            List<String> apiSuggestions = new ArrayList<>();
            Map<String, GeoPoint> apiCoordinates = new HashMap<>();
            
            for (int i = 0; i < results.length(); i++) {
                org.json.JSONObject result = results.getJSONObject(i);
                
                // Extract coordinates
                double lat = result.getDouble("lat");
                double lon = result.getDouble("lon");
                GeoPoint coordinates = new GeoPoint(lat, lon);
                
                // Build enhanced display name with proper formatting
                String displayName = buildEnhancedDisplayName(result);
                
                if (displayName != null && !displayName.trim().isEmpty()) {
                    apiSuggestions.add(displayName);
                    apiCoordinates.put(displayName, coordinates);
                    
                    Log.d(TAG, "Added API suggestion: " + displayName + " at " + coordinates);
                }
            }
            
            if (!apiSuggestions.isEmpty()) {
                Log.d(TAG, "API search successful: " + apiSuggestions.size() + " results");
                updateSuggestions(apiSuggestions, apiCoordinates);
            } else {
                Log.d(TAG, "No valid API results, showing test suggestions");
                showTestSuggestions();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing API results", e);
            showTestSuggestions();
        }
    }
    
    /**
     * Build enhanced display name from Nominatim result with proper address formatting
     */
    private String buildEnhancedDisplayName(org.json.JSONObject result) {
        try {
            // Try to get address components for better formatting
            if (result.has("address")) {
                org.json.JSONObject address = result.getJSONObject("address");
                StringBuilder displayName = new StringBuilder();
                
                // Build address in proper order: number + street, area, city, country
                if (address.has("house_number") && address.has("road")) {
                    displayName.append(address.getString("road"))
                              .append(" ")
                              .append(address.getString("house_number"));
                } else if (address.has("road")) {
                    displayName.append(address.getString("road"));
                } else if (address.has("name")) {
                    displayName.append(address.getString("name"));
                }
                
                // Add area/suburb if available
                if (address.has("suburb")) {
                    if (displayName.length() > 0) displayName.append(", ");
                    displayName.append(address.getString("suburb"));
                } else if (address.has("neighbourhood")) {
                    if (displayName.length() > 0) displayName.append(", ");
                    displayName.append(address.getString("neighbourhood"));
                }
                
                // Add city
                String city = null;
                if (address.has("city")) {
                    city = address.getString("city");
                } else if (address.has("town")) {
                    city = address.getString("town");
                } else if (address.has("village")) {
                    city = address.getString("village");
                }
                
                if (city != null) {
                    if (displayName.length() > 0) displayName.append(", ");
                    displayName.append(city);
                }
                
                // Add country if not Serbia (to keep it clean for local results)
                if (address.has("country") && 
                    !address.getString("country").equalsIgnoreCase("Serbia") &&
                    !address.getString("country").equalsIgnoreCase("Srbija")) {
                    displayName.append(", ").append(address.getString("country"));
                } else {
                    // For Serbia, just add "Serbia" for clarity
                    displayName.append(", Serbia");
                }
                
                String finalName = displayName.toString().trim();
                if (!finalName.isEmpty()) {
                    return finalName;
                }
            }
            
            // Fallback to display_name but clean it up
            if (result.has("display_name")) {
                String displayName = result.getString("display_name");
                // Clean up the display name by removing excessive country/region info
                String[] parts = displayName.split(",");
                if (parts.length > 3) {
                    // Take first 3-4 most relevant parts
                    StringBuilder cleaned = new StringBuilder();
                    for (int i = 0; i < Math.min(4, parts.length); i++) {
                        if (i > 0) cleaned.append(", ");
                        cleaned.append(parts[i].trim());
                    }
                    return cleaned.toString();
                }
                return displayName;
            }
            
            return null;
            
        } catch (Exception e) {
            Log.w(TAG, "Error building display name", e);
            // Fallback to basic display_name
            try {
                return result.getString("display_name");
            } catch (Exception ex) {
                return null;
            }
        }
    }
    
    /**
     * Enhanced query builder for better Serbia/Belgrade results
     */
    private String enhanceQueryForSerbia(String originalQuery) {
        String enhanced = originalQuery.trim();
        
        // If query looks like a street address but doesn't mention city, add Belgrade
        if (enhanced.matches(".*\\d+.*") && // Contains numbers (likely address)
            !enhanced.toLowerCase().contains("belgrade") &&
            !enhanced.toLowerCase().contains("beograd") &&
            !enhanced.toLowerCase().contains("novi sad") &&
            !enhanced.toLowerCase().contains("niš") &&
            !enhanced.toLowerCase().contains("nis")) {
            enhanced += ", Belgrade";
        }
        
        // If it's just a street name without city context, add Belgrade
        if (!enhanced.toLowerCase().contains("belgrade") &&
            !enhanced.toLowerCase().contains("beograd") &&
            !enhanced.toLowerCase().contains("serbia") &&
            !enhanced.toLowerCase().contains("srbija") &&
            enhanced.split("\\s+").length <= 3) { // Short queries likely need city context
            enhanced += ", Belgrade, Serbia";
        }
        
        Log.d(TAG, "Enhanced query: '" + originalQuery + "' -> '" + enhanced + "'");
        return enhanced;
    }
    
    /**
     * Update suggestions and refresh dropdown (optimized for performance)
     */
    private void updateSuggestions(List<String> newSuggestions, Map<String, GeoPoint> newCoordinates) {
        Log.d(TAG, "Updating suggestions: " + newSuggestions.size() + " items");
        
        // Perform data updates off the main thread if possible
        searchHandler.post(() -> {
            try {
                // Clear and update data
                suggestions.clear();
                suggestionCoordinates.clear();
                
                suggestions.addAll(newSuggestions);
                suggestionCoordinates.putAll(newCoordinates);
                
                // Batch adapter updates to reduce UI thread work
                adapter.setNotifyOnChange(false); // Temporarily disable automatic notifications
                adapter.clear();
                adapter.addAll(suggestions);
                adapter.setNotifyOnChange(true); // Re-enable notifications
                adapter.notifyDataSetChanged(); // Single notification
                
                Log.d(TAG, "Adapter updated with " + adapter.getCount() + " items");
                
                // Refresh dropdown with delay to avoid keyboard animation conflicts
                searchHandler.postDelayed(() -> refreshDropdown(), 100);
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating suggestions: " + e.getMessage());
            }
        });
    }
    
    /**
     * Refresh dropdown display (optimized to avoid blocking keyboard animations)
     */
    private void refreshDropdown() {
        // Only refresh if the view has focus and is not in the middle of keyboard animation
        if (!autoCompleteTextView.hasFocus()) {
            return;
        }
        
        // Use longer delay to avoid interfering with keyboard animations
        searchHandler.postDelayed(() -> {
            if (autoCompleteTextView.hasFocus() && autoCompleteTextView.isShown()) {
                try {
                    autoCompleteTextView.dismissDropDown();
                    
                    // Wait a bit longer before showing to ensure keyboard animation is complete
                    searchHandler.postDelayed(() -> {
                        if (autoCompleteTextView.hasFocus() && autoCompleteTextView.isShown()) {
                            autoCompleteTextView.showDropDown();
                            Log.d(TAG, "Dropdown refreshed and shown");
                        }
                    }, 150);
                } catch (Exception e) {
                    Log.w(TAG, "Error refreshing dropdown: " + e.getMessage());
                }
            }
        }, 100);
    }
    
    /**
     * Setup item click listener
     */
    private void setupItemClickListener() {
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedSuggestion = adapter.getItem(position);
                GeoPoint coordinates = suggestionCoordinates.get(selectedSuggestion);
                
                Log.d(TAG, "Item selected: " + selectedSuggestion);
                
                if (coordinates != null && autocompleteListener != null) {
                    autocompleteListener.onSuggestionSelected(selectedSuggestion, coordinates);
                }
            }
        });
    }
    
    /**
     * Setup focus listener
     */
    private void setupFocusListener() {
        autoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                Log.d(TAG, "AutoCompleteTextView focused");
                String currentText = autoCompleteTextView.getText().toString().trim();
                
                if (currentText.isEmpty()) {
                    showTestSuggestions();
                } else {
                    showCurrentSuggestions();
                }
            }
        });
        
        autoCompleteTextView.setOnClickListener(v -> {
            Log.d(TAG, "AutoCompleteTextView clicked");
            if (autoCompleteTextView.hasFocus()) {
                autoCompleteTextView.showDropDown();
            }
        });
    }
    
    /**
     * Clear input and suggestions
     */
    public void clearInput() {
        autoCompleteTextView.setText("");
        suggestions.clear();
        suggestionCoordinates.clear();
        adapter.clear();
        adapter.notifyDataSetChanged();
    }
    
    /**
     * Get the AutoCompleteTextView widget
     * @return The AutoCompleteTextView instance
     */
    public AutoCompleteTextView getAutoCompleteTextView() {
        return autoCompleteTextView;
    }
    
    /**
     * Get current text from the autocomplete field
     * @return Current text content
     */
    public String getText() {
        return autoCompleteTextView != null ? autoCompleteTextView.getText().toString() : "";
    }
    
    /**
     * Set text in the autocomplete field
     * @param text Text to set
     */
    public void setText(String text) {
        if (autoCompleteTextView != null) {
            autoCompleteTextView.setText(text);
        }
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        Log.d(TAG, "AutocompleteController cleaned up");
    }
} 