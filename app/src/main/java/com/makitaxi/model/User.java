package com.makitaxi.model;

public class User {
    private String fullName;
    private String email;
    private String username;
    private String phone;
    private String password;
    private String role;
    private boolean verified;
    private String profilePicture;

    // Driver-specific fields
    private String licenseNumber;
    private String carModel;
    private String carColor;
    private String carPlateNumber;
    private String carType; // BASIC, LUXURY, LIMOUSINE
    private boolean available;
    private double rating;
    private int totalRides;
    private int totalDistance; // in kilometers
    private int ratingCount; // number of ratings received
    private double totalRatingSum; // sum of all ratings for average calculation
    private double totalMoneySpent; // for passengers - money spent on rides
    private double totalMoneyEarned; // for drivers - money earned from rides
    private String gender;
    private String birthday;

    // Required empty constructor for Firebase
    public User() {
        this.role = "PASSENGER"; // Default role
        this.verified = false; // Default verification status
        this.available = false;
        this.rating = 0.0;
        this.totalRides = 0;
        this.totalDistance = 0;
        this.ratingCount = 0;
        this.totalRatingSum = 0.0;
        this.totalMoneySpent = 0.0;
        this.totalMoneyEarned = 0.0;
    }

    // Optional: constructor with fields
    public User(String fullName, String email, String phone, String username) {
        this();
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.username = username;
    }

    // Getters and Setters

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public String getCarColor() {
        return carColor;
    }

    public void setCarColor(String carColor) {
        this.carColor = carColor;
    }

    public String getCarPlateNumber() {
        return carPlateNumber;
    }

    public void setCarPlateNumber(String carPlateNumber) {
        this.carPlateNumber = carPlateNumber;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getTotalRides() {
        return totalRides;
    }

    public void setTotalRides(int totalRides) {
        this.totalRides = totalRides;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }

    public String getUserType() {
        return role.equals("DRIVER") ? "driver" : "passenger";
    }

    public int getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(int totalDistance) {
        this.totalDistance = totalDistance;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public double getTotalRatingSum() {
        return totalRatingSum;
    }

    public void setTotalRatingSum(double totalRatingSum) {
        this.totalRatingSum = totalRatingSum;
    }

    public double getTotalMoneySpent() {
        return totalMoneySpent;
    }

    public void setTotalMoneySpent(double totalMoneySpent) {
        this.totalMoneySpent = totalMoneySpent;
    }

    public double getTotalMoneyEarned() {
        return totalMoneyEarned;
    }

    public void setTotalMoneyEarned(double totalMoneyEarned) {
        this.totalMoneyEarned = totalMoneyEarned;
    }

    // Utility methods for updating statistics
    public void updateStatisticsAfterRide(int newRating, int rideDistance) {
        // Increment total rides
        this.totalRides++;
        
        // Add distance
        this.totalDistance += rideDistance;
        
        // Update rating if provided
        if (newRating > 0) {
            this.ratingCount++;
            this.totalRatingSum += newRating;
            this.rating = this.totalRatingSum / this.ratingCount;
        }
    }

    public void updateRatingOnly(int newRating) {
        if (newRating > 0) {
            this.ratingCount++;
            this.totalRatingSum += newRating;
            this.rating = this.totalRatingSum / this.ratingCount;
        }
    }

    public void incrementRideCount() {
        this.totalRides++;
    }

    public void addDistance(int distance) {
        this.totalDistance += distance;
    }

    public void addMoneySpent(double amount) {
        this.totalMoneySpent += amount;
    }

    public void addMoneyEarned(double amount) {
        this.totalMoneyEarned += amount;
    }
}
