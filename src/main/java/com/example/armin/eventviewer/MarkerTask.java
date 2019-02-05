package com.example.armin.eventviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

class MarkerTask extends AsyncTask<String, Void, String> {
    private MapsActivity activity;
    private JSONObject jsonObject;
    private HashMap<MarkerOptions, Event> markerMap;
    private Event event = null;

    MarkerTask(MapsActivity activity, JSONObject jsonObject) {
        this.activity = activity;
        this.jsonObject = jsonObject;
        markerMap = new HashMap<>();
    }

    @Override
    protected String doInBackground(String... params) {
        for (Object obj : (JSONArray) jsonObject.get("events")) {
            JSONObject jObj = (JSONObject) obj;

            if (jObj != null) {
                String id = null;
                String name = null;
                String eventCoverUrl = null;
                String description = null;
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
                Date startTime = null;
                Date endTime = null;
                Long attendingCount = null;

                if (jObj.containsKey("id")) {
                    id = (String) jObj.get("id");
                }

                if (jObj.containsKey("name")) {
                    name = (String) jObj.get("name");
                }

                if (jObj.containsKey("description")) {
                    description = (String) jObj.get("description");
                }

                if (jObj.containsKey("coverPicture")) {
                    eventCoverUrl = (String) jObj.get("coverPicture");
                }

                if (jObj.containsKey("startTime")) {
                    try {
                        startTime = dateFormat.parse((String) jObj.get("startTime"));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if (jObj.containsKey("endTime")) {
                    try {
                        endTime = dateFormat.parse((String) jObj.get("startTime"));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if (jObj.containsKey("stats")) {
                    JSONObject stats = (JSONObject) jObj.get("stats");

                    if (stats.containsKey("attending")) {
                        attendingCount = (Long) stats.get("attending");
                    }
                }

                if (jObj.containsKey("venue")) {
                    JSONObject venue = (JSONObject) jObj.get("venue");
                    String profilePictureUrl = null;

                    if (venue.containsKey("profilePicture")) {
                        profilePictureUrl = (String) venue.get("profilePicture");
                    }

                    if (venue.containsKey("location")) {
                        JSONObject location = (JSONObject) venue.get("location");
                        Double latitude = null;
                        Double longitude = null;

                        if (location.containsKey("latitude")) {
                            latitude = (Double) location.get("latitude");
                        }

                        if (location.containsKey("longitude")) {
                            longitude = (Double) location.get("longitude");
                        }

                        if (latitude != null && longitude != null && profilePictureUrl != null) {
                            event = new Event();
                            Bitmap bmp = null;

                            event.setLatitude(latitude);
                            event.setLongitude(longitude);

                            if (id != null) {
                                event.setId(id);
                            }

                            if (name != null) {
                                event.setName(name);
                            }

                            if (eventCoverUrl != null) {
                                event.setEventPictureUrl(eventCoverUrl);
                            }

                            if (description != null) {
                                event.setDescription(description);
                            }

                            if (startTime != null) {
                                event.setStartTime(startTime);
                            }

                            if (endTime != null) {
                                event.setEndTime(endTime);
                            }

                            if (attendingCount != null) {
                                event.setAttendingCount(attendingCount);
                            }

                            Event conEvent = getConnectedEvent(event);

                            if (conEvent == null) {
                                // add marker icon
                                try {
                                    URL url = new URL(profilePictureUrl);
                                    bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                    event.setProfilePicture(bmp);
                                    bmp = getMarkerBitmapFromView(bmp);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                // add marker
                                MarkerOptions markerOptions = new MarkerOptions()
                                        .position(new LatLng(latitude, longitude))
                                        .icon(BitmapDescriptorFactory.fromBitmap(bmp));

                                markerMap.put(markerOptions, event);
                            } else {
                                conEvent.addConnectedEvent(event);
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    private Event getConnectedEvent(Event e) {
        for (Object o : markerMap.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            Event event = (Event) pair.getValue();

            if ((event.getLatitude() == e.getLatitude()) &&
                    (event.getLongitude() == e.getLongitude())) {
                return event;
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        HashMap<Marker, Event> eventMap = activity.getEventMap();
        Iterator it = markerMap.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            if (event != null && activity.isConnected()) {
                Marker m = activity.getmMap().addMarker((MarkerOptions) pair.getKey());
                eventMap.put(m, (Event) pair.getValue());
            }
            it.remove();
        }
        activity.setMarkersFilled(true);
    }

    private Bitmap getMarkerBitmapFromView(Bitmap b) {
        @SuppressLint("InflateParams") View customMarkerView = ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.view_custom_marker, null);
        ImageView markerImageView = (ImageView) customMarkerView.findViewById(R.id.profile_image);
        markerImageView.setImageBitmap(b);
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
        customMarkerView.buildDrawingCache();
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);
        Drawable drawable = customMarkerView.getBackground();
        if (drawable != null)
            drawable.draw(canvas);
        customMarkerView.draw(canvas);
        return returnedBitmap;
    }
}