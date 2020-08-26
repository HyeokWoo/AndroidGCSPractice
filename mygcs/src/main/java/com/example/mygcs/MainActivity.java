package com.example.mygcs;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

import static com.o3dr.services.android.lib.drone.attribute.AttributeType.BATTERY;
import static java.security.AccessController.getContext;


//<<Main Class>>==================================================================================================================================================================================================================
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener {

    NaverMap mymap;

    private static final String TAG = MainActivity.class.getSimpleName();

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private boolean connectDrone = false;
    private final Handler handler = new Handler();
    private Spinner modeSelector;
    private boolean altitudeset = false;
    private LinearLayout setlist;           //우측상단 이륙고도
    private boolean armstatus = false;
    private Button takeoffmenu;
    private MediaCodecManager mediaCodecManager;
    private ArrayList<LatLng> pathcoords = new ArrayList<>();
    PolylineOverlay dronePath = new PolylineOverlay();
    Handler mainHandler;
    private Object userVO;
    private GuideMode guideMode;
    private LinearLayout armingbtn;
    private LinearLayout btnset;
    private boolean togglebtn = false;
    private boolean maplock = false;
    private boolean mapoption = false;
    private boolean mapcads =false;
    private boolean mapfollow = true;
    private LocationOverlay locationOverlay;
    private ArrayList<String> alertlist = new ArrayList<>();
    private RecyclerView recyclerView;
    private StateTextAdapter adapter;
    private boolean droneState = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 핸드폰 맨위 시간, 안테나 타이틀 없애기
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // 가로모드 고정
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE); //GPS 위치 관리자 객체 참조하기

        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);

        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        if (!connectDrone) {
            armingbtn = (LinearLayout) findViewById(R.id.connectmenu);
            armingbtn.setVisibility(View.INVISIBLE);
        }

        mapFragment.getMapAsync(this);

        //mapMode
        LinearLayout list1 = (LinearLayout)findViewById(R.id.maplocklayer);
        LinearLayout list2 = (LinearLayout)findViewById(R.id.mapoptionlayer);
        LinearLayout list3 = (LinearLayout)findViewById(R.id.mapcadstrallayer);
        btnset =(LinearLayout)findViewById(R.id.linearLayout3);
        btnset.setVisibility(View.INVISIBLE);
        list1.setVisibility(View.INVISIBLE);
        list2.setVisibility(View.INVISIBLE);
        list3.setVisibility(View.INVISIBLE);

        //recyclerView
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new StateTextAdapter(alertlist);
        recyclerView.setAdapter(adapter);


        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        this.modeSelector = (Spinner) findViewById(R.id.flightModeSelectorSpinner);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }

        });

        guideMode = new GuideMode();

        // Initialize media codec manager to decode video stream packets.
        HandlerThread mediaCodecHandlerThread = new HandlerThread("MediaCodecHandlerThread");
        mediaCodecHandlerThread.start();
        Handler mediaCodecHandler = new Handler(mediaCodecHandlerThread.getLooper());
        mediaCodecManager = new MediaCodecManager(mediaCodecHandler);

        mainHandler = new Handler(getApplicationContext().getMainLooper());
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.mymap = naverMap;
        droneLocation();

        mymap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick( PointF pointF,  LatLng latLng) {
                runGuideMode(latLng);
            }
        });
    }

    //Operate Event=====================================================================================================================================================================================================
    //Overlay
    public void droneLocation() {
        try {
            Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
            LatLng dronePosition = new LatLng(droneGps.getPosition().getLatitude(), droneGps.getPosition().getLongitude());

            LocationOverlay locationOverlay = mymap.getLocationOverlay();
            locationOverlay.setVisible(true);

            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.flight));
            locationOverlay.setIconWidth(LocationOverlay.SIZE_AUTO);
            locationOverlay.setIconHeight(LocationOverlay.SIZE_AUTO);

            locationOverlay.setPosition(dronePosition);

            if(mapfollow)
                mymap.moveCamera(CameraUpdate.scrollTo(dronePosition));

        } catch (NullPointerException e) {
            Log.d("myLog", "getPosition Error : " + e.getMessage());
            LocationOverlay locationOverlay = mymap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setPosition(new LatLng(35.942339, 126.683388));

            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.flight));
            locationOverlay.setIconWidth(LocationOverlay.SIZE_AUTO);
            locationOverlay.setIconHeight(LocationOverlay.SIZE_AUTO);

            if(mapfollow)
                mymap.moveCamera(CameraUpdate.scrollTo(new LatLng(35.945378,126.682110)));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        Log.d("testLog", event.toString());

        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                droneState = mydronestate();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.HOME_UPDATED:
                updateDistanceFromHome();
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateBatteryVolt();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;

            case AttributeEvent.GPS_COUNT:
                updateNumberOfSatellites();
                break;

            case AttributeEvent.GPS_POSITION:
                pathLine();
                delGuideMode();
                break;

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }

    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    public void onBtnConnectTap() {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }
    }

    public void btn_event(View v) {
        switch (v.getId()) {
            case R.id.btnconnect:
                onBtnConnectTap();
                break;
            case R.id.btnarm:
                onArmButtonTap();
                break;
            case R.id.btnland:
                break;
            case R.id.takeoffset:
                takeoffsetTap();
                break;
            case R.id.drone_rise:
                onAsecTap();
                break;
            case R.id.drone_fall:
                onDescTap();
                break;
            case R.id.maplockbtn:
                maplock = !maplock;
                LinearLayout list = (LinearLayout)findViewById(R.id.maplocklayer);
                onMapbtnTap(list,maplock);
                break;
            case R.id.mapoptionbtn:
                mapoption = !mapoption;
                LinearLayout list1 = (LinearLayout)findViewById(R.id.mapoptionlayer);
                onMapbtnTap(list1,mapoption);
                break;
            case R.id.mapcadastral:
                mapcads = !mapcads;
                LinearLayout list2 = (LinearLayout)findViewById(R.id.mapcadstrallayer);
                onMapbtnTap(list2,mapcads);
                break;
            case R.id.maplock:
                mapfollow = true;
                mapfollowTap();
                break;
            case R.id.mapmove:
                mapfollow = false;
                mapfollowTap();
                break;
            case R.id.basicmap:
                onMapOptionTap(R.id.basicmap);
                break;
            case R.id.satellitemap:
                onMapOptionTap(R.id.satellitemap);
                break;
            case R.id.hybridmap:
                onMapOptionTap(R.id.hybridmap);
                break;
            case R.id.cadaston:
                onCadastTap(R.id.cadaston);
                break;
            case R.id.cadastoff:
                onCadastTap(R.id.cadastoff);
                break;
            case R.id.toggle:
                onToggleTap();
                break;

        }
    }

    //Distance=================================================================================================================================================================================================
    protected void updateDistanceFromHome() {
        TextView distanceTextView = (TextView) findViewById(R.id.yawValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double vehicleAltitude = droneAltitude.getAltitude();
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromHome = 0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            Home droneHome = this.drone.getAttribute(AttributeType.HOME);
            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
        } else {
            distanceFromHome = 0;
        }

        distanceTextView.setText(String.format("%3.1f", distanceFromHome) + "m");
    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    //Vehicle Value=======================================================================================================================================================================================================================
    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    protected void updateBatteryVolt() {
        TextView voltTextView = (TextView) findViewById(R.id.batteryVoltageValueTextView);
        Battery droneVolt = this.drone.getAttribute(BATTERY);
        Log.d("MYLOG", "베터리 변화 : " + droneVolt.getBatteryVoltage());
        voltTextView.setText(String.format(" " + droneVolt.getBatteryVoltage() + "V"));
    }

    protected void updateYaw() {
        LocationOverlay locationOverlay = mymap.getLocationOverlay();
        double yawvalue = 0;
        TextView yawTextView = (TextView) findViewById(R.id.yawValueTextView);
        Attitude droneyaw = this.drone.getAttribute(AttributeType.ATTITUDE);

        if (droneyaw.getYaw() < 0)
            yawvalue = droneyaw.getYaw() + 360;
        else
            yawvalue = droneyaw.getYaw();

        yawTextView.setText(String.format("%3.1f", yawvalue));
        locationOverlay.setBearing((float) droneyaw.getYaw());
    }

    protected void updateNumberOfSatellites() {
        TextView numberOfSatellitesTextView = (TextView) findViewById(R.id.numberofSatellitesValueTextView);
        Gps droneNumberOfSatellites = this.drone.getAttribute(AttributeType.GPS);
        Log.d("MYLOG", "위성 수 변화 : " + droneNumberOfSatellites.getSatellitesCount());
        numberOfSatellitesTextView.setText(String.format("%d", droneNumberOfSatellites.getSatellitesCount()));
    }

    //Button=================================================================================================================================================================================================
    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.btnconnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
            connectDrone = false;
            armingbtn.setVisibility(View.INVISIBLE);
        } else {
            connectButton.setText("Connect");
            connectDrone = true;
            armingbtn.setVisibility(View.VISIBLE);
        }
    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.btnarm);

        if (!this.drone.isConnected()) {
            armingbtn.setVisibility(View.INVISIBLE);
        } else {
            armingbtn.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    //지도 옵션 버튼
    public void onToggleTap(){
        togglebtn = !togglebtn;
        LinearLayout list1 = (LinearLayout)findViewById(R.id.maplocklayer);
        LinearLayout list2 = (LinearLayout)findViewById(R.id.mapoptionlayer);
        LinearLayout list3 = (LinearLayout)findViewById(R.id.mapcadstrallayer);
        if(togglebtn){
            btnset.setVisibility(View.VISIBLE);
        }
        else{
            maplock = false;
            mapoption = false;
            mapcads = false;
            list1.setVisibility(View.INVISIBLE);
            list2.setVisibility(View.INVISIBLE);
            list3.setVisibility(View.INVISIBLE);
            btnset.setVisibility(View.INVISIBLE);
        }

    }
    public void onCadastTap(int id){
        Button cadastbtn = (Button)findViewById(R.id.mapcadastral);
        LinearLayout list = (LinearLayout)findViewById(R.id.mapcadstrallayer);

        switch(id){
            case R.id.cadaston:
                mymap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                cadastbtn.setText("지적도on");
                break;
            case R.id.cadastoff:
                mymap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                cadastbtn.setText("지적도off");
                break;
        }
        mapcads = false;
        list.setVisibility(View.INVISIBLE);
    }
    public void onMapOptionTap(int id){
        Button mapoptionbtn = (Button)findViewById(R.id.mapoptionbtn);
        LinearLayout list = (LinearLayout)findViewById(R.id.mapoptionlayer);

        switch(id)
        {
            case R.id.basicmap:
                mymap.setMapType(NaverMap.MapType.Basic);
                mapoptionbtn.setText("기본지도");
                break;
            case R.id.satellitemap:
                mymap.setMapType(NaverMap.MapType.Satellite);
                mapoptionbtn.setText("위성지도");
                break;
            case R.id.hybridmap:
                mymap.setMapType(NaverMap.MapType.Hybrid);
                mapoptionbtn.setText("hybrid");
                break;

        }
        mapoption = false;
        list.setVisibility(View.INVISIBLE);
    }
    public void mapfollowTap(){
        Button lockbtn = (Button)findViewById(R.id.maplockbtn);
        LinearLayout list = (LinearLayout)findViewById(R.id.maplocklayer);

        if(mapfollow)
            lockbtn.setText("맵 잠금");
        else
            lockbtn.setText("맵 이동");

        maplock = false;
        list.setVisibility(View.INVISIBLE);
    }

    public void onMapbtnTap(LinearLayout list, boolean visual){
        if(visual){
            list.setVisibility(View.VISIBLE);
        }
        else{
            list.setVisibility(View.INVISIBLE);
        }
    }

    //상단우측의 이륙고도 조정 버튼
    public void takeoffsetTap() {

        altitudeset = !altitudeset;
        if (altitudeset) {
            setlist.setVisibility(View.VISIBLE);
        } else
            setlist.setVisibility(View.INVISIBLE);
    }

    public void onAsecTap() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Altitude currentAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        if (vehicleState.isFlying()) {
            ControlApi.getApi(this.drone).climbTo(currentAltitude.getAltitude() + 0.5);
        }
    }

    public void onDescTap() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Altitude currentAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        if (vehicleState.isFlying()) {
            if (currentAltitude.getAltitude() > 0)
                ControlApi.getApi(this.drone).climbTo(currentAltitude.getAltitude() - 0.5);
        }
    }

    public void alertMessage() {
        final Drone mydrone = this.drone;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("arming alert");
        builder.setMessage("기체의 모터를 가동하시겠습니까?");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                VehicleApi.getApi(mydrone).arm(true, false, new SimpleCommandListener() {
                    @Override
                    public void onError(int executionError) {
                        alertUser("Unable to arm vehicle.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Arming operation timed out.");
                    }
                });
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void takeoffAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Drone mydrone = this.drone;
        builder.setTitle("takeoff alert");
        builder.setMessage("기체가 상승합니다 안전거리 유지 바랍니다.");     //
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ControlApi.getApi(mydrone).takeoff(5.5, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        alertUser("Taking off...");
                    }

                    @Override
                    public void onError(int i) {
                        alertUser("Unable to take off.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Unable to take off.");
                    }
                });
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();

            }
        });

        AlertDialog alertdialog = builder.create();
        alertdialog.show();
    }

    //Helper===================================================================================================================================================================================================
   /* protected void updatetrack(){
        try{

            Gps dronegps = this.drone.getAttribute(AttributeType.GPS);
            LatLng droneposition = new LatLng(dronegps.getPosition().getLatitude(),dronegps.getPosition().getLongitude());

            Log.d("GPSERROR1",""+droneposition.latitude);
            this.locationOverlay = mymap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.flight));
            locationOverlay.setPosition(droneposition);
            if(mapfollow)
                mymap.moveCamera(CameraUpdate.scrollTo(droneposition));
        }catch(NullPointerException e){
            Log.d("GPSERROR","GPS POSITION NULL");
            // locationOverlay = mymap.getLocationOverlay();
            this.locationOverlay = mymap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.flight));
            locationOverlay.setPosition(new LatLng(35.945378,126.682110));
            //locationOverlay.setAnchor(new PointF((float)0.5,(float)0.5));
            if(mapfollow)
                mymap.moveCamera(CameraUpdate.scrollTo(new LatLng(35.945378,126.682110)));

        }
        //
        //mymap.setLocationTrackingMode(LocationTrackingMode.Follow);
    }*/
    
    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
        alertlist.add(message);

        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(alertlist.size()-1);
    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch (connectionStatus.getStatusCode()) {
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    protected void updateVehicleModesForType(int droneType) {

        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    public void onArmButtonTap() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });
        } else if (vehicleState.isArmed()) {
            takeoffAlert();
            // Take off
//            ControlApi.getApi(this.drone).takeoff(10, new AbstractCommandListener() {
//
//                @Override
//                public void onSuccess() {
//                    alertUser("Taking off...");
//                }
//
//                @Override
//                public void onError(int i) {
//                    alertUser("Unable to take off.");
//                }
//
//                @Override
//                public void onTimeout() {
//                    alertUser("Unable to take off.");
//                }
//            });
        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            alertMessage();
            // Connected but not Armed
//            VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {
//                @Override
//                public void onError(int executionError) {
//                    alertUser("Unable to arm vehicle.");
//                }
//
//                @Override
//                public void onTimeout() {
//                    alertUser("Arming operation timed out.");
//                }
//            });
        }
    }

    //I don't understand What those are========================================================================================================================================================================
    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null) {
            alertUser("Unable to retrieve the solo state.");
        } else {
            alertUser("Solo state is up to date.");
        }

    }

    //<<GuideMode>>=================================================================================================================================================================================================================
//    private void runGuideMode(LatLng targetPoint) {
//        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
//        if (vehicleState.isConnected()) {
//            if (vehicleState.isArmed()) {
//                if(vehicleState.isArmed()) {
//                    guideMode.mMarkerGuide.setPosition(targetPoint);
//                    guideMode.mMarkerGuide.setMap(mymap);
//                    guideMode.mMarkerGuide.setIcon(OverlayImage.fromResource(R.drawable.destnation));
//                    guideMode.DialogSimple(drone, new LatLong(targetPoint.latitude, targetPoint.longitude));
//               } else {
//                    Toast.makeText(this, "비행중이 아니군요.", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//            } else {
//                Toast.makeText(this, "이번엔 시동이 문제군요.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//        } else {
//            Toast.makeText(this, "기체가 아직 연결이 안 되었군요.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//    }

    private void runGuideMode(LatLng latLng) {
        if (droneState) {
            guideMode.mGuidedPoint = latLng;
            guideMode.mMarkerGuide.setPosition(latLng);
            guideMode.mMarkerGuide.setMap(mymap);
            guideMode.mMarkerGuide.setIcon(OverlayImage.fromResource(R.drawable.destnation));
            guideMode.DialogSimple(drone, new LatLong(latLng.latitude, latLng.longitude));
        }
        else {
            Toast.makeText(this, "비행중이 아니군요.", Toast.LENGTH_SHORT).show();
            return;
        }

    }

    public boolean mydronestate(){
        State vehiclestate = this.drone.getAttribute(AttributeType.STATE);
        if(vehiclestate.isArmed())
            return true;
        else
            return false;
    }

    public void delGuideMode() {
        Drone mydrone = this.drone;

        try {
            if (guideMode.CheckGoal(mydrone, guideMode.mGuidedPoint)) {
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_LOITER, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        guideMode.mMarkerGuide.setMap(null);
                        alertUser("mode changed / 현재고도를 유지하며 이동합니다.");
                    }

                    @Override
                    public void onError(int executionError) {
                        alertUser("이동 할 수 없습니다.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("시간 초과입니다.");
                    }
                });
            }
        } catch (NullPointerException e) {
            Log.d("NONMARKER", "no marker exist");
        }
    }

    //경로선
    public void pathLine(){
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLng dronePosition = new LatLng(droneGps.getPosition().getLatitude(),droneGps.getPosition().getLongitude());

        try{
            pathcoords.add(dronePosition);
            dronePath.setCoords(pathcoords);
            dronePath.setPattern(10, 5);
            dronePath.setMap(mymap);

            Log.d("DRONEPATH","list size:"+pathcoords.size());
        }catch(NullPointerException e){
            Log.d("DRONEPATH","gps position list is null");
        }


    }

    class GuideMode {
        LatLng mGuidedPoint; //가이드모드 목적지 저장
        Marker mMarkerGuide = new com.naver.maps.map.overlay.Marker(); //GCS 위치 표시 마커 옵션

        void DialogSimple(final Drone drone, final LatLong point) {
            AlertDialog.Builder alt_bld = new AlertDialog.Builder(MainActivity.this);
            alt_bld.setMessage("확인하시면 가이드모드로 전환 후 기체가 이동합니다.").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Action for 'Yes' Button
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener() {
                        @Override
                        public void onSuccess() {
                            ControlApi.getApi(drone).goTo(point, true, null);
                            alertUser("Success");
                        }

                        @Override
                        public void onError(int i) {
                            alertUser("Error");
                        }

                        @Override
                        public void onTimeout() {
                            alertUser("Timeout(잠시중단)");
                        }
                    });
                }
            }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = alt_bld.create();       //알림창 객체 생성
            // Title for AlertDialog
            alert.setTitle("Title");
            // Icon for AlertDialog
            alert.show();
        }

        public boolean CheckGoal(final Drone drone, LatLng recentLatLng) {
            GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
            LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(), guidedState.getCoordinate().getLongitude());
            return target.distanceTo(recentLatLng) <= 1;
        }
    }
}


