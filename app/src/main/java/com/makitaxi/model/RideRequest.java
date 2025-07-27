package com.makitaxi.model;

import com.makitaxi.utils.NotificationStatus;

import java.util.HashMap;
import java.util.Map;

public class RideRequest {
    private String requestId;
    private String passengerId;
    private String driverId;
    private String notifiedDriverId;
    private Long notificationTimestamp;
    private Long timeout;
    private Map<String, Boolean> declinedBy;

    // Changed from GeoPoint to primitive types
    private double pickupLatitude;
    private double pickupLongitude;
    private double dropoffLatitude;
    private double dropoffLongitude;

    private String pickupAddress;
    private String dropoffAddress;
    private String carType;
    private NotificationStatus status;
    private double distance;
    private double duration;
    private double estimatedPrice;

    public RideRequest() {
        // Required empty constructor for Firebase
    }

    public RideRequest(String passengerId, double pickupLatitude, double pickupLongitude, double dropoffLatitude, double dropoffLongitude,
                       String pickupAddress, String dropoffAddress,
                       String carType, double distance, double duration) {
        this.passengerId = passengerId;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.carType = carType;
        this.status = NotificationStatus.CREATED;
        this.distance = distance;
        this.duration = duration;
        this.declinedBy = new HashMap<>();

        // Price calculation remains the same
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

    // Getters and Setters for primitive coordinates
    public double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public double getDropoffLatitude() {
        return dropoffLatitude;
    }

    public void setDropoffLatitude(double dropoffLatitude) {
        this.dropoffLatitude = dropoffLatitude;
    }

    public double getDropoffLongitude() {
        return dropoffLongitude;
    }

    public void setDropoffLongitude(double dropoffLongitude) {
        this.dropoffLongitude = dropoffLongitude;
    }

    // Rest of the getters and setters remain the same
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getNotifiedDriverId() {
        return notifiedDriverId;
    }

    public void setNotifiedDriverId(String notifiedDriverId) {
        this.notifiedDriverId = notifiedDriverId;
    }

    public Long getNotificationTimestamp() {
        return notificationTimestamp;
    }

    public void setNotificationTimestamp(Long notificationTimestamp) {
        this.notificationTimestamp = notificationTimestamp;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Map<String, Boolean> getDeclinedBy() {
        return declinedBy;
    }

    public void setDeclinedBy(Map<String, Boolean> declinedBy) {
        this.declinedBy = declinedBy;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(double estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }
}