package com.example.armin.eventviewer;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Date;

class Event {
    private String id;
    private String name;
    private String eventPictureUrl;
    private String description;
    private Date startTime;
    private Date endTime;
    private Bitmap profilePicture;
    private Bitmap eventPicture;
    private ArrayList<Event> connectedEventList;
    private double latitude;
    private double longitude;
    private Long attendingCount;
    private Event primaryEvent;

    Event() {
        connectedEventList = new ArrayList<>();
    }

    void addConnectedEvent(Event e) {
        connectedEventList.add(e);
    }

    void downloadEventPicture(ImageView imageView) {
        new ImageDownloadTask(eventPictureUrl, this, imageView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    Bitmap getProfilePicture() {
        return profilePicture;
    }

    void setProfilePicture(Bitmap profilePicture) {
        this.profilePicture = profilePicture;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    void setEventPictureUrl(String eventPictureUrl) {
        this.eventPictureUrl = eventPictureUrl;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    Date getStartTime() {
        return startTime;
    }

    void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    Bitmap getEventPicture() {
        return eventPicture;
    }

    void setEventPicture(Bitmap eventPicture) {
        this.eventPicture = eventPicture;
    }

    ArrayList<Event> getConnectedEventList() {
        return connectedEventList;
    }

    double getLatitude() {
        return latitude;
    }

    void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    double getLongitude() {
        return longitude;
    }

    void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    Event getPrimaryEvent() {
        return primaryEvent;
    }

    void setPrimaryEvent(Event primaryEvent) {
        this.primaryEvent = primaryEvent;
    }

    Long getAttendingCount() {
        return attendingCount;
    }

    void setAttendingCount(Long attendingCount) {
        this.attendingCount = attendingCount;
    }

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }
}
