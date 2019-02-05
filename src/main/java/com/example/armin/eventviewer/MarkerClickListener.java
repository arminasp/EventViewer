package com.example.armin.eventviewer;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState.EXPANDED;

class MarkerClickListener implements GoogleMap.OnMarkerClickListener {
    private MapsActivity activity;

    MarkerClickListener(MapsActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Event event = activity.getEventMap().get(marker);
        SlidingUpPanelLayout layout = (SlidingUpPanelLayout) activity.findViewById(R.id.sliding_layout);

        if (event != null && (activity.getCurrentEvent() != event)) {
            if (activity.getCurrentEvent() == null) {
                activity.fillEvent(event);
            } else {
                Event primaryEvent = activity.getCurrentEvent().getPrimaryEvent();
                if (primaryEvent != event) {
                    activity.fillEvent(event);
                }
            }
        }

        layout.setPanelState(EXPANDED);
        return false;
    }
}
