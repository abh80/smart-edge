package com.abh80.smartedge.plugins.MediaSession;

import android.graphics.Bitmap;

public class UpdateQueueStruct {
    private String artist;
    private String title;
    private Bitmap cover;

    public UpdateQueueStruct(String artist, String title, Bitmap cover) {
        this.artist = artist;
        this.title = title;
        this.cover = cover;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Bitmap getCover() {
        return cover;
    }

    public void setCover(Bitmap cover) {
        this.cover = cover;
    }
}
