package com.example.armin.eventviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.IOException;
import java.net.URL;

class ImageDownloadTask extends AsyncTask<String, Void, String> {
    private String imgUrl;
    private ImageView imageView;
    private Bitmap img;
    private Event event;

    ImageDownloadTask(String imgUrl, Event event, ImageView imageView) {
        this.imgUrl = imgUrl;
        this.imageView = imageView;
        this.event = event;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            URL url = new URL(imgUrl);
            img = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            event.setEventPicture(img);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        imageView.setImageBitmap(img);
    }
}
