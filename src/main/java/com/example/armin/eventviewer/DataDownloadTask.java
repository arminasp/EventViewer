package com.example.armin.eventviewer;

import android.os.AsyncTask;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class DataDownloadTask extends AsyncTask<String, Void, String> {
    private MapsActivity activity;

    DataDownloadTask(MapsActivity activity) {
        this.activity = activity;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            return downloadContent(params[0]);
        } catch (IOException e) {
            activity.setDownloadFailed(true);
            return "Unable to retrieve data. URL may be invalid.";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        JSONObject jsonObject;
        try {
            jsonObject = (JSONObject) new JSONParser().parse(result);

            if (jsonObject != null) {
                new MarkerTask(activity, jsonObject).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } catch (ParseException e) {
            activity.setDownloadFailed(true);
        }
    }

    private String downloadContent(String myUrl) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();

            return convertInputStreamToString(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private String convertInputStreamToString(InputStream stream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        String str = "";
        String line;
        while ((line = r.readLine()) != null) {
            str += line + '\n';
        }
        return str;
    }
}