package com.huaxia.hpn.hpnapp;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.esri.core.geometry.Point;
import com.huaxia.hpn.convert.ConvertGauss2Geodetic;
import com.huaxia.hpn.headerview.MyToolBar;
import com.huaxia.hpn.route.LineSegment;
import com.huaxia.hpn.route.RoutePlanning;
import com.huaxia.hpn.utils.AppUtils;
import com.huaxia.hpn.utils.HttpUtils;
import com.huaxia.hpn.utils.ImageUtils;
import com.huaxia.hpn.utils.IpMacUtils;
import com.huaxia.hpn.utils.SocketUtil;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationSource;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.functions.Function;
import com.mapbox.mapboxsdk.style.functions.stops.Stop;
import com.mapbox.mapboxsdk.style.functions.stops.Stops;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.Constants;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.services.api.ServicesException;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ips.casm.com.radiomap.MPoint;
import ips.casm.com.util.iMessage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static java.lang.Thread.sleep;

public class MapBoxActivity extends Activity implements PermissionsListener, SearchView.OnQueryTextListener {
    private static final String TAG = "Activity";
    private static final int PERMISSIONS_LOCATION = 0;
    private MapView mapView;
    private MapboxMap map;
    private FloatingActionButton floatingActionButton;// 定位按钮
    private FloatingActionButton floatingActionGuideButton;// 导览按钮
    private LocationEngine locationEngine;
    private LocationEngineListener locationEngineListener;
    private PermissionsManager permissionsManager;
    private DirectionsRoute currentRoute;
    private Point mPoint;
    private Intent startIpsServiceIntent;
    private String rmFilePathStr = null;
    private BroadcastReceiver mIPSPointReceiver;
    private IntentFilter ipsPointRecerverIntentFilter;
    //    private PictureMarkerSymbol mPointMarkerSymbol;
//    private GraphicsLayer graphicsLayer = null;
//    private Drawable maker_img_locate,newmarker;//定位指向标志
    private Map pointMap = new HashMap();

    private String result;

    private View levelButtons;
    private GeoJsonSource indoorRouteSource;

    private MyToolBar myToolBar;// 自定义toolbar
    private JSONArray imageArray;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private MapBoxActivity.SendPointAndAzimuthTask mSendTask = null;

    // 导航flg
    private boolean naviFlg=false;
    // 导览flg
    private boolean guideFlg;
    // 定位flg
    private boolean locationFlg;

    private ValueAnimator hotelColorAnimator;

    private Map<String, org.json.JSONObject > imageMap;

    private SearchView mSearchView;

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        MapboxAccountManager.start(this, getString(R.string.access_Token));
        Mapbox.getInstance(this, getString(R.string.access_Token));
        setContentView(R.layout.activity_mapbox);
        // 初始化视图
        initView();
//        locationServices = LocationServices.getLocationServices(MapBoxActivity.this);
        // Get the location engine object for later use.
        locationEngine = LocationSource.getLocationEngine(this);
        locationEngine.activate();

        // Set up the MapView
        mapView = (MapView) findViewById(R.id.mapIndoorView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;

                longclick(map);
                for (Marker maker : map.getMarkers()) {
                    map.removeMarker(maker);
                }
//                map.addMarker(new MarkerOptions().position(new LatLng(39.947635, 116.420298)));

                // 楼层按钮
                levelButtons = findViewById(R.id.floor_level_buttons);
                AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
                animation.setDuration(500);
                levelButtons.startAnimation(animation);
                levelButtons.setVisibility(View.VISIBLE);
                String routesGeojson = loadJsonFromAsset("routes.geojson");
                RoutePlanning.init(routesGeojson);
                indoorRouteSource = new GeoJsonSource("indoor-building", routesGeojson);
//                indoorRouteSource = new GeoJsonSource("indoor-building", loadJsonFromAsset("routes.geojson"));
                mapboxMap.addSource(indoorRouteSource);
//
//                // Add the building layers since we know zoom levels in range
                loadBuildingLayer();
                Point point = (Point)pointMap.get("Point");
//                if (point != null){
//                    mapboxMap.addMarker(new MarkerOptions()
//                            .position(new LatLng(point.getY(), point.getX()))
//                            .title("http://www.baidu.com")
//                            .snippet("http://www.baidu.com")
//                    );
//                } else {
//                    mapboxMap.addMarker(new MarkerOptions()
//                            .position(new LatLng(39.947635, 116.420298))
//                            .title("http://www.baidu.com")
//                            .snippet("http://www.baidu.com"));
//                }
                mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener(){
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        IconFactory iconFactory = IconFactory.getInstance(MapBoxActivity.this);
                        Icon icon = iconFactory.fromResource(R.drawable.click_marker);
//                        Bitmap bitmap = marker.getIcon().getBitmap();
//                        Icon icon = iconFactory.fromBitmap(ImageUtils.createRGBImage(bitmap, R.color.blue));
//                        marker.setIcon(icon);
                        if(imageMap.get(marker.getTitle()) != null){
                            Intent intent = new Intent(MapBoxActivity.this, PhotoDetailActivity.class);
                            /* 通过Bundle对象存储需要传递的数据 */
                            Bundle bundle = new Bundle();
                            bundle.putString("image", imageMap.get(marker.getTitle()).toString());
                            intent.putExtras(bundle);
                            startActivity(intent);
                        } else {
                            Toast.makeText(getApplicationContext(), "没找到关联资源!", Toast.LENGTH_SHORT).show();
                        }
                        return false;
                    }
                });
//                setLayerVisible("indoor-building");
                // Add the hotels source to the map
                GeoJsonSource hotelSource = new GeoJsonSource("hotels", loadJsonFromAsset("la_hotels.geojson"));
                mapboxMap.addSource(hotelSource);

                FillLayer hotelLayer = new FillLayer("hotels", "hotels").withProperties(
                        fillColor(Color.parseColor("#5a9fcf")),
                        PropertyFactory.visibility(Property.NONE)
                );

                mapboxMap.addLayer(hotelLayer);

                final FillLayer hotels = (FillLayer) mapboxMap.getLayer("hotels");

                hotelColorAnimator = ValueAnimator.ofObject(
                        new ArgbEvaluator(),
                        Color.parseColor("#5a9fcf"), // Brighter shade
                        Color.parseColor("#2C6B97") // Darker shade
                );
                hotelColorAnimator.setDuration(1000);
                hotelColorAnimator.setRepeatCount(ValueAnimator.INFINITE);
                hotelColorAnimator.setRepeatMode(ValueAnimator.REVERSE);
                hotelColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {

                        hotels.setProperties(
                                fillColor((int) animator.getAnimatedValue())
                        );
                    }

                });
                com.getbase.floatingactionbutton.FloatingActionButton toggleHotelsFab = (com.getbase.floatingactionbutton.FloatingActionButton) findViewById(R.id.fab_toggle_hotels);
                toggleHotelsFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setLayerVisible("hotels");
                    }
                });
            }
        });

        // 取得经纬度和方位角
        getPointAndAzimuth();

        //定位
        floatingActionButton = (FloatingActionButton) findViewById(R.id.location_toggle_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null) {
//                    toggleGps(!map.isMyLocationEnabled());
                    toggleGps(!locationFlg);
                }
            }
        });

        //导览
        floatingActionGuideButton = (FloatingActionButton) findViewById(R.id.location_guide_fab);
        floatingActionGuideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null) {
                    // 发送当前位置和方位角给后台
                    sendPointAndAzimuth();
                }
            }
        });
        // Set up autocomplete widget
//        GeocoderAutoCompleteView autocomplete = (GeocoderAutoCompleteView) findViewById(R.id.query);
//        autocomplete.setAccessToken(Mapbox.getAccessToken());
//        autocomplete.setType(GeocodingCriteria.TYPE_POI);
//        autocomplete.setOnFeatureListener(new GeocoderAutoCompleteView.OnFeatureListener() {
//            @Override
//            public void onFeatureClick(CarmenFeature feature) {
//                Position position = feature.asPosition();
//                updateMap(position.getLatitude(), position.getLongitude());
//            }
//        });

        Button buttonSecondLevel = (Button) findViewById(R.id.second_level_button);
        buttonSecondLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                indoorBuildingSource.setGeoJson(loadJsonFromAsset("white_house_lvl_1.geojson"));
                map.setStyleUrl("mapbox://styles/maper/ciwvpz28c002z2qpqxdg2m5cy");
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(-0.008069, 112.520855))
                        .zoom(10)
                        .build();
                map.setCameraPosition(cameraPosition);
                // Customize Map with markers, polylines, etc.
            }
        });

        Button buttonGroundLevel = (Button) findViewById(R.id.ground_level_button);
        buttonGroundLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                indoorBuildingSource.setGeoJson(loadJsonFromAsset("white_house_lvl_0.geojson"));
                map.setStyleUrl("mapbox://styles/maper/cizfl4jyx007m2sji1ndyc4nl");
                // Customize Map with markers, polylines, etc.
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(39.947635, 116.420298))
                        .zoom(20.5)
                        .build();
                map.setCameraPosition(cameraPosition);
//                // Add the building layers since we know zoom levels in range
//                indoorRouteSource.setGeoJson(loadJsonFromAsset("routes.geojson"));
                map.getLayers();
                map.removeLayer("indoor-building-fill");
                map.removeLayer("indoor-building-line");
//                setLayerVisible("indoor-building-fill");
//                setLayerVisible("indoor-building-line");
                loadBuildingLayer();

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

//                String routesGeojson = loadJsonFromAsset("routes.geojson");
//                RoutePlanning.init(routesGeojson);

                //删除所有之前的标记
                mapboxMap.removeAnnotations();

                // Set the origin waypoint to the devices location设置初始位置
//                Position origin = Position.fromCoordinates(mapboxMap.getMyLocation().getLongitude(), mapboxMap.getMyLocation().getLatitude());
                Point pointBegin = (Point)pointMap.get("Point");
                if(pointBegin==null|| Math.abs(pointBegin.getX())<0.1||Math.abs(pointBegin.getY())<0.1){
                    pointBegin =  new Point(116.420298,39.947635);
                }
                Position origin = Position.fromCoordinates(pointBegin.getX(),pointBegin.getY());

                // 设置目的地路径--点击的位置点
                Position destination = Position.fromCoordinates(point.getLongitude(), point.getLatitude());
                Log.e("longclick: ",pointBegin.toString() + " -- " + destination.toString());
                // Add marker to the destination waypoint
                mapboxMap.addMarker(new MarkerOptions()
                        .position(new LatLng(point))
                        .title("目的地")
                        .snippet("My destination"));

                // Get route from API
                try {
//                    getRoute(origin, destination);
                    drawRoute(origin, destination);
                } catch (ServicesException servicesException) {
                    servicesException.printStackTrace();
                }
            }
        });
    }

    private void getRoute(Position origin, Position destination) throws ServicesException {
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
                    Log.e(TAG, "无法找到路线, 请确认您的相关权限设置和访问令牌.");
                    return;
                } else if (response.body().getRoutes().size() < 1) {
                    Log.e(TAG, "无法找到路线");
                    return;
                }

                // Print some info about the route
                currentRoute = response.body().getRoutes().get(0);
                Log.d(TAG, "Distance: " + currentRoute.getDistance());
//                showToastMessage("You are %d meters from your destination");
                Toast.makeText(
                        MapBoxActivity.this,
                        "目的地距离您 " + currentRoute.getDistance() + "米远.",
                        Toast.LENGTH_SHORT).show();
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

    private  void drawRoute(Position origin, Position destination){
        List<Position> routePosition = new RoutePlanning().getRoutePlanning(origin,destination);
        LatLng[] points = new LatLng[routePosition.size()];
        for (int i = 0; i < routePosition.size(); i++) {
            points[i] = new LatLng(
                    routePosition.get(i).getLatitude(),
                    routePosition.get(i).getLongitude());
        }
        // Draw Points on MapView
        map.addPolyline(new PolylineOptions()
                .add(points)
                .color(Color.parseColor("#009688"))
                .width(5));
        naviFlg =true;
    }

    private void toggleGps(boolean enableGps) {
        if (enableGps) {
            // Check if user has granted location permission
            permissionsManager = new PermissionsManager(this);
            if (!PermissionsManager.areLocationPermissionsGranted(this)) {
                permissionsManager.requestLocationPermissions(this);
            } else {
                enableLocation(true);
                locationFlg = true;
            }
        } else {
            locationFlg = false;
            //删除所有之前的标记
            map.removeAnnotations();
            enableLocation(false);
        }
    }

    private void enableLocation(boolean enabled) {
        if (enabled) {
            // If we have the last location of the user, we can move the camera to that position.
            Location lastLocation = locationEngine.getLastLocation();
            if (lastLocation != null) {
//                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16));
                Point point = (Point)pointMap.get("Point");
                if(point!=null){
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(point.getY(),point.getX()), 22.5));
                }
            }

            locationEngineListener = new LocationEngineListener() {
                @Override
                public void onConnected() {
//                    locationEngine.requestLocationUpdates();
                }

                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
//                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16));
                        Point point = (Point)pointMap.get("Point");
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(point.getY(),point.getX()), 22.5));
                        locationEngine.removeLocationEngineListener(this);
                    }
                }
            };
            locationEngine.addLocationEngineListener(locationEngineListener);
            floatingActionButton.setImageResource(R.drawable.ic_location_disabled_24dp);
        } else {
            floatingActionButton.setImageResource(R.drawable.ic_my_location_24dp);
        }
        // Enable or disable the location layer on the map
//        map.setMyLocationEnabled(enabled);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "This app needs location permissions in order to show its functionality.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocation(true);
        } else {
            Toast.makeText(this, "You didn't grant location permissions.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void getPointAndAzimuth() {
        //取得指纹数据
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String SystemDirPath = Environment.getExternalStorageDirectory().toString();
            String IPSFolderPath = SystemDirPath + File.separator + "IndoorPositionSystem";
            String RadioMapPath = IPSFolderPath + File.separator + "RadioMap";
            String rmfilePath = RadioMapPath + File.separator + "Dice_Radio_Map.txt";
//            String rmfilePath = getClass().getResource("Dice_Radio_Map.txt").getPath(); //getResources().getAssets().toString() + "file:///android_asset/Dice_Radio_Map.txt";
            //魅族手机获取的是相对路径。
            File rmFile = new File(rmfilePath);
            if (!rmFile.exists()) {
                Toast.makeText(MapBoxActivity.this, "没有找到该指纹库文件", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                rmFilePathStr = rmfilePath;
                Log.i("IPS_DCActivity", "获得的文件路径为：" + rmfilePath);
            }
        }
        startIpsServiceIntent = new Intent(MapBoxActivity.this, ips.casm.com.service.MIPSService.class);
        Bundle ipsServiceBundle = new Bundle();
        ipsServiceBundle.putString(iMessage.IN_FILE_PATH, rmFilePathStr);
        startIpsServiceIntent.putExtras(ipsServiceBundle);
//        startIpsServiceIntent.putExtra(iMessage.IN_FILE_PATH, rmFilePathStr);
        bindService(startIpsServiceIntent, this.ipsServiceConnection, Context.BIND_AUTO_CREATE);

        mIPSPointReceiver = new IPSPointReceiver();
        ipsPointRecerverIntentFilter = new IntentFilter(iMessage.sendIPSPoint2ActivityBroadcastActionIntent);
        registerReceiver(mIPSPointReceiver, ipsPointRecerverIntentFilter);
    }


    private ServiceConnection ipsServiceConnection = new ServiceConnection() {
        /**
         * 获取service调用
         */
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
//            mIPSService=((MIPSService.MBinder)(service)).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
        }
    };

    private class IPSPointReceiver extends BroadcastReceiver {
        private MPoint lastPoint=new MPoint();
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("IPSPointReceiver", "调用onReceiver()方法");
            // TODO Auto-generated method stub
            //获得service回传的point
            MPoint point_Plane = (MPoint) intent.getExtras().get(iMessage.sendIPSPoint2ActivityBroadcastActionKey);
            if(Double.isNaN(point_Plane.x)){
                point_Plane.x = 0.0;
            };
            if(Double.isNaN(point_Plane.y)){
                point_Plane.y = 0.0;
            };
            Log.e("IPSPointReceiver", "接收到的坐标为：" + point_Plane.toString());
            ConvertGauss2Geodetic convertGauss2Geodetic = new ConvertGauss2Geodetic();
            MPoint cPoint = convertGauss2Geodetic.getLatLon(point_Plane.y,point_Plane.x);
            //创建geometry
            Point point = new Point(cPoint.x, cPoint.y);

            Log.e("IPSPointReceiver", cPoint.toString());
//			//创建投影坐标系的空间参考
//			SpatialReference cgcs2000NoneZone=SpatialReference.create(4548);
//			//获取当前地图的空间参考
//			SpatialReference cgcs2000Geodetic=mMapView.getSpatialReference();
            //投影反算,获得点位信息
//            mPoint=(Point) GeometryEngine.project(point, cgcs2000NoneZone, cgcs2000Geodetic);
            Log.e("IPSPointReceiver", "投影后坐标为：" + cPoint.x + "," + cPoint.y);
            //获得azimuth
            float azimuth = point_Plane.getAzimuth();
//            Point point2 = new Point(116.420298, 39.947635);

            pointMap.put("Point", point);
//            pointMap.put("Point", point2);
            pointMap.put("Azimuth", azimuth);

            if (point != null && map != null && locationFlg){
                // Create an Icon object for the marker to use
                IconFactory iconFactory = IconFactory.getInstance(MapBoxActivity.this);
                Icon icon = iconFactory.fromResource(R.drawable.navi_map_gps_locked);

                if(false){
                    Position nowPosition = Position.fromCoordinates(point.getX(),point.getY());
                    if(Math.abs(point_Plane.s-0)>0.1 ){
                        Position lastPosition = Position.fromCoordinates(lastPoint.x,lastPoint.y);
                        LineSegment  lineSegment =  RoutePlanning.getNearestLine(lastPosition);
                        Position start = lineSegment.getP1();
                        Position end = lineSegment.getP2();
                        double k = (end.getLongitude()-start.getLongitude())/(end.getLatitude()-start.getLatitude());
                        double deltaY=point_Plane.s*k/Math.sqrt(k*k+1);
                        double deltaX=Math.sqrt(point_Plane.s*point_Plane.s-deltaY*deltaY);
                        MPoint nowPoint=lastPoint;
                        if(end.getLongitude()-start.getLongitude()>0){
                            nowPoint.x +=deltaX;
                        }else{
                            nowPoint.x -=deltaX;
                        }
                        if(end.getLatitude()-start.getLatitude()>0){
                            nowPoint.y +=deltaY;
                        }else{
                            nowPoint.y -=deltaY;
                        }
                        Log.i("IPSPointReceiver", "位移坐标为：" + nowPoint.x + "," + nowPoint.y);
                        cPoint = convertGauss2Geodetic.getLatLon(nowPoint.y,nowPoint.x);
                        //创建geometry
                        point = new Point(cPoint.x, cPoint.y);
                        nowPosition = Position.fromCoordinates(point.getX(),point.getY());
                    }else{
                        lastPoint = point_Plane;
                    }
                    Position rp = RoutePlanning.getRouttingPosition(nowPosition);
                    point= new Point(rp.getLongitude(),rp.getLatitude());
                }else{
                    lastPoint = point_Plane;
                }
                // 只删除定位marker
                for (Marker maker : map.getMarkers()) {
                    if("location".equals(maker.getTitle())){
                        map.removeMarker(maker);
                    }
                }

                map.addMarker(new MarkerOptions()
                        .position(new LatLng(point.getY(), point.getX()))
                        .title("location")
                        .snippet("http://www.baidu.com")
                        .icon(icon));
//                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(point.getY(),point.getX()), 20.5));
            }
//            sendPoint();

            Log.i("pointMap", pointMap.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationEngineListener != null) {
            locationEngine.removeLocationEngineListener(locationEngineListener);
        }
        if (mapView != null) {
            mapView.onDestroy();
        }
        if (startIpsServiceIntent != null) {
            stopService(startIpsServiceIntent);
        }
        if (mIPSPointReceiver != null) {
            unregisterReceiver(mIPSPointReceiver);
        }
        System.gc();
    }


    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
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

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mapView.onDestroy();
//    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void loadBuildingLayer() {
        // Method used to load the indoor layer on the map. First the fill layer is drawn and then the
        // line layer is added.

        FillLayer indoorBuildingLayer = new FillLayer("indoor-building-fill", "indoor-building").withProperties(
                fillColor(Color.parseColor("#eeeeee")),
                visibility(VISIBLE),
                // Function.zoom is used here to fade out the indoor layer if zoom level is beyond 16. Only
                // necessary to show the indoor map at high zoom levels.
                fillOpacity(Function.zoom(Stops.exponential(
                        Stop.stop(17f, fillOpacity(1f)),
                        Stop.stop(16.5f, fillOpacity(0.5f)),
                        Stop.stop(16f, fillOpacity(0f))
                )))

        );

        map.addLayer(indoorBuildingLayer);

        LineLayer indoorBuildingLineLayer = new LineLayer("indoor-building-line", "indoor-building").withProperties(
                lineColor(Color.parseColor("#50667f")),
                lineWidth(0.5f),
                visibility(VISIBLE),
                fillOpacity(Function.zoom(Stops.exponential(
                        Stop.stop(17f, fillOpacity(1f)),
                        Stop.stop(16.5f, fillOpacity(0.5f)),
                        Stop.stop(16f, fillOpacity(0f))
                )))

        );
        map.addLayer(indoorBuildingLineLayer);
    }

    private String loadJsonFromAsset(String filename) {
        // Using this method to load in GeoJSON files from the assets folder.

        try {
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void setLayerVisible(String layerId) {
        Layer layer = map.getLayer(layerId);
        if (layer == null) {
            return;
        }
        if (VISIBLE.equals(layer.getVisibility().getValue())) {
            layer.setProperties(visibility(NONE));
        } else {
            layer.setProperties(visibility(VISIBLE));
        }
    }
    List<String> list = new ArrayList<String>();
    ArrayList<String> showlist = new ArrayList<String>(); //搜索建议显示列表
    /*
     * 初始化视图
     */
    private void initView() {
        list.add("aaa");list.add("bbb");list.add("ccc"); list.add("airsaid");
        mSearchView = (SearchView) findViewById(R.id.searchView);
        // 为该SearchView组件设置事件监听器
        mSearchView.setOnQueryTextListener(this);
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setTextFilterEnabled(true);
    }
    // 当点击搜索按钮时触发该方法
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    // 当搜索内容改变时触发该方法
    @Override
    public boolean onQueryTextChange(String newText) {
        showlist.clear();
        if (!StringUtils.isEmpty(newText)){
            for(int i = 0;i < list.size();i++){
                if(list.get(i).startsWith(newText)){
                    showlist.add(list.get(i));
                }
            }
            mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, showlist));
            mListView.setFilterText(newText);
        }else{
            mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, showlist));
            mListView.clearTextFilter();
        }
        return false;
    }

    //  发送当前位置和方位角给后台
    private void sendPointAndAzimuth(){
        String result = null;
        if(!guideFlg){
            guideFlg = true;
            //floatingActionGuideButton.setImageResource(R.drawable.ic_location_disabled_24dp);
            floatingActionGuideButton.setBackgroundColor(Color.parseColor("#FFFF00"));
        } else {
            guideFlg = false;
            //floatingActionGuideButton.setImageResource(R.drawable.map_guide);
            floatingActionGuideButton.setBackgroundColor(Color.parseColor("#FF4081"));
        }
        try {
            Properties properties = AppUtils.getProperties(getApplicationContext());
            String RequestURL=properties.getProperty("sendPointAndAzimuthUrl");
            Log.i(TAG, "请求的URL=" + RequestURL);

            // 得到手机Mac
            String macCode = IpMacUtils.getMacFromWifi();
            // 定位point和方位角
            Point point = (Point)pointMap.get("Point");
            float azimuth = (float)pointMap.get("Azimuth");

            mSendTask = new MapBoxActivity.SendPointAndAzimuthTask(RequestURL, pointMap);
            mSendTask.execute((String) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * SendPointAndAzimuthTask
     * 发送当前位置和方位角给后台的异步执行Class
     */
    public class SendPointAndAzimuthTask extends AsyncTask<String, Void, Boolean> {

        private String sRequestURL;
        private Map sPointMap;
        private String result;

        SendPointAndAzimuthTask(String RequestURL, Map pointMap) {
            sRequestURL = RequestURL;
            sPointMap = pointMap;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                // 得到手机Mac
                String macCode = IpMacUtils.getMacFromWifi();
                // 定位point和方位角
                Point point = (Point)pointMap.get("Point");
                float azimuth = (float)pointMap.get("Azimuth");

                net.sf.json.JSONObject reParams = new net.sf.json.JSONObject();
                reParams.put("macCode", macCode);
                reParams.put("latitude", point.getY());
                reParams.put("longitude", point.getX());
                reParams.put("azimuth", azimuth);
                reParams.put("operater", "app");
//                client.setConnectTimeout(10000);
                result = HttpUtils.doPost(sRequestURL,reParams.toString());
                net.sf.json.JSONObject resultJson = net.sf.json.JSONObject.fromObject(result);
                if(resultJson.size() == 0){
                    return false;
                } else if(resultJson.get("collectionses") != null){
//                    Intent intent = new Intent(MapBoxActivity.this, PhotoDetailActivity.class);
//                    /* 通过Bundle对象存储需要传递的数据 */
//                    Bundle bundle = new Bundle();
//                    bundle.putString("image", resultJson.get("collectionses").toString());
//                    intent.putExtras(bundle);
//                    startActivity(intent);
                    // 再地图上把图片的经纬度加到marker上
                    imageArray = new JSONArray(resultJson.get("collectionses").toString());
                }

            } catch (Exception e) {
                return false;
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSendTask = null;

            if (success) {
                Toast.makeText(getApplicationContext(), "发送经纬度和方位角成功!", Toast.LENGTH_SHORT).show();
                try{
                    // 再地图上把图片的经纬度加到marker上
                    Properties properties = AppUtils.getProperties(getApplicationContext());
                    final String defURL=properties.getProperty("defUrl");
                    imageMap = new HashMap<String, org.json.JSONObject>();
                    // 先删除以前的marker
                    for (Marker maker : map.getMarkers()) {
                        map.removeMarker(maker);
                    }
                    for(int i=0;i<imageArray.length();i++){
                        org.json.JSONObject jsonobject = imageArray.getJSONObject(i);
                        String imgurl = defURL + jsonobject.get("pictureUrl").toString();
                        imageMap.put(jsonobject.getString("name"), jsonobject);

                        map.addMarker(new MarkerOptions()
                                .position(new LatLng((Double)jsonobject.get("latitude"), (Double)jsonobject.get("longitude")))
                                .title(jsonobject.getString("name"))
                                .snippet(jsonobject.getString("commentText"))
                        );
                    }
                    map.setInfoWindowAdapter(new MapboxMap.InfoWindowAdapter() {
                        @Nullable
                        @Override
                        public View getInfoWindow(@NonNull Marker marker){
                            // The info window layout is created dynamically, parent is the info window
                            // container
                            LinearLayout parent = new LinearLayout(MapBoxActivity.this);
                            parent.setLayoutParams(new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            parent.setOrientation(LinearLayout.VERTICAL);
                            try{
                                // Depending on the marker title, the correct image source is used. If you
                                // have many markers using different images, extending Marker and
                                // baseMarkerOptions, adding additional options such as the image, might be
                                // a better choice.
                                ImageView countryFlagImage = new ImageView(MapBoxActivity.this);
                                String imgurl = defURL + imageMap.get(marker.getTitle()).get("pictureUrl").toString();
                                ImageUtils imageUtils = new ImageUtils();
                                Bitmap image = imageUtils.getImageAsync(imgurl);
                                countryFlagImage.setImageBitmap(image);

                                // Set the size of the image
                                countryFlagImage.setLayoutParams(new android.view.ViewGroup.LayoutParams(150, 100));

                                // add the image view to the parent layout
                                parent.addView(countryFlagImage);
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                            return parent;
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
                //finish();
            } else {
//                mPasswordView.setError(getString(R.string.error_incorrect_password));
//                net.sf.json.JSONObject resultJson = net.sf.json.JSONObject.fromObject(result);
//                Toast.makeText(getApplicationContext(), resultJson.get("msg").toString(), Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "导览失败！", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            mSendTask = null;
        }
    }

    private void sendPoint(){
        try{
            String macCode = IpMacUtils.getMacFromWifi();
            // 定位point和方位角
            Point point = (Point)pointMap.get("Point");
            float azimuth = (float)pointMap.get("Azimuth");

            net.sf.json.JSONObject reParams = new net.sf.json.JSONObject();
            reParams.put("macCode", macCode);
            reParams.put("latitude", point.getY());
            reParams.put("longitude", point.getX());
            reParams.put("azimuth", azimuth);
            reParams.put("operater", "app");
            SocketUtil socketUtil = new SocketUtil("192.168.1.103", 60301, reParams.toString());

            new Thread(socketUtil).start();
            sleep(3000);
//            socketUtil.writeBuf("123456".getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
