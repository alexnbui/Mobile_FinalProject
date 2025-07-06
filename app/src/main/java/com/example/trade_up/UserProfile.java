package com.example.trade_up;

public class UserProfile {
    private String uid;
    private String displayName;
    private String bio;
    private String contactInfo;
    private String photoUrl;
    private double rating;
    private int totalTransactions;

    public UserProfile() {}

    public UserProfile(String uid, String displayName, String bio, String contactInfo, String photoUrl, double rating, int totalTransactions) {
        this.uid = uid;
        this.displayName = displayName;
        this.bio = bio;
        this.contactInfo = contactInfo;
        this.photoUrl = photoUrl;
        this.rating = rating;
        this.totalTransactions = totalTransactions;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
}

