package com.huaxia.hpn.hpnapp;

import android.Manifest;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.huaxia.hpn.utils.IpMacUtils;
import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.Constants;
import com.mapbox.services.android.geocoder.ui.GeocoderAutoCompleteView;
import com.mapbox.services.commons.ServicesException;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.directions.v5.DirectionsCriteria;
import com.mapbox.services.directions.v5.MapboxDirections;
import com.mapbox.services.directions.v5.models.DirectionsResponse;
import com.mapbox.services.directions.v5.models.DirectionsRoute;
import com.mapbox.services.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.geocoding.v5.models.CarmenFeature;

import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity2 extends AppCompatActivity {
    private static final String TAG = "AppCompatActivity";
    private static final int PERMISSIONS_LOCATION = 0;

    private MapView mapView;
    private MapboxMap map;
    private FloatingActionButton floatingActionButton;
    private LocationServices locationServices;
    private DirectionsRoute currentRoute;

    private String result;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapboxAccountManager.start(this, getString(R.string.access_Token));
        setContentView(R.layout.activity_main2);
        locationServices = LocationServices.getLocationServices(MainActivity2.this);
        try{
            // 开启一个子线程，进行网络操作，等待有返回结果，使用handler通知UI
            new Thread(getThread).start();
        }catch (Exception e){
            e.printStackTrace();
        }

        // Set up the MapView
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                //toggleGps(!map.isMyLocationEnabled());
                longclick(map);
            }
        });
        String mac = IpMacUtils.getMacFromWifi();
        Log.i(TAG, "123");
        floatingActionButton = (FloatingActionButton) findViewById(R.id.location_toggle_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null) {
                    toggleGps(!map.isMyLocationEnabled());
                }
            }
        });

        // Set up autocomplete widget
        GeocoderAutoCompleteView autocomplete = (GeocoderAutoCompleteView) findViewById(R.id.query);
        autocomplete.setAccessToken(MapboxAccountManager.getInstance().getAccessToken());
        autocomplete.setType(GeocodingCriteria.TYPE_POI);
        autocomplete.setOnFeatureListener(new GeocoderAutoCompleteView.OnFeatureListener() {
            @Override
            public void OnFeatureClick(CarmenFeature feature) {
                Position position = feature.asPosition();
                updateMap(position.getLatitude(), position.getLongitude());
            }
        });

    }

    private void updateMap(double latitude, double longitude) {
        // Build marker
        map.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title("Geocoder result"));
        //跳转目的地界面
        // Animate camera to geocoder result location
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(15)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);
    }

    private void longclick(final MapboxMap mapboxMap) {
        mapboxMap.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {

                //删除所有之前的标记
                mapboxMap.removeAnnotations();

                // Set the origin waypoint to the devices location设置初始位置
                Position origin = Position.fromCoordinates(mapboxMap.getMyLocation().getLongitude(), mapboxMap.getMyLocation().getLatitude());

                // 设置目的地路径--点击的位置点
                Position destination = Position.fromCoordinates(point.getLongitude(), point.getLatitude());

                // Add marker to the destination waypoint
                mapboxMap.addMarker(new MarkerOptions()
                        .position(new LatLng(point))
                        .title("目的地")
                        .snippet("My destination"));

                // Get route from API
                try {
                    getRoute(origin, destination);
                } catch (ServicesException servicesException) {
                    servicesException.printStackTrace();
                }
            }
        });
    }

    private void getRoute(Position origin, Position destination) throws ServicesException{
        MapboxDirections directions = new MapboxDirections.Builder()
                .setAccessToken(getString(R.string.access_Token))
                .setOrigin(origin)
                .setDestination(destination)
                .setProfile(DirectionsCriteria.PROFILE_WALKING)
                .build();

        directions.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                // You can get the generic HTTP info about the response
                Log.d(TAG, "Response code: " + response.code());
                if (response.body() == null) {
                    Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().getRoutes().size() < 1) {
                    Log.e(TAG, "No routes found");
                    return;
                }

                // Print some info about the route
                currentRoute = response.body().getRoutes().get(0);
                Log.d(TAG, "Distance: " + currentRoute.getDistance());
                showToastMessage("You are %d meters from your destination");

                // Draw the route on the map
                drawRoute(currentRoute);
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Log.e(TAG, "Error: " + throwable.getMessage());
                showToastMessage("Error: " + throwable.getMessage());
            }
        });
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void drawRoute(DirectionsRoute route) {
        // Convert LineString coordinates into LatLng[]
        LineString lineString = LineString.fromPolyline(route.getGeometry(), Constants.OSRM_PRECISION_V5);
        List<Position> coordinates = lineString.getCoordinates();
        LatLng[] points = new LatLng[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            points[i] = new LatLng(
                    coordinates.get(i).getLatitude(),
                    coordinates.get(i).getLongitude());
        }

        // Draw Points on MapView
        map.addPolyline(new PolylineOptions()
                .add(points)
                .color(Color.parseColor("#009688"))
                .width(5));
    }

    private void toggleGps(boolean enableGps) {
        if (enableGps) {
            // Check if user has granted location permission
            if (!locationServices.areLocationPermissionsGranted()) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            } else {
                enableLocation(true);
            }
        } else {
            enableLocation(false);
        }
    }

    private void enableLocation(boolean enabled) {
        if (enabled) {
            // If we have the last location of the user, we can move the camera to that position.
            Location lastLocation = locationServices.getLastLocation();
            if (lastLocation != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16));
            }

            locationServices.addLocationListener(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16));
                        locationServices.removeLocationListener(this);
                    }
                }
            });
            floatingActionButton.setImageResource(R.drawable.ic_location_disabled_24dp);
        } else {
            floatingActionButton.setImageResource(R.drawable.ic_my_location_24dp);
        }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private Thread getThread = new Thread(){
        public void run() {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("http://10.0.2.2:8081/hpn-new/app/hpn/spotData!obtainCollectionses.do");
                connection = (HttpURLConnection) url.openConnection();
                // 设置请求方法，默认是GET
                connection.setRequestMethod("POST");
                // 设置字符集
                connection.setRequestProperty("Charset", "UTF-8");
                // 设置文件类型
                connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
                // 设置请求参数，可通过Servlet的getHeader()获取
                connection.setRequestProperty("Cookie", "AppName=" + URLEncoder.encode("你好", "UTF-8"));
                // 设置自定义参数
                connection.setRequestProperty("MyProperty", "this is me!");
                OutputStream outputStream = connection.getOutputStream();
                JSONObject maps = new JSONObject();
                maps.put("MACCode", "08:00:20:0A:8C:6D");
                maps.put("latitude", "-0.008065");
                maps.put("longitude", "112.520855");
                maps.put("azimuth", "0");
                maps.put("operater", "admin");
                String params = "data.MACCode=08:00:20:0A:8C:6D&data.latitude=-0.008065&data.longitude=112.520855&data.azimuth=0&data.operater=admin";
                JSONObject paramsJ = new JSONObject();
                paramsJ.put("data",maps);
                // 注意编码格式
                outputStream.write(maps.toString().getBytes());
                outputStream.close();

                if(connection.getResponseCode() == 200){
                    InputStream is = connection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(
                            is, "utf-8");
                    BufferedReader bufferedReader = new BufferedReader(
                            inputStreamReader);
                    String str = null;
                    StringBuffer buffer = new StringBuffer();
                    while ((str = bufferedReader.readLine()) != null) {
                        buffer.append(str);
                    }

                    // 释放资源
                    bufferedReader.close();
                    inputStreamReader.close();
                    is.close();
                    result = buffer.toString();

                    Message msg = Message.obtain();
                    msg.what = 0;
                    getHandler.sendMessage(msg);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        };
    };

    private Handler getHandler = new Handler(){
        public void handleMessage(Message msg) {
            if(msg.what == 0 && result!=null){
                Log.i(TAG, "return: " + result);;
            }
        };
    };
}

