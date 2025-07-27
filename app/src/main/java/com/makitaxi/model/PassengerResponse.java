package com.makitaxi.model;

import com.makitaxi.utils.NotificationStatus;

public class PassengerResponse {
    private String driverId;

    private String passengerId;

    private String rideRequestId;
    private Long notificationTimestamp;

    private NotificationStatus status;

    private String driverId_RideRequestId;

    public PassengerResponse() {}

    public PassengerResponse(String driverId, String passengerId, String rideRequestId, Long notificationTimestamp, NotificationStatus status) {
        this.driverId = driverId;
        this.passengerId = passengerId;
        this.rideRequestId = rideRequestId;
        this.driverId_RideRequestId = driverId + "_" + rideRequestId;
        this.notificationTimestamp = notificationTimestamp;
        this.status = status;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }

    public Long getNotificationTimestamp() {
        return notificationTimestamp;
    }

    public void setNotificationTimestamp(Long notificationTimestamp) {
        this.notificationTimestamp = notificationTimestamp;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getRideRequestId() {
        return rideRequestId;
    }

    public void setRideRequestId(String rideRequestId) {
        this.rideRequestId = rideRequestId;
    }

    public String getDriverId_RideRequestId() {
        return driverId_RideRequestId;
    }

    public void setDriverId_RideRequestId(String driverId_RideRequestId) {
        this.driverId_RideRequestId = driverId_RideRequestId;
    }
}
