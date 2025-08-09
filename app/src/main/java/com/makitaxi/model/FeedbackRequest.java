package com.makitaxi.model;

import java.io.Serializable;

public class FeedbackRequest implements Serializable {
    private String feedbackId;
    private String rideRequestId;
    private String passengerId;
    private String driverId;
    private String passengerName;
    private String driverName;
    private String pickupAddress;
    private String dropoffAddress;
    private double price;
    private String carType;
    private double distance; // in kilometers
    private long timestamp;
    private boolean isSubmitted;
    private int rating;
    private String comment;

    public FeedbackRequest() {
    }

    public FeedbackRequest(String feedbackId, String rideRequestId, String passengerId, String driverId,
                          String passengerName, String driverName, String pickupAddress, String dropoffAddress,
                          double price, String carType, double distance, long timestamp) {
        this.feedbackId = feedbackId;
        this.rideRequestId = rideRequestId;
        this.passengerId = passengerId;
        this.driverId = driverId;
        this.passengerName = passengerName;
        this.driverName = driverName;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.price = price;
        this.carType = carType;
        this.distance = distance;
        this.timestamp = timestamp;
        this.isSubmitted = false;
        this.rating = 0;
        this.comment = "";
    }

    public String getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId;
    }

    public String getRideRequestId() {
        return rideRequestId;
    }

    public void setRideRequestId(String rideRequestId) {
        this.rideRequestId = rideRequestId;
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

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSubmitted() {
        return isSubmitted;
    }

    public void setSubmitted(boolean submitted) {
        isSubmitted = submitted;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
} 