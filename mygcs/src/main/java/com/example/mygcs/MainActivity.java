package com.example.mygcs;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaCodec;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Surface;
import android.view.TextureView;
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
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
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
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.command.YawCondition;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;
import com.o3dr.services.android.lib.util.MathUtils;

import org.droidplanner.services.android.impl.core.helpers.geoTools.LineLatLong;
import org.droidplanner.services.android.impl.core.polygon.Polygon;
import org.droidplanner.services.android.impl.core.survey.grid.CircumscribedGrid;
import org.droidplanner.services.android.impl.core.survey.grid.Trimmer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener{
    private boolean dronestate = false;
    protected Drone drone;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private boolean connectDrone = false;
    private boolean armstatus = false;
    private boolean altitudeset = false;
    private boolean maplock = false;
    private boolean mapoption = false;
    private boolean mapcads =false;
    private boolean mapfollow = true;
    private boolean togglebtn = false;
    private boolean missionlist = false;
    private LinearLayout btnset;
    private LinearLayout armingbtn;
    private LinearLayout setlist;
    private double dronealtitude=5.5;
    private static final String TAG = MainActivity.class.getSimpleName();
    private Button takeoffsetbtn;
    private final Handler handler = new Handler();
    private FusedLocationSource locationSource;
    private NaverMap mymap;
    private ArrayList<LatLng> pathcoords = new ArrayList<>();
    private PathOverlay dronepath;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private Spinner modeSelector;
    private LocationOverlay locationOverlay;
    private ArrayList<String> alertlist = new ArrayList<>();
    private RecyclerView recyclerView;
    private StateTextAdapter adapter;

    //polygon
    private boolean mission = false;
    public ArrayList<LatLong> polygonPointList = new ArrayList<LatLong>();
    public ArrayList<LatLong> sprayPointList = new ArrayList<>();
    private MainActivity mainActivity;
    private ManageOverlay manageOverlays;
    private LatLong pointA = null;
    private LatLong pointB = null;
    private double sprayDistance = 5.5f;
    private int maxSprayDistance = 50;
    private int capacity = 0;
    private double sprayAngle;
    private int missioncount=0;
    // video manage
    Handler mainHandler;
    private TextureView videoView;
    private MediaCodecManager mediaCodecManager;
    private String videotag = "testvideotag";




    @Nullable
    private LocationManager locationManager;

    @Nullable
    private LocationSource.OnLocationChangedListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        this.modeSelector = (Spinner) findViewById(R.id.flymode);
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
        locationSource =
                new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
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
        if(armstatus==false){

            setlist= (LinearLayout)findViewById(R.id.takeoffsetlist);
            setlist.setVisibility(View.INVISIBLE);
        }
        takeoffsetbtn = (Button)findViewById(R.id.takeoffset);
        takeoffsetbtn.setText("고도"+dronealtitude);
        LinearLayout list1 = (LinearLayout)findViewById(R.id.maplocklayer);
        LinearLayout list2 = (LinearLayout)findViewById(R.id.mapoptionlayer);
        LinearLayout list3 = (LinearLayout)findViewById(R.id.mapcadstrallayer);
        LinearLayout list4 = (LinearLayout)findViewById(R.id.missiondrawer);

        btnset =(LinearLayout)findViewById(R.id.linearLayout3);
        btnset.setVisibility(View.INVISIBLE);
        list1.setVisibility(View.INVISIBLE);
        list2.setVisibility(View.INVISIBLE);
        list3.setVisibility(View.INVISIBLE);
        list4.setVisibility(View.INVISIBLE);
        dronepath = new PathOverlay();

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new StateTextAdapter(alertlist);
        recyclerView.setAdapter(adapter);

        mapFragment.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                mymap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.mymap = naverMap;
        manageOverlays = new ManageOverlay(this.mymap,this);
        // 네이버 로고 위치 변경
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLogoMargin(2080, 0, 0, 925);

        // 나침반 제거
        uiSettings.setCompassEnabled(false);

        // 축척 바 제거
        uiSettings.setScaleBarEnabled(false);

        // 줌 버튼 제거
        uiSettings.setZoomControlEnabled(false);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 핸드폰 맨위 시간, 안테나 타이틀 없애기

        //임무 전송 버튼
        Button btnMission = (Button)findViewById(R.id.sendmission);
        btnMission.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                if(polygonPointList.size()>0) {
                    setMission();
                }else
                    alertUser("need Marker");
            }
        });
        //임무 시작 버튼
        Button btnStartmission = (Button)findViewById(R.id.startmission);
        btnStartmission.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                if(btnStartmission.getText().equals("임무시작")){
                    changetoAutomode();
                    btnStartmission.setText("임무중지");
                }
                else if(btnStartmission.getText().equals("임무중지")){
                    abortmission();
                    btnStartmission.setText("임무시작");
                }
            }
        });
    }

    //======================================================================================<<자율주행 미션>>=============================================================================================
    //마커&좌표 넣기
    public void customMission(){
        LatLong stationPointM = new LatLong(35.942197, 126.678888);   //스테이션 좌표
        LatLong stationPoint1 = new LatLong(35.942087, 126.678860);
        LatLong stationPoint2 = new LatLong(35.942072, 126.678933);
        LatLong stationPoint3 = new LatLong(35.942173, 126.678978);

        polygonPointList.add(stationPointM); //좌표를 arrayList에 넣는다.
        polygonPointList.add(stationPoint1);
        polygonPointList.add(stationPoint2);
        polygonPointList.add(stationPoint3);

        manageOverlays.stationMarker();

        dronepath.setCoords(Arrays.asList(   //스테이션 경로
                new LatLng(35.942197, 126.678888),
                new LatLng(35.942087, 126.678860),
                new LatLng(35.942072, 126.678933),
                new LatLng(35.942173, 126.678978),
                new LatLng(35.942197, 126.678888)
        ));

        dronepath.setPatternImage(OverlayImage.fromResource(R.drawable.arrow));
        dronepath.setPatternInterval(70);

        dronepath.setWidth(40);
        dronepath.setOutlineWidth(10);
        dronepath.setOutlineColor(Color.TRANSPARENT);

        dronepath.setMap(mymap);

        alertUser("스테이션M 좌표:" + polygonPointList.get(0).getLatitude());
        alertUser("스테이션1 좌표:" + polygonPointList.get(1).getLatitude());
        alertUser("스테이션2 좌표:" + polygonPointList.get(2).getLatitude());
        alertUser("스테이션3 좌표:" + polygonPointList.get(3).getLatitude());
    }

    //setmission
    public void setMission(){
        Mission mMission = new Mission();
        for(int i=0;i<polygonPointList.size();i++){
            Waypoint waypoint = new Waypoint();
            waypoint.setDelay(1);

            LatLongAlt latLongAlt = new LatLongAlt(polygonPointList.get(i).latitude,polygonPointList.get(i).longitude,dronealtitude);
            waypoint.setCoordinate(latLongAlt);

            mMission.addMissionItem(waypoint);
        }
        MissionApi.getApi(this.drone).setMission(mMission,true);
    }

    //startmission
    public void changetoAutomode(){
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_AUTO,new SimpleCommandListener(){
            @Override
            public void onSuccess() {
                alertUser("Auto 모드로 변경 중...");
            }


            @Override
            public void onError(int executionError) {
                alertUser("Auto 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Auto 모드 변경 실패.");
            }
        });
    }
    //stop mission
    public void abortmission(){
        MissionApi.getApi(this.drone).pauseMission(null);
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER,new SimpleCommandListener(){
            @Override
            public void onSuccess(){
                alertUser("Loiter mode");

            }
            @Override
            public void onError(int executionError) {
                alertUser("Loiter 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Loiter 모드 변경 실패.");
            }
        });
    }

    //gettoplatform
    public void getPlat(){
        //pauseMission();
        ChangeToLoiterMode();
        alertUser("Change to LoiterMode");

        ControlApi.getApi(this.drone).climbTo(1);
        alertUser("approaching to the platform...");

        try{
            Thread.sleep(5000); //wait for onboard signal
            ControlApi.getApi(this.drone).climbTo(dronealtitude);
            alertUser("leaving...Platform");

            changetoAutomode();
            alertUser("Change to AutoMode");

        }catch(InterruptedException e){
            alertUser("sleep denied");
            ControlApi.getApi(this.drone).climbTo(dronealtitude);

            changetoAutomode();
            alertUser("Change to AutoMode");
        }
        /*
        finally{
            MissionApi.getApi(this.drone).startMission(true,true,null);
            alertUser("return to the mission..");
        }*/

    }
/*
    private void pauseMission() {
        MissionApi.getApi(this.drone).pauseMission(null);
    }*/

    private void ChangeToLoiterMode() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Loiter 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Loiter 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Loiter 모드 변경 실패");
            }
        });
    }
    //========================================================================================<<드론 이벤트>>===========================================================================================
    @Override
    public void onDroneEvent(String event, Bundle extras) {
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

                break;
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
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
            case AttributeEvent.GPS_POSITION:
                updatetrack();
                break;
            case AttributeEvent.BATTERY_UPDATED:
                updateVolt();
                break;
            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;
            case AttributeEvent.GPS_COUNT:
                updateNumberOfSatellites();
                break;
            case AttributeEvent.MISSION_SENT:
                alertUser("mission upload succ");
                break;
            case AttributeEvent.MISSION_ITEM_REACHED:
                getPlat();
                missioncount++;
                if(missioncount==polygonPointList.size())
                {
                    missioncount =0;
                    changetoAutomode();
                }
                //getPlat();
            case AttributeEvent.MISSION_UPDATED:
                break;

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    //button event list
    public void btn_event(View v){
        LinearLayout missiondrawlist = (LinearLayout)findViewById(R.id.missiondrawer);
        switch(v.getId()){
            case R.id.connect:
                onBtnConnectTap();
                break;
            case R.id.arm:
                onArmButtonTap();
                break;
            case R.id.land:
                onLandButtonTap();
                break;
            case R.id.takeoffset:
                takeoffsetTap();
                break;
            case R.id.drone_asec:
                onAsecTap();
                break;
            case R.id.drone_desc:
                onDescTap();
                break;
            case R.id.maplockbtn:

                LinearLayout list = (LinearLayout)findViewById(R.id.maplocklayer);

                onlistbtnTap(list);
                break;
            case R.id.mapoptionbtn:

                LinearLayout list1 = (LinearLayout)findViewById(R.id.mapoptionlayer);
                onlistbtnTap(list1);
                break;
            case R.id.mapcadastral:

                LinearLayout list2 = (LinearLayout)findViewById(R.id.mapcadstrallayer);
                onlistbtnTap(list2);
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
            case R.id.mission:
                missionlist = !missionlist;

                if(missiondrawlist.getVisibility()==View.INVISIBLE)
                    missiondrawlist.setVisibility(View.VISIBLE);
                else
                    missiondrawlist.setVisibility(View.INVISIBLE);
                break;
            case R.id.nomission:
                resetMarker();
                polygonPointList.clear();
                dronepath.setMap(null);
                missiondrawlist.setVisibility(View.INVISIBLE);
                break;
            case R.id.custom:
                customMission();
                missiondrawlist.setVisibility(View.INVISIBLE);
                break;

        }
    }
    //=============================================================================================<<Helper>>>>=====================================================================================================
    public void resetMarker(){
        manageOverlays.reset();
    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
        alertlist.add(message);

        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(alertlist.size()-1);
    }

    public void onLandButtonTap(){
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
        }
    }

    public void takeoffsetTap(){

        altitudeset = !altitudeset;
        if(altitudeset)
        {
            setlist.setVisibility(View.VISIBLE);
        }
        else
            setlist.setVisibility(View.INVISIBLE);
    }
    public void onAsecTap(){

        if(dronealtitude<10){
            dronealtitude += 0.5;
            takeoffsetbtn.setText("이륙고도"+dronealtitude);
        }
    }
    public void onDescTap(){

        if(dronealtitude>3){
            dronealtitude -= 0.5;
            takeoffsetbtn.setText("이륙고도"+dronealtitude);
        }

    }
    public void alertMessage(){
        Drone mydrone = this.drone;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("arming alert");
        builder.setMessage("모터를 가동합니다");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
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
    public void takeoffAlert(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Drone mydrone = this.drone;
        builder.setTitle("takeoff alert");
        builder.setMessage("기체가 상승합니다 안전거리 유지 바랍니다.");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ControlApi.getApi(mydrone).takeoff(dronealtitude,new AbstractCommandListener(){
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
    public void onArmButtonTap() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isArmed()) {
            // Take off
            takeoffAlert();

        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            alertMessage();
            // Connected but not Armed

        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null) {
            alertUser("Unable to retrieve the solo state.");
        } else {
            alertUser("Solo state is up to date.");
        }
    }
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

    protected void updatetrack(){
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
    }
    protected void updateNumberOfSatellites() {
        TextView numberOfSatellitesTextView = (TextView)findViewById(R.id.satenum);
        Gps droneNumberOfSatellites = this.drone.getAttribute(AttributeType.GPS);

        numberOfSatellitesTextView.setText(String.format("%d", droneNumberOfSatellites.getSatellitesCount()));
    }
    protected void updateVehicleModesForType(int droneType) {

        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }
    /* protected void updateTakeOffDrawer(){
         State vehicleState = this.drone.getAttribute(AttributeType.STATE);

         if(vehicleState.isArmed())
         {
             armstatus = true;
             takeoffsetbtn.setVisibility(View.VISIBLE);

         }
         else{
             armstatus = false;
             takeoffsetbtn.setVisibility(View.INVISIBLE);

         }

     }*/
    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.arm);

        if (!this.drone.isConnected()) {
            armingbtn.setVisibility(View.INVISIBLE);
        } else {
            armingbtn.setVisibility(View.VISIBLE);
        }


        if (vehicleState.isArmed()) {
            // Take off

            armButton.setText("TAKE-OFF");

        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect((TowerListener) this);

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

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.connect);
        if (isConnected) {
            connectButton.setText("Disco");
            connectDrone = false;
            armingbtn.setVisibility(View.INVISIBLE);
        } else {
            connectButton.setText("Conn");
            connectDrone = true;
            armingbtn.setVisibility(View.VISIBLE);
        }
    }

    public void onBtnConnectTap() {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }

    }

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

    public void onlistbtnTap(LinearLayout list){
        if(list.getVisibility() == View.INVISIBLE){
            list.setVisibility(View.VISIBLE);
        }
        else{
            list.setVisibility(View.INVISIBLE);
        }
    }


    protected void updateAltitude() {

        TextView altitudeTextView = (TextView) findViewById(R.id.altitude);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");



    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speed);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");


    }

    protected void updateVolt(){
        TextView voltTextView = (TextView)findViewById(R.id.volt);
        Battery droneVolt = this.drone.getAttribute(AttributeType.BATTERY);
        voltTextView.setText(String.format("%3.2f",droneVolt.getBatteryVoltage())+"V");
    }

    protected void updateYaw(){
        double yawvalue=0;
        TextView yawTextView = (TextView)findViewById(R.id.YAW1);
        Attitude droneyaw = this.drone.getAttribute(AttributeType.ATTITUDE);
        if(droneyaw.getYaw()<0)
            yawvalue = droneyaw.getYaw()+360;
        else
            yawvalue = droneyaw.getYaw();

        yawTextView.setText(String.format("%3.1f",yawvalue));
        locationOverlay.setBearing((float) droneyaw.getYaw());
    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
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

}


