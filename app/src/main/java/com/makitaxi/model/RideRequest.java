package com.makitaxi.model;

import org.osmdroid.util.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class RideRequest {
    private String requestId;
    private String passengerId;
    private String driverId;
    private String status; // PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
    private String carType; // BASIC, LUXURY, TRANSPORT
    private double pickupLat;
    private double pickupLng;
    private double dropoffLat;
    private double dropoffLng;
    private String pickupAddress;
    private String dropoffAddress;
    private long timestamp;
    private double estimatedPrice;
    private double distance; // in km
    private double duration; // in minutes
    private Map<String, Boolean> declinedBy; // Keeps track of drivers who declined

    // Required empty constructor for Firebase
    public RideRequest() {
        this.declinedBy = new HashMap<>();
    }

    public RideRequest(String passengerId, GeoPoint pickup, GeoPoint dropoff,
                      String pickupAddress, String dropoffAddress, String carType,
                      double distance, double duration) {
        this.passengerId = passengerId;
        this.pickupLat = pickup.getLatitude();
        this.pickupLng = pickup.getLongitude();
        this.dropoffLat = dropoff.getLatitude();
        this.dropoffLng = dropoff.getLongitude();
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.carType = carType;
        this.status = "PENDING";
        this.timestamp = System.currentTimeMillis();
        this.distance = distance;
        this.duration = duration;
        this.declinedBy = new HashMap<>();
        // Simple price calculation based on distance and car type
        double basePrice = 150; // Base price in RSD
        double perKmPrice;
        switch (carType) {
            case "LUXURY":
                perKmPrice = 120;
                break;
            case "TRANSPORT":
                perKmPrice = 100;
                break;
            case "BASIC":
            default:
                perKmPrice = 80;
                break;
        }
        this.estimatedPrice = basePrice + (distance * perKmPrice);
    }

    // Getters and setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCarType() { return carType; }
    public void setCarType(String carType) { this.carType = carType; }

    public double getPickupLat() { return pickupLat; }
    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }

    public double getPickupLng() { return pickupLng; }
    public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }

    public double getDropoffLat() { return dropoffLat; }
    public void setDropoffLat(double dropoffLat) { this.dropoffLat = dropoffLat; }

    public double getDropoffLng() { return dropoffLng; }
    public void setDropoffLng(double dropoffLng) { this.dropoffLng = dropoffLng; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDropoffAddress() { return dropoffAddress; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(double estimatedPrice) { this.estimatedPrice = estimatedPrice; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }

    public Map<String, Boolean> getDeclinedBy() { return declinedBy; }
    public void setDeclinedBy(Map<String, Boolean> declinedBy) { this.declinedBy = declinedBy; }

    public GeoPoint getPickupLocation() {
        return new GeoPoint(pickupLat, pickupLng);
    }

    public GeoPoint getDropoffLocation() {
        return new GeoPoint(dropoffLat, dropoffLng);
    }
} 