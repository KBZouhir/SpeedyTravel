package com.example.speedytravel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    ProgressDialog pd;
    MapView map = null;
    IMapController mapController;
    GeoPoint myStartPoint, destinationPoint;
    Marker myMarker, startMarker, destinationMarker;
    double latitude;
    double longitude;
    String provider;
    Context ctx;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ctx = getApplicationContext();

        if (!CheckPermissions()) {
            RequestPermissions();
        }

        map = (MapView) findViewById(R.id.map);
        ImageButton goToMyLocalisation = findViewById(R.id.goToMyLocalisation);
        final ImageButton goToMarker = findViewById(R.id.goToMarker);


//        ==================
        final ImageButton zoomin = findViewById(R.id.zoomin);
        final ImageButton zoomout = findViewById(R.id.zoomout);
        zoomin.setOnClickListener(e -> {
            if (map.getZoomLevelDouble() == map.getMaxZoomLevel()) {
                zoomin.setBackground(getResources().getDrawable(R.drawable.bg_circle_dark));
            } else {
                mapController.setZoom(map.getZoomLevelDouble() + 1);
                zoomout.setBackground(getResources().getDrawable(R.drawable.bg_circle));
            }
        });
        zoomout.setOnClickListener(e -> {
            if (map.getZoomLevelDouble() == map.getMinZoomLevel()) {
                zoomout.setBackground(getResources().getDrawable(R.drawable.bg_circle_dark));
            } else {
                mapController.setZoom(map.getZoomLevelDouble() - 1);
                zoomin.setBackground(getResources().getDrawable(R.drawable.bg_circle));
            }
        });
//          ============
        mapController = map.getController();
        mapController.setZoom(10.0);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setMinZoomLevel(13.0);
        map.setMaxZoomLevel(19.0);
        BoundingBox boundingBox = new BoundingBox(36.584819, 6.897789, 36.151927, 6.516044);
        map.setScrollableAreaLimitDouble(boundingBox);
        locationMethode(ctx);

        goToMyLocalisation.setOnClickListener(e -> {
            AnimateBotton(goToMyLocalisation);
            myStartPoint = new GeoPoint(latitude, longitude);
            mapController.setCenter(myStartPoint);
            startMarker.setPosition(myStartPoint);
            mapController.setZoom(17.0);
        });

        goToMarker.setOnClickListener(e -> {
            AnimateBotton(goToMarker);
            if (null != destinationPoint && null != myStartPoint) {
                GetRoute(myStartPoint, destinationPoint);

            }
        });


    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void locationMethode(Context ctx) {

        LocationManager locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (provider != null) {
            Criteria cr = new Criteria();
            cr.setAccuracy(Criteria.ACCURACY_FINE); // précision
            cr.setAltitudeRequired(true); // altitude
            cr.setBearingRequired(true); // direction
            cr.setCostAllowed(false); // payant/gratuit
            cr.setPowerRequirement(Criteria.POWER_HIGH);  // consommation
            cr.setSpeedRequired(true);  // vitesse
            provider = locationManager.getBestProvider(cr, false);

        } else provider = LocationManager.GPS_PROVIDER;


        startMarker = new Marker(map);
        destinationMarker = new Marker(map);
        startMarker.setIcon(getResources().getDrawable(R.drawable.ic_twotone_my_location_24));
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        destinationMarker.setIcon(getResources().getDrawable(R.drawable.ic_twotone_emoji_flags_24));
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        map.getOverlays().add(startMarker);
        map.getOverlays().add(destinationMarker);


        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
        if (lastKnownLocation != null) {
            latitude = lastKnownLocation.getLatitude();
            longitude = lastKnownLocation.getLongitude();
            //altitude = lastKnownLocation.getAltitude();
            GeoPoint lastGeoPoint = new GeoPoint(latitude, longitude);
//            mapController.setCenter(lastGeoPoint);
            startMarker.setPosition(lastGeoPoint);
        }


        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                //            altitude = location.getAltitude();
                myStartPoint = new GeoPoint(latitude, longitude);
                mapController.setCenter(myStartPoint);
                startMarker.setPosition(myStartPoint);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

        };
        locationManager.requestLocationUpdates(provider, 10000, 100, locationListener);
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                Toast.makeText(ctx, "Click a log to confirm your destination !!", Toast.LENGTH_SHORT).show();

                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                destinationPoint = p;
                map.invalidate();
                destinationMarker.setPosition(p);
                Toast.makeText(ctx, "Your destination is ready!", Toast.LENGTH_SHORT).show();

                return false;
            }
        };


        MapEventsOverlay OverlayEvents = new MapEventsOverlay(getBaseContext(), mReceive);
        map.getOverlays().add(OverlayEvents);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void drawMarker(GeoPoint point, int resourceId, String title) {
        myMarker = new Marker(map);
        myMarker.setIcon(getResources().getDrawable(resourceId));
        myMarker.setPosition(point);
        myMarker.setTitle(title);
        map.getOverlays().add(myMarker);
    }

    public List<GeoPoint> getPathFromJson(String json) {
        try {
            List<GeoPoint> path = new ArrayList<GeoPoint>();
            JSONObject jsonObj = new JSONObject(json);
            JSONArray pathJsonArray = jsonObj.getJSONObject("data").getJSONArray("path");
            for (int i = 0; i < pathJsonArray.length(); i++) {
                String name = pathJsonArray.getJSONObject(i).getString("name");

                String lon = pathJsonArray.getJSONObject(i).getString("lon");
                String lat = pathJsonArray.getJSONObject(i).getString("lat");
                GeoPoint point = new GeoPoint(Double.parseDouble(lat), Double.parseDouble(lon));
                if (i == 0) {
                    mapController.setCenter(point);
                }
                drawMarker(point, R.drawable.ic_twotone_bus_alert_24, name);
                path.add(point);
            }
            return path;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void AnimateBotton(ImageButton v) {
        Animator scale = ObjectAnimator.ofPropertyValuesHolder(v,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1, 1.1f, 1),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1, 1.1f, 1)
        );
        scale.setDuration(600);
        scale.start();
    }

    private void progress() {
        pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("loading");
        pd.show();
    }


    public void GetRoute(GeoPoint myStartPoint, GeoPoint destinationPoint) {
        progress();
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        String url = "https://a-star-pfe.mogh-apps.com/api/direction/" + myStartPoint.getLatitude() + "," + myStartPoint.getLongitude() + "/" + destinationPoint.getLatitude() + "," + destinationPoint.getLongitude() + "";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    String str = response;
                    List<GeoPoint> pathPoint = new ArrayList<>();

                    pathPoint = getPathFromJson(str);
                    Polyline myPath = new Polyline(map);
                    myPath.setPoints(pathPoint);
                    myPath.setColor(Color.CYAN);

                    map.getOverlayManager().add(myPath);
                    map.invalidate();

                    pd.hide();
                }, error -> {
                    pd.hide();
                    Toast.makeText(MainActivity.this, "did not work", Toast.LENGTH_SHORT).show();
        });

        queue.add(stringRequest);

    }


    public boolean CheckPermissions() {
        int result = ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
        int result1 = ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, WRITE_EXTERNAL_STORAGE}, 1);
    }


}