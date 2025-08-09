package com.makitaxi.model;

public class RideReview {
    private String reviewId;
    private String rideRequestId;
    private String passengerId;
    private String driverId;
    private String passengerName;
    private String driverName;
    private int rating;
    private String comment;
    private long timestamp;
    private String pickupAddress;
    private String dropoffAddress;
    private double price;
    private String carType;

    public RideReview() {
    }

    public RideReview(String reviewId, String rideRequestId, String passengerId, String driverId, 
                     String passengerName, String driverName, int rating, String comment, 
                     long timestamp, String pickupAddress, String dropoffAddress, 
                     double price, String carType) {
        this.reviewId = reviewId;
        this.rideRequestId = rideRequestId;
        this.passengerId = passengerId;
        this.driverId = driverId;
        this.passengerName = passengerName;
        this.driverName = driverName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.price = price;
        this.carType = carType;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
} 