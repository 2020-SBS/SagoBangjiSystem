package com.example.samplesbs.data_model;

public class UserSituationData {
    float distance;
    long time;
    float speed;
    public UserSituationData(){}

    public UserSituationData(float distance, long time, float speed) {
        this.distance = distance;
        this.time = time;
        this.speed = speed;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}
