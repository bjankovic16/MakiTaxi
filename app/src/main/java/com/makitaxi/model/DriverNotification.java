package com.makitaxi.model;

import com.makitaxi.utils.NotificationStatus;

public class DriverNotification {
    private String driverId;
    private Long notificationTimestamp;

    private RideRequest rideRequest;

    private NotificationStatus status = NotificationStatus.CREATED;

    public DriverNotification() {
    }

    public DriverNotification(String driverId, Long notificationTimestamp, RideRequest rideRequest) {
        this.driverId = driverId;
        this.notificationTimestamp = notificationTimestamp;
        this.rideRequest = rideRequest;
    }
    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public Long getNotificationTimestamp() {
        return notificationTimestamp;
    }

    public void setNotificationTimestamp(Long notificationTimestamp) {
        this.notificationTimestamp = notificationTimestamp;
    }

    public RideRequest getRideRequest() {
        return rideRequest;
    }

    public void setRideRequest(RideRequest rideRequest) {
        this.rideRequest = rideRequest;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }
}