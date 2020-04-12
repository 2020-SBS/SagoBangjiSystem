package com.example.samplesbs.data_model;

public class LocationData {
    private double latitude;
    private double longitude;
    private double angle;

    public LocationData(){};

    public LocationData(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.angle = 0.0;
    }
    public LocationData( double latitude, double longitude, double angle) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.angle = angle;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
