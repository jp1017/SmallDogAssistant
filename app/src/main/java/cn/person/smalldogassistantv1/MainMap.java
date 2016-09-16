package cn.person.smalldogassistantv1;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.person.smalldogassistantv1.locationservice.LocationService;
import cn.person.smalldogassistantv1.navigation.NavigationAty;
import cn.person.smalldogassistantv1.personelaty.PersonelAty;
import cn.person.smalldogassistantv1.staticdata.StaticData;
import cn.person.smalldogassistantv1.voiceservice.VoiceService;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.MyTrafficStyle;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.overlay.BusRouteOverlay;
import com.amap.api.maps.overlay.DrivingRouteOverlay;
import com.amap.api.maps.overlay.WalkRouteOverlay;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.WalkPath;

public class MainMap extends Activity implements OnClickListener,
		ServiceConnection {
	private MapView mapView;
	private AMap aMap;
	private UiSettings mUiSettings;
	private ImageButton btn_traffic_condition;
	// private ImageButton btn_ride_navigation;
	private boolean select = true;
	private LocationService locationservice = null;
	private PopupWindow mPopupWindow = null;
	private PopupWindow PopupWindowlayer = null;
	private View vPop = null;
	private View vPop_layer = null;
	private Button btn_clock;
	private ImageButton btn_zoom_in, btn_zoom_out, btn_layer;// btn_box;
	private Button btn_begin_record, btn_clear_route, btn_personel_record,
			btn_cancel, btn_map_normal, btn_map_satellite, btn_map_3d,
			btn_route;
	private RelativeLayout startup;
	private LinearLayout record_panel;
	private TextView speedtext, timetext;

	private float totaldistance = 0;
	private float maxspeed = 0;
	private boolean start = false;
	private boolean change = false;
	private boolean layselect = false;
	private long lastClickTime = 0;
	private long h, m, s, t;
	// *************************绑定service相关操作*****************
	private Intent ServiceIntent = null;
	private double latitude = 0;
	private double longitude = 0;
	private double mlatitude = 0;
	private double mlongitude = 0;
	private double speed = 0;
	private LatLng latlng;
	private boolean closeinit = true;
	private int mapmode = 0;
	private DecimalFormat df = new DecimalFormat("#0.00");
	private int duration = 0;
	private VoiceService voice = null;
	private String provider = null;
	// *************************设置监听器********************
	private IntentFilter filter = new IntentFilter(
			StaticData.LOCATION_DATA_BRODCAST_INTENT);
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			provider = intent.getStringExtra(StaticData.PROVIDER);
			if (latitude != intent.getDoubleExtra(StaticData.LATITUDE, 0)
					&& longitude != intent.getDoubleExtra(StaticData.LONGITUDE,
							0)) {
				if (start) {
					latlng = new LatLng(latitude, longitude);
					LatLng temp = new LatLng(intent.getDoubleExtra(
							StaticData.LATITUDE, 0), intent.getDoubleExtra(
							StaticData.LONGITUDE, 0));

					float temp_distance = AMapUtils.calculateLineDistance(
							latlng, temp);
					if (provider.equals("lbs")) {
						if (temp_distance <= 60) {
							speed = temp_distance / 5 * 3.6;
							totaldistance += temp_distance / 1000;
							voice.playText("当前速度为" + df.format(speed) + "千米每小时");
						} else {
							speed = temp_distance / 5 * 3.6;
							totaldistance += temp_distance / 1000;
							voice.playText("当前速度为" + df.format(speed)
									+ "千米每小时,速度有些过快");
						}
					} else if (provider.equals("gps")) {
						speed = intent.getDoubleExtra(StaticData.SPEED, 0);
						voice.playText("当前速度为" + df.format(speed) + "千米每小时");
					}
					if (speed >= maxspeed) {
						maxspeed = (float) speed;
					}
					speedtext.setText("速度：" + df.format(speed) + "km/h");
					DrawRideTrace(latlng, temp);
				}

				latitude = intent.getDoubleExtra(StaticData.LATITUDE, 0);
				longitude = intent.getDoubleExtra(StaticData.LONGITUDE, 0);
			}
			if (start) {
				t++;
				h = t / 3600;
				m = t % 3600 / 60;
				s = t % 3600 % 60;
				timetext.setText("时间：" + h + "h:" + m + "m:" + s + "s");
				if (m == 10) {
					voice.playText("您已经骑行" + df.format(totaldistance) + "千米");
				}
			}
			mlatitude = intent.getDoubleExtra(StaticData.LATITUDE, 0);
			mlongitude = intent.getDoubleExtra(StaticData.LONGITUDE, 0);
			if (closeinit && locationservice != null) {
				duration++;
				setLocationIcon();
				if (duration == 6) {
					duration = 0;
					closeinit = false;
					startup.setVisibility(View.GONE);
				}
			}
		}
	};

	// **********************电源管理机制****************************
	private WakeLock wakeLock = null;

	private void acquireWakeLock() {
		if (wakeLock == null) {
			System.out.println("Get lock");
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this
					.getClass().getCanonicalName());
			wakeLock.acquire();
		}

	}

	private void releaseWakeLock() {
		if (wakeLock != null && wakeLock.isHeld()) {
			System.out.println("Release LOCK");
			wakeLock.release();
			wakeLock = null;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// R 需要引用包import com.amapv2.apis.R;
		setContentView(R.layout.main_map);
		mapView = (MapView) findViewById(R.id.map);
		mapView.onCreate(savedInstanceState);// 必须要写
		init();
	}

	private void init() {
		if (aMap == null) {
			aMap = mapView.getMap();
		}
		setVolumeControlStream(AudioManager.STREAM_MUSIC);// 设置声音控制
		initComponent();
		setMapUi();
		mstartService();
		setTraffic();
		getPopView();
		getMaplayer();
	}

	private void mstartService() {
		if (ServiceIntent == null) {
			ServiceIntent = new Intent(MainMap.this, LocationService.class);
		}
		bindService(ServiceIntent, this, Context.BIND_AUTO_CREATE);
	}

	private void mstopService() {
		unbindService(this);
		ServiceIntent = null;
	}

	private void initComponent() {
		startup = (RelativeLayout) findViewById(R.id.start_up_picture);
		record_panel = (LinearLayout) findViewById(R.id.record_panel);
		record_panel.setVisibility(View.GONE);

		speedtext = (TextView) findViewById(R.id.speed_text);
		timetext = (TextView) findViewById(R.id.time_text);
		btn_traffic_condition = (ImageButton) findViewById(R.id.btn_traffic_condition);
		btn_traffic_condition.setOnClickListener(this);

		btn_clock = (Button) findViewById(R.id.btn_clock);
		btn_clock.setOnClickListener(this);

		btn_zoom_in = (ImageButton) findViewById(R.id.btn_zoom_in);
		btn_zoom_in.setOnClickListener(this);

		btn_zoom_out = (ImageButton) findViewById(R.id.btn_zoom_out);
		btn_zoom_out.setOnClickListener(this);

		btn_layer = (ImageButton) findViewById(R.id.btn_layer);
		btn_layer.setOnClickListener(this);

		btn_cancel = (Button) findViewById(R.id.btn_cancel);
		btn_cancel.setOnClickListener(this);
		btn_cancel.setVisibility(View.GONE);

		btn_route = (Button) findViewById(R.id.btn_route);
		btn_route.setOnClickListener(this);

		// btn_box = (ImageButton) findViewById(R.id.btn_box);
		// btn_box.setOnClickListener(this);
	}

	// 配置mapUI界面
	private void setMapUi() {
		mUiSettings = aMap.getUiSettings();
		mUiSettings.setMyLocationButtonEnabled(false);
		mUiSettings.setZoomControlsEnabled(false);
		mUiSettings.setLogoPosition(AMapOptions.LOGO_POSITION_BOTTOM_LEFT);
		mUiSettings.setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
		// 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种

	}

	private void setLocationIcon() {
		// 自定义系统定位小蓝点
		MyLocationStyle myLocationStyle = new MyLocationStyle();
		myLocationStyle.myLocationIcon(BitmapDescriptorFactory
				.fromResource(R.drawable.location_marker));
		myLocationStyle.strokeColor(Color.TRANSPARENT);// 设置圆形的边框颜色
		myLocationStyle.radiusFillColor(Color.argb(100, 0, 0, 180));// 设置圆形的填充颜色
		myLocationStyle.strokeWidth(0.1f);// 设置圆形的边框粗细
		aMap.setMyLocationStyle(myLocationStyle);
		aMap.setLocationSource(locationservice);
		aMap.setMyLocationEnabled(true);
		getMapMode();
	}

	// 配置显示道路情况的一些设置
	private void setTraffic() {
		MyTrafficStyle myTrafficStyle = new MyTrafficStyle();
		myTrafficStyle.setSeriousCongestedColor(0xff92000a);
		myTrafficStyle.setCongestedColor(0xffea0312);
		myTrafficStyle.setSlowColor(0xffff7508);
		myTrafficStyle.setSmoothColor(0xff00a209);
		aMap.setMyTrafficStyle(myTrafficStyle);

	}

	// ************************对于相关后台Service连接的操作*********************
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		// 取得location service实例
		locationservice = ((LocationService.LocationServiceBinder) service)
				.getLocationService();
		voice = locationservice.getVoice();
		// ClearSQL();
	}

	// 在service崩溃时触发
	@Override
	public void onServiceDisconnected(ComponentName name) {
		// TODO Auto-generated method stub

	}

	// ********************************popupwindow初始化************************
	private void getPopView() {
		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		vPop = inflater.inflate(R.layout.clock_list_menu, null, false);
		vPop.setFocusable(true);
		vPop.setFocusableInTouchMode(true);

		btn_begin_record = (Button) vPop.findViewById(R.id.btn_begin_record);
		btn_begin_record.setOnClickListener(this);

		btn_clear_route = (Button) vPop.findViewById(R.id.btn_clear_route);
		btn_clear_route.setOnClickListener(this);

		btn_personel_record = (Button) vPop
				.findViewById(R.id.btn_personel_record);
		btn_personel_record.setOnClickListener(this);

		mPopupWindow = new PopupWindow(vPop, LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT, true);
		mPopupWindow.setAnimationStyle(R.style.popupstyle);
		mPopupWindow.setFocusable(true);

		vPop.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mPopupWindow != null && mPopupWindow.isShowing()) {
					change = false;
					btn_clock.setVisibility(View.VISIBLE);
					btn_cancel.setVisibility(View.GONE);
					mPopupWindow.dismiss();
					return true;
				}
				return false;
			}
		});
		vPop.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (mPopupWindow != null && mPopupWindow.isShowing()) {
						change = false;
						btn_clock.setVisibility(View.VISIBLE);
						btn_cancel.setVisibility(View.GONE);
						mPopupWindow.dismiss();
						return true;
					}
				}
				return false;
			}
		});
	}

	private void getMaplayer() {
		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		vPop_layer = inflater.inflate(R.layout.map_layer_popup, null, false);
		vPop_layer.setFocusable(true);
		vPop_layer.setFocusableInTouchMode(true);

		btn_map_normal = (Button) vPop_layer.findViewById(R.id.btn_map_normal);
		btn_map_normal.setOnClickListener(this);

		btn_map_satellite = (Button) vPop_layer
				.findViewById(R.id.btn_map_satellite);
		btn_map_satellite.setOnClickListener(this);

		btn_map_3d = (Button) vPop_layer.findViewById(R.id.btn_map_3d);
		btn_map_3d.setOnClickListener(this);

		PopupWindowlayer = new PopupWindow(vPop_layer,
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
		PopupWindowlayer.setAnimationStyle(R.style.maplayerstyle);
		PopupWindowlayer.setFocusable(true);

		vPop_layer.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (PopupWindowlayer != null && PopupWindowlayer.isShowing()) {
					PopupWindowlayer.dismiss();
					layselect = false;
					btn_layer.setImageDrawable(getResources().getDrawable(
							R.drawable.btn_layer_48));
					return true;
				}
				return false;
			}
		});
		vPop_layer.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (PopupWindowlayer != null
							&& PopupWindowlayer.isShowing()) {
						layselect = false;
						btn_layer.setImageDrawable(getResources().getDrawable(
								R.drawable.btn_layer_48));
						PopupWindowlayer.dismiss();
						return true;
					}
				}
				return false;
			}
		});
	}

	// **************************按键监听相关设置*****************************
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_layer:
			PopupWindowlayer.showAsDropDown(btn_layer);
			if (layselect == false) {
				layselect = true;
				btn_layer.setImageDrawable(getResources().getDrawable(
						R.drawable.btn_cancel));
			}
			break;
		case R.id.btn_traffic_condition:
			if (select) {
				ToastShow("实时路况已开启");
				btn_traffic_condition.setImageDrawable(getResources()
						.getDrawable(R.drawable.main_roadcondition_on));
				aMap.setTrafficEnabled(true);
				select = false;
			} else {
				ToastShow("实时路况已关闭");
				btn_traffic_condition.setImageDrawable(getResources()
						.getDrawable(R.drawable.main_roadcondition_off));
				aMap.setTrafficEnabled(false);
				select = true;
			}
			break;
		case R.id.btn_zoom_in:
			aMap.moveCamera(CameraUpdateFactory.zoomIn());
			break;
		case R.id.btn_zoom_out:
			aMap.moveCamera(CameraUpdateFactory.zoomOut());
			break;
		case R.id.btn_clock:
			int cy = btn_clock.getHeight();
			if (change == false) {
				change = true;
				btn_clock.setVisibility(View.GONE);
				btn_cancel.setVisibility(View.VISIBLE);
			}
			mPopupWindow.showAtLocation(btn_clock, Gravity.BOTTOM
					| Gravity.LEFT, 0, cy);
			break;
		case R.id.btn_route:
			Intent pathsearch = new Intent();
			Bundle bn = new Bundle();
			if (mlatitude != 0 && mlongitude != 0) {
				bn.putDouble(StaticData.MLATITUDE, mlatitude);
				bn.putDouble(StaticData.MLONGITUDE, mlongitude);
			} else {
				ToastShow("现在无法进行路线查询，请检查网络");
			}
			pathsearch.putExtras(bn);
			pathsearch.setClass(MainMap.this, NavigationAty.class);
			startActivityForResult(pathsearch, StaticData.REQUEST_PATH_SEARCH);
			break;

		// case R.id.btn_box:
		// Intent off = new Intent(MainMap.this, OffLineMap.class);
		// startActivity(off);
		// break;

		case R.id.btn_begin_record:
			if (start == false) {
				start = true;
				btn_begin_record.setText("停止记录");
				record_panel.setVisibility(View.VISIBLE);
			} else {
				saveRelatedData();
				start = false;
				h = 0;
				m = 0;
				s = 0;
				t = 0;
				maxspeed = 0;
				speed = 0;
				totaldistance = 0;
				btn_begin_record.setText("开始记录");
				record_panel.setVisibility(View.GONE);
			}
			break;
		case R.id.btn_clear_route:
			aMap.clear();
			if (locationservice != null) {
				setLocationIcon();
			}
			break;
		case R.id.btn_personel_record:
			if (start)
				saveRelatedData();
			Intent perIntent = new Intent();
			perIntent.setClass(MainMap.this, PersonelAty.class);
			startActivityForResult(perIntent, StaticData.REQUEST_PERSON_RECORD);
			break;
		case R.id.btn_map_normal:
			selectMapLayer(R.id.btn_map_normal);
			break;
		case R.id.btn_map_satellite:
			selectMapLayer(R.id.btn_map_satellite);
			break;
		case R.id.btn_map_3d:
			selectMapLayer(R.id.btn_map_3d);
			break;
		}
	}

	private void getMapMode() {
		SharedPreferences pref = getSharedPreferences(
				StaticData.SMALL_DOG_PREFERENCE, 0);
		int i;
		i = pref.getInt(StaticData.MAP_MODE, 0);
		switch (i) {
		case 0:
			aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);
			aMap.setMapType(AMap.MAP_TYPE_NORMAL);
			btn_map_normal.setBackgroundColor(getResources().getColor(
					R.color.orange));
			btn_map_satellite.setBackgroundColor(R.drawable.btn_background);
			btn_map_3d.setBackgroundColor(R.drawable.btn_background);
			break;
		case 1:
			aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);
			aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
			btn_map_normal.setBackgroundColor(R.drawable.btn_background);
			btn_map_satellite.setBackgroundColor(getResources().getColor(
					R.color.orange));
			btn_map_3d.setBackgroundColor(R.drawable.btn_background);
			break;
		case 2:
			aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_ROTATE);
			aMap.setMapType(AMap.MAP_TYPE_NORMAL);
			btn_map_normal.setBackgroundColor(R.drawable.btn_background);
			btn_map_satellite.setBackgroundColor(R.drawable.btn_background);
			btn_map_3d.setBackgroundColor(getResources().getColor(
					R.color.orange));
			break;
		}
	}

	private void selectMapLayer(int i) {
		SharedPreferences pref = getSharedPreferences(
				StaticData.SMALL_DOG_PREFERENCE, 0);
		Editor pref_editor = pref.edit();
		switch (i) {
		case R.id.btn_map_normal:
			mapmode = 0;
			pref_editor.putInt(StaticData.MAP_MODE, mapmode);
			aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);
			aMap.setMapType(AMap.MAP_TYPE_NORMAL);
			btn_map_normal.setBackgroundColor(getResources().getColor(
					R.color.orange));
			btn_map_satellite.setBackgroundColor(R.drawable.btn_background);
			btn_map_3d.setBackgroundColor(R.drawable.btn_background);
			pref_editor.commit();
			break;
		case R.id.btn_map_satellite:
			mapmode = 1;
			pref_editor.putInt(StaticData.MAP_MODE, mapmode);
			aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);
			aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
			btn_map_normal.setBackgroundColor(R.drawable.btn_background);
			btn_map_satellite.setBackgroundColor(getResources().getColor(
					R.color.orange));
			btn_map_3d.setBackgroundColor(R.drawable.btn_background);
			pref_editor.commit();
			break;
		case R.id.btn_map_3d:
			mapmode = 2;
			pref_editor.putInt(StaticData.MAP_MODE, mapmode);
			aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_ROTATE);
			aMap.setMapType(AMap.MAP_TYPE_NORMAL);
			btn_map_normal.setBackgroundColor(R.drawable.btn_background);
			btn_map_satellite.setBackgroundColor(R.drawable.btn_background);
			btn_map_3d.setBackgroundColor(getResources().getColor(
					R.color.orange));
			pref_editor.commit();
			break;
		}
	}

	private void ToastShow(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	// *******************对后退按键的处理**********************
	@Override
	public void onBackPressed() {
		if (lastClickTime <= 0) {
			ToastShow("再按一次后退键退出应用");
			lastClickTime = System.currentTimeMillis();
		} else {
			long currentClickTime = System.currentTimeMillis();
			if (currentClickTime - lastClickTime < 1000) {
				finish();
			} else {
				ToastShow("再按一次后退键退出应用");
				lastClickTime = currentClickTime;
			}
		}
	}

	// ***********************声明周期处理*********************
	@Override
	protected void onResume() {
		super.onResume();
		acquireWakeLock();
		registerReceiver(receiver, filter);
		mapView.onResume();
	}

	@Override
	protected void onPause() {
		releaseWakeLock();
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
		System.out.println("onDestroy");
		unregisterReceiver(receiver);
		mstopService();
		super.onDestroy();
		mapView.onDestroy();
	}

	// **************************记录轨迹函数**********************
	private void DrawRideTrace(LatLng point1, LatLng point2) {
		if (aMap != null) {
			aMap.addPolyline((new PolylineOptions()).add(point1, point2)
					.geodesic(false).color(R.color.blueviolet));
		}
	}

	private void PlotBusRoute(LatLonPoint startpoint, LatLonPoint endpoint,
			BusPath buspath) {
		aMap.clear();// 清理地图上的所有覆盖物
		setLocationIcon();
		BusRouteOverlay routeOverlay = new BusRouteOverlay(this, aMap, buspath,
				startpoint, endpoint);
		routeOverlay.removeFromMap();
		routeOverlay.addToMap();
		routeOverlay.zoomToSpan();
	}

	private void PlotCarRoute(LatLonPoint startpoint, LatLonPoint endpoint,
			DrivePath drivepath) {
		aMap.clear();// 清理地图上的所有覆盖物
		setLocationIcon();
		DrivingRouteOverlay routeOverlay = new DrivingRouteOverlay(this, aMap,
				drivepath, startpoint, endpoint);
		routeOverlay.removeFromMap();
		routeOverlay.addToMap();
		routeOverlay.zoomToSpan();
	}

	private void PlotManRoute(LatLonPoint startpoint, LatLonPoint endpoint,
			WalkPath walkpath) {
		aMap.clear();// 清理地图上的所有覆盖物
		setLocationIcon();
		WalkRouteOverlay routeOverlay = new WalkRouteOverlay(this, aMap,
				walkpath, startpoint, endpoint);
		routeOverlay.removeFromMap();
		routeOverlay.addToMap();
		routeOverlay.zoomToSpan();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case StaticData.REQUEST_PATH_SEARCH:// 进行路线搜索
			if (data != null) {
				Bundle re_Budle = data.getExtras();
				if (re_Budle != null) {

					double endpoint_latitude = re_Budle
							.getDouble(StaticData.END_LATITUDE);
					double endpoint_longitude = re_Budle
							.getDouble(StaticData.END_LONGITUDE);

					double startpoint_latitude = re_Budle
							.getDouble(StaticData.START_LATITUDE);
					double startpoint_longitude = re_Budle
							.getDouble(StaticData.START_LONGITUDE);

					LatLonPoint startpoint = new LatLonPoint(
							startpoint_latitude, startpoint_longitude);
					LatLonPoint endpoint = new LatLonPoint(endpoint_latitude,
							endpoint_longitude);

					switch (resultCode) {
					case 0:
						BusPath buspath = re_Budle
								.getParcelable(StaticData.BUS_PATH);
						PlotBusRoute(startpoint, endpoint, buspath);
						break;
					case 1:
						DrivePath carpath = re_Budle
								.getParcelable(StaticData.CAR_PATH);
						PlotCarRoute(startpoint, endpoint, carpath);
						break;
					case 2:
						WalkPath manpath = re_Budle
								.getParcelable(StaticData.WALK_PATH);
						PlotManRoute(startpoint, endpoint, manpath);
						break;
					}
				}
			}
		}
	}

	private void saveRelatedData() {
		SharedPreferences pre = getSharedPreferences(
				StaticData.SMALL_DOG_PREFERENCE, 0);
		Editor pre_edit = pre.edit();
		pre_edit.putLong(StaticData.H, h);
		pre_edit.putLong(StaticData.M, m);
		pre_edit.putLong(StaticData.S, s);
		pre_edit.putFloat(StaticData.TOTAL_DISTANCE, totaldistance);
		pre_edit.putFloat(StaticData.MAX_SPEED, maxspeed);
		pre_edit.commit();
	}
}
