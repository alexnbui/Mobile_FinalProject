package com.example.trade_up;

public class Item {
    public String id;
    public String title;
    public String description;
    public String imageUrl;
    public String sellerId;
    public double price;
    public Item(String id, String title, String description, String imageUrl, String sellerId, double price) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sellerId = sellerId;
        this.price = price;
    }
    public Item() {}
}
