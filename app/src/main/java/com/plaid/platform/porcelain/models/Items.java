package com.plaid.platform.porcelain.models;

public class Items {

    /**
     * NOTE changed id to use long rather than int
     */

    private long id;
    private byte[] image ;
    private String name;
    private String type;

    public Items(String name, String type, byte[] image){

        this.name=name;
        this.type=type;
        this.image=image;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeName() {
        return type;
    }

    public void setTypeName(String type) {
        this.type = type;
    }
}