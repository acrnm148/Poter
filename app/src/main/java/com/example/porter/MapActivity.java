package com.example.porter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapMarkerItem2;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapActivity extends AppCompatActivity{
    private TMapGpsManager tMapGps = null;
    private TMapView tMapView;
    private double lat;
    private double lon;
    private double realdistance = 0; //거리값
    private static String AppKey = "l7xx2b1c5cd91b914c2c9c80aab1109ae5d3";
    private long backKeyPressedTime = 0; // 마지막으로 뒤로 가기 버튼을 눌렀던 시간 저장
    private Toast toast; // 첫 번째 뒤로 가기 버튼을 누를 때 표시
    private String[] item;
    private int routeNum;
    private int choiceRoute = 0; //경로 종류 선택받음
    /**
     - 0: 교통최적+추천(기본값)
     - 1: 교통최적+무료우선
     - 2: 교통최적+최소시간
     - 3: 교통최적+초보
     - 4: 교통최적+고속도로우선
     - 10: 최단거리+유/무료
     - 12: 이륜차도로우선 (일반도로가 없는 경우 자동차 전용도로로 안내 할 수 있습니다.)
     - 19: 교통최적+어린이보호구역 회피
    */
    ListView listView;
    EditText editStart;
    EditText editEnd;
    TextView textView;
    Spinner spinner;
    ArrayAdapter<POI> mAdapter;
    String keyword;
    LinearLayout layout;

    private double currentLatitude;
    private double currentlongitude;
    private boolean locationState = true; //현위치로 이동 여부
    private boolean startBtnState = false;
    private boolean endBtnState = false;
    private TMapPoint tMapPointStart = null;
    private TMapPoint tMapPointEnd = null;
    //private TMapPoint tMapPointStart = new TMapPoint(35.17241886016579, 129.1263765979288);//영상물 등급위원회
    //private TMapPoint tMapPointEnd = new TMapPoint(35.17127425152002, 129.12722778443444);//영화의 전당


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE); //키보드 내리기

        editStart = (EditText) findViewById(R.id.edit_start);
        editEnd = (EditText) findViewById(R.id.edit_end);
        listView = (ListView) findViewById(R.id.listView);
        layout = (LinearLayout) findViewById(R.id.tmap);
        mAdapter = new ArrayAdapter<POI>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(mAdapter);
        listView.setVisibility(View.GONE); //listview 안보이게

        //터치하면 텍스트 전체 선택
        editStart.selectAll();
        editEnd.selectAll();

        //상태바 투명 & 아이콘 회색
        setStateBar();

        //TMapAPI 활용(지도, 현재위치)
        setTMap();

        //현재위치를 받아오는 부분
        TMapGpsManager gps = new TMapGpsManager(this);
        gps.setMinTime(1000);
        gps.setMinDistance(5);
        gps.setProvider(gps.GPS_PROVIDER);
        //안드로이드 기기의 API 버전이 23이상일 때 위치권한 허용에 관한 여부를 물음
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1); //위치권한 탐색 허용 관련 내용
            }
            return;
        }
        gps.OpenGps();
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {

                if (location != null) {
                    currentLatitude = location.getLatitude();
                    currentlongitude = location.getLongitude();
                    tMapView.setLocationPoint(currentlongitude, currentLatitude);
                    if (locationState==true) {
                        tMapView.setCenterPoint(currentlongitude, currentLatitude); //지도 센터에 처음 한번만 뜨게함
                        locationState = false;
                    }
                }
            }
            public void onProviderDisabled(String provider) { }
            public void onProviderEnabled(String provider) { }
            public void onStatusChanged(String provider, int status, Bundle extras) { }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, mLocationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mLocationListener);


        //버튼 클릭 이벤트
        /* 출발지 search 버튼 */
        Button btnStart = (Button) findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startBtnState = true;
                keyword = editStart.getText().toString();
                searchPOI();
                imm.hideSoftInputFromWindow(editStart.getWindowToken(), 0); //키보드 내리기
            }
        });
        /* 출발지 editText 클릭이벤트 */
        editStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        //터치했을 때의 이벤트
                        startBtnState = true;
                        break;
                    }
                }
                return false;
            }
        });
        /* 출발지 text 삭제 및 경로 제거 */
        ImageView cancelSta = (ImageView) findViewById(R.id.cancel_st);
        cancelSta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tMapView.removeTMapPath();//경로 제거
                editStart.setText(null);//text 삭제
                tMapPointStart = null; //출발지 초기화
                listView.setVisibility(View.GONE); //listview 숨기기
            }
        });
        /* 도착지 search 버튼 */
        Button btnEnd = (Button) findViewById(R.id.btn_end);
        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //endBtnState = true;
                keyword = editEnd.getText().toString();
                searchPOI();
                imm.hideSoftInputFromWindow(editEnd.getWindowToken(), 0); //키보드 내리기
            }
        });
        /* 도착지 editText 클릭이벤트 */
        editEnd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        //터치했을 때의 이벤트
                        endBtnState = true;
                        break;
                    }
                }
                return false;
            }
        });
        /* 도착지 text 삭제 및 경로 제거 */
        ImageView cancelFin = (ImageView) findViewById(R.id.cancel_fin);
        cancelFin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tMapView.removeTMapPath();//경로 제거
                editEnd.setText(null);//text 삭제
                tMapPointEnd = null; //도착지 초기화
                listView.setVisibility(View.GONE); //listview 숨기기
            }
        });
        /* 현위치를 출발지로 설정 버튼 */
        TextView tvSetLocStart = (TextView)  findViewById(R.id.tv_setLocStart);
        tvSetLocStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TMapPoint tpoint = tMapView.getLocationPoint();
                double Latitude_ = tpoint.getLatitude(); //위도
                double Longitude_ = tpoint.getLongitude(); //경도
                String address_ = getCurrentAddress(Latitude_,Longitude_); //주소

                tMapView.setCenterPoint(Longitude_, Latitude_); //현위치를 지도 중심

                LocationManager lm = (LocationManager)getSystemService(Context. LOCATION_SERVICE);
                editStart.setText(address_); //장소명 setText
                tMapPointStart = tMapView.getLocationPoint(); //출발지에 현위치 좌표 넣기
                tMapView.removeAllMarkerItem(); //마커 안보이게
            }
        });
        /* 현위치 보기 버튼 */
        TextView tvMoveLoc = (TextView)  findViewById(R.id.tv_moveLoc);
        tvMoveLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TMapPoint tpoint = tMapView.getLocationPoint();
                double Latitude_ = tpoint.getLatitude(); //위도
                double Longitude_ = tpoint.getLongitude(); //경도
                tMapView.setCenterPoint(Longitude_, Latitude_);
            }
        });
        /* 길찾기 버튼 */
        Button btnDraw = (Button) findViewById(R.id.btn_draw);
        btnDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tMapView.removeAllMarkerItem(); //마커 안보이게
                //출발지 or 도착지가 null일 때
                if (tMapPointStart != null && tMapPointEnd != null) {
                    drawCashPath(tMapPointStart, tMapPointEnd);
                } else if (tMapPointStart == null && tMapPointEnd != null) {
                    Toast.makeText(getApplicationContext(), "출발지를 입력해주세요", Toast.LENGTH_SHORT).show();
                } else if (tMapPointStart != null && tMapPointEnd == null) {
                    Toast.makeText(getApplicationContext(), "도착지를 입력해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "출발지와 도착지를 입력해주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //listview 클릭 이벤트
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                POI poi = (POI) listView.getItemAtPosition(position);
                moveMap(poi.item.getPOIPoint().getLatitude(), poi.item.getPOIPoint().getLongitude());
                lat = poi.item.getPOIPoint().getLatitude();
                lon = poi.item.getPOIPoint().getLongitude();
                //Log.e("선택된 좌표", "lat : " + String.valueOf(lat) + " lon : " +lon);

                if (startBtnState == true) { //출발지 선택
                    tMapPointStart = new TMapPoint(poi.item.getPOIPoint().getLatitude(), poi.item.getPOIPoint().getLongitude());
                    startBtnState = false;
                    editStart.setText(poi.toString()); //장소명 setText
                    editStart.clearFocus(); //포커스 없앰
                } else if (endBtnState == true) { //도착지 선택
                    tMapPointEnd = new TMapPoint(poi.item.getPOIPoint().getLatitude(), poi.item.getPOIPoint().getLongitude());
                    endBtnState = false;
                    editEnd.setText(poi.toString()); //장소명 setText
                    editEnd.clearFocus(); //포커스 없앰
                }

                listView.setVisibility(View.GONE); //주소 선택 후 listview 안보이게
            }
        });

        //우선 경로 설정 부분
        item = new String[]{"원하는 경로를 선택해주세요 (추천경로)","추천경로", "무료도로", "최소시간" , "편한길 우선",
                "고속도로 우선", "최단거리", "이륜차 통행가능", "어린이 보호"};
        spinner = (Spinner) findViewById(R.id.spinner_field);
        //문자열 배열과 기본 스피너 레이아웃을 사용하여 ArrayAdapter 만들기
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, item);
        //선택목록이 나타날때 사용할 레이아웃을 지정
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //스피너에 어댑터 적용
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(getApplicationContext(), categories[position], Toast.LENGTH_SHORT).show();
                if(spinner.getSelectedItemPosition() > 0){
                    //선택된 항목
                    routeNum = spinner.getSelectedItemPosition();
                    if(routeNum == 1) {choiceRoute = 0;}
                    else if(routeNum == 2) {choiceRoute = 1;}
                    else if(routeNum == 3) {choiceRoute = 2;}
                    else if(routeNum == 4) {choiceRoute = 3;}
                    else if(routeNum == 5) {choiceRoute = 4;}
                    else if(routeNum == 6) {choiceRoute = 10;}
                    else if(routeNum == 7) {choiceRoute = 12;}
                    else if(routeNum == 8) {choiceRoute = 19;}
                    Log.v("알림",routeNum + spinner.getSelectedItem().toString()+ "is selected");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //choiceRoute = 0;
            }
        });
    } // -- onCreate()



    //상태바 투명 & 아이콘 회색 설정 함수
    private void setStateBar() {
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        View view = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (view != null) {
                // 23 버전 이상일 때 상태바 하얀 색상에 회색 아이콘 색상을 설정
                view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                getWindow().setStatusBarColor(Color.parseColor("#f2f2f2"));
            }
        } else if (Build.VERSION.SDK_INT >= 21) {
            // 21 버전 이상일 때
            getWindow().setStatusBarColor(Color.BLACK);
        }
    }

    //TMapAPI 활용(지도, 현재위치)
    private void setTMap() {
        /* 지도 부분 */
        LinearLayout linearLayoutTmap = (LinearLayout) findViewById(R.id.tmap);
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(AppKey);
        linearLayoutTmap.addView(tMapView);
        /* 현위치 아이콘표시 */
        tMapView.setIconVisibility(true);
        /* 줌레벨 */
        tMapView.setZoomLevel(15);
        tMapView.setMapType(TMapView.MAPTYPE_STANDARD);
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN);
        /*현재 위치 마커 커스텀*/
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.custom_poi_marker_end);
        //tMapView.setIcon(bitmap);
    }


    // 경로 그리는 함수
    public void drawCashPath(TMapPoint tMapPointStart, TMapPoint tMapPointEnd) {
        TMapData tmapdata = new TMapData();
        ArrayList passList = new ArrayList<>();
        //자동차 다중 경로
        tmapdata.findPathDataWithType(TMapData.TMapPathType.CAR_PATH, tMapPointStart, tMapPointEnd, null, choiceRoute, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine polyLine) {
                polyLine.setLineColor(Color.BLUE); // Color.rgb(85, 90, 181)
                tMapView.addTMapPath(polyLine);
                realdistance = polyLine.getDistance(); //거리
            }
        });
    }

    //주소 검색
    private void searchPOI() {
        TMapData data = new TMapData();
        listView.setVisibility(View.VISIBLE); //listview 보이게

        if (!TextUtils.isEmpty(keyword)) {
            data.findAllPOI(keyword, new TMapData.FindAllPOIListenerCallback() {
                @Override
                public void onFindAllPOI(final ArrayList<TMapPOIItem> arrayList) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tMapView.removeAllMarkerItem();
                            mAdapter.clear();

                            for (TMapPOIItem poi : arrayList) {
                                addMarker(poi);
                                mAdapter.add(new POI(poi));
                            }

                            if (arrayList.size() > 0) {
                                TMapPOIItem poi = arrayList.get(0);
                                moveMap(poi.getPOIPoint().getLatitude(), poi.getPOIPoint().getLongitude());
                            }
                        }
                    });
                }
            });
        }
    }

    //마커 표시 함수
    public void addMarker(TMapPOIItem poi) {
        TMapMarkerItem item = new TMapMarkerItem();
        item.setTMapPoint(poi.getPOIPoint());
        Bitmap icon = ((BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.custom_poi_marker_selected)).getBitmap();
        item.setIcon(icon);
        item.setPosition(0.5f, 1);
        item.setCalloutTitle(poi.getPOIName());
        //item.setCalloutSubTitle(poi.getPOIContent());
        item.setCanShowCallout(true);
        tMapView.addMarkerItem(poi.getPOIID(), item);
    }

    //해당 좌표로 지도 이동 함수
    private void moveMap(double lat, double lng) {
        tMapView.setCenterPoint(lng, lat);
    }

    //좌표를 주소로 변환하는 함수
    public String getCurrentAddress(double latitude, double longitude) {
        //지오코더 - GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";
        }
        Address address = addresses.get(0);
        return address.getAddressLine(0).toString() + "\n";
    }

    //뒤로가기 - listview 지우기, 앱 종료
    public void onBackPressed() {
        //super.onBackPressed(); // 기존 뒤로 가기 버튼의 기능을 막기 위해 주석 처리 또는 삭제

        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간에 2.5초를 더해 현재 시간과 비교 후 마지막으로 뒤로 가기 버튼을 눌렀던 시간이 2.5초가 지났으면 Toast 출력
        if (System.currentTimeMillis() > backKeyPressedTime + 2500) {
            backKeyPressedTime = System.currentTimeMillis();
            listView.setVisibility(View.GONE); //주소 선택 후 listview 안보이게
            return;
        }
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간에 2.5초를 더해 현재 시간과 비교 후 마지막으로 뒤로 가기 버튼을 눌렀던 시간이 2.5초가 지나지 않았으면 종료
        if (System.currentTimeMillis() <= backKeyPressedTime + 2500) {
            finish();
        }
    }

}
