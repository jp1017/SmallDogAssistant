package cn.person.smalldogassistantv1.navigation;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import cn.person.smalldogassistantv1.R;
import cn.person.smalldogassistantv1.staticdata.StaticData;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiItemDetail;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.amap.api.services.poisearch.PoiSearch.OnPoiSearchListener;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.RouteSearch.BusRouteQuery;
import com.amap.api.services.route.RouteSearch.DriveRouteQuery;
import com.amap.api.services.route.RouteSearch.OnRouteSearchListener;
import com.amap.api.services.route.RouteSearch.WalkRouteQuery;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;

public class NavigationAty extends Activity implements OnClickListener,
		OnPoiSearchListener, OnRouteSearchListener, OnItemClickListener {
	private ImageButton btn_bus, btn_car, btn_walk, btn_back;
	private Button btn_clear_all, btn_route_search;
	private AutoCompleteTextView start_text, end_text;
	private LinearLayout mprogress;
	private ListView history;
	private HistroyAdapter adapter;
	private int mode = 2;
	private String startstr = null;
	private String endstr = null;

	// ***********************搜索相关参数************************
	private RouteSearch routeSearch;
	private PoiSearch.Query startSearchQuery;
	private PoiSearch.Query endSearchQuery;
	private BusRouteResult busRouteResult;// 公交模式查询结果
	private DriveRouteResult driveRouteResult;// 驾车模式查询结果
	private WalkRouteResult walkRouteResult;// 步行模式查询结果 @Override
	private int busMode = RouteSearch.BusDefault;// 公交默认模式
	private int drivingMode = RouteSearch.DrivingDefault;// 驾车默认模式
	private int walkMode = RouteSearch.WalkDefault;// 步行默认模式
	private LatLonPoint startPoint;
	private LatLonPoint endPoint;
	private double latitude;
	private double longitude;
	private boolean full = false;
	private int length = 0;
	private int index = 0;
	private String temp_start = null;
	private String temp_end = null;

	// ********************************************************
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.route_search_layer);
		init();
	}

	private void getStartPoint() {
		Intent recive = this.getIntent();
		Bundle rebundle = recive.getExtras();
		try {
			latitude = rebundle.getDouble(StaticData.MLATITUDE);
		} catch (Exception e) {

		}
		try {
			longitude = rebundle.getDouble(StaticData.MLONGITUDE);
		} catch (Exception e) {

		}
		if (latitude != 0 && longitude != 0) {
			startPoint = new LatLonPoint(latitude, longitude);
		} else {
			startPoint = null;
		}

	}

	private void init() {
		mprogress = (LinearLayout) findViewById(R.id.myprogress);
		mprogress.setVisibility(View.GONE);

		btn_back = (ImageButton) findViewById(R.id.btn_back_route);
		btn_back.setOnClickListener(this);

		btn_bus = (ImageButton) findViewById(R.id.btn_bus);
		btn_bus.setOnClickListener(this);

		btn_car = (ImageButton) findViewById(R.id.btn_car);
		btn_car.setOnClickListener(this);

		btn_walk = (ImageButton) findViewById(R.id.btn_walk);
		btn_walk.setOnClickListener(this);

		start_text = (AutoCompleteTextView) findViewById(R.id.Edit_start_text);
		end_text = (AutoCompleteTextView) findViewById(R.id.Edit_end_text);

		btn_clear_all = (Button) findViewById(R.id.btn_clear_all);
		btn_clear_all.setOnClickListener(this);

		btn_route_search = (Button) findViewById(R.id.btn_route_search);
		btn_route_search.setOnClickListener(this);

		routeSearch = new RouteSearch(this);
		routeSearch.setRouteSearchListener(this);

		history = (ListView) findViewById(R.id.history_list);
		adapter = new HistroyAdapter(this);
		initListView();

		getStartPoint();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_bus:
			setBusMode();
			break;
		case R.id.btn_car:
			setCarMode();
			break;
		case R.id.btn_walk:
			setWalkMode();
			break;
		// 返回操作
		case R.id.btn_back_route:
			finish();
			break;
		case R.id.btn_clear_all:
			RemoveAllPreferences();
			break;
		case R.id.btn_route_search:
			RouteSearch();
			break;
		}

	}

	private void RouteSearch() {
		startstr = start_text.getText().toString().trim();
		endstr = end_text.getText().toString().trim();

		if (startPoint == null) {
			if (startstr == null || startstr.length() == 0) {
				ToastShow("未获取当前坐标，请手动输入起点");
				return;
			}
		}

		if (endstr == null || endstr.length() == 0) {
			ToastShow("请输入终点");
			return;
		}
		if (startstr.equals(endstr)) {
			ToastShow("起点与终点距离很近，您可以步行前往");
			return;
		}

		startSearchResult();
	}

	private void ProgressShow() {
		mprogress.setVisibility(View.VISIBLE);
	}

	private void ProgressDismiss() {
		mprogress.setVisibility(View.GONE);
	}

	private void setBusMode() {
		mode = 0;
		btn_bus.setImageDrawable(getResources().getDrawable(
				R.drawable.poi_bus_pressed));
		btn_car.setImageDrawable(getResources().getDrawable(
				R.drawable.poi_car_unpressed));
		btn_walk.setImageDrawable(getResources().getDrawable(
				R.drawable.poi_walk_unpressed));
	}

	private void setCarMode() {
		mode = 1;
		btn_bus.setImageDrawable(getResources().getDrawable(
				R.drawable.poi_bus_unpressed));
		btn_car.setImageDrawable(getResources().getDrawable(
				R.drawable.poi_car_pressed));
		btn_walk.setImageDrawable(getResources().getDrawable(
				R.drawable.poi_walk_unpressed));
	}

	private void setWalkMode() {
		mode = 2;
		btn_bus.setImageDrawable(getResources().getDrawable(
				R.drawable.poi_bus_unpressed));
		btn_car.setImageDrawable(getResources().getDrawable(
				R.drawable.poi_car_unpressed));
		btn_walk.setImageDrawable(getResources().getDrawable(
				R.drawable.poi_walk_pressed));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	private void ToastShow(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	// *********************列表中点击监听***********************
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		ProgressShow();
		HistoryPathItem data = adapter.getItem(position);

		startstr = data.getStartname();
		start_text.setText(startstr);
		endstr = data.getEndname();
		end_text.setText(endstr);

		if (data.getStartname().equals("当前位置")) {
			endSearchQuery = new PoiSearch.Query(endstr, "", "010"); // 第一个参数表示查询关键字，第二参数表示poi搜索类型，第三个参数表示城市区号或者城市名
			endSearchQuery.setPageNum(0);// 设置查询第几页，第一页从0开始
			endSearchQuery.setPageSize(20);// 设置每页返回多少条数据

			PoiSearch poiSearch = new PoiSearch(NavigationAty.this,
					endSearchQuery);
			poiSearch.setOnPoiSearchListener(this);
			poiSearch.searchPOIAsyn(); // 异步poi查询
		} else {
			startSearchResult();
		}

	}

	// 公交车搜索结果回调
	@Override
	public void onBusRouteSearched(BusRouteResult result, int rCode) {
		ProgressDismiss();
		if (rCode == 0) {
			if (result != null && result.getPaths() != null
					&& result.getPaths().size() > 0) {
				busRouteResult = result;
				StorageData(startstr, endstr);
				BusPath busPath = busRouteResult.getPaths().get(0);
				LatLonPoint tr_start = busRouteResult.getStartPos();
				LatLonPoint tr_end = busRouteResult.getTargetPos();

				double tr_latitude_start = tr_start.getLatitude();
				double tr_longitude_start = tr_start.getLongitude();

				double tr_latitude_end = tr_end.getLatitude();
				double tr_longitude_end = tr_end.getLongitude();
				// 发送消息
				Intent tr_Intent = new Intent();
				Bundle tr_Bundle = new Bundle();
				tr_Bundle.putParcelable(StaticData.BUS_PATH, busPath);
				tr_Bundle.putDouble(StaticData.START_LATITUDE,
						tr_latitude_start);
				tr_Bundle.putDouble(StaticData.START_LONGITUDE,
						tr_longitude_start);
				tr_Bundle.putDouble(StaticData.END_LATITUDE, tr_latitude_end);
				tr_Bundle.putDouble(StaticData.END_LONGITUDE, tr_longitude_end);

				tr_Intent.putExtras(tr_Bundle);

				setResult(StaticData.BUS_OK, tr_Intent);
				finish();

			} else {
				ToastShow("未搜索到结果");
			}
		} else if (rCode == 27) {
			ToastShow("网络错误");
		} else if (rCode == 32) {
			ToastShow("key不存在");
		} else {
			ToastShow("其他错误");
		}
	}

	// 自驾搜索结果回调
	@Override
	public void onDriveRouteSearched(DriveRouteResult result, int rCode) {
		ProgressDismiss();
		if (rCode == 0) {
			if (result != null && result.getPaths() != null
					&& result.getPaths().size() > 0) {
				driveRouteResult = result;
				StorageData(startstr, endstr);
				DrivePath drivePath = driveRouteResult.getPaths().get(0);
				// 发送消息
				LatLonPoint tr_start = driveRouteResult.getStartPos();
				LatLonPoint tr_end = driveRouteResult.getTargetPos();

				double tr_latitude_start = tr_start.getLatitude();
				double tr_longitude_start = tr_start.getLongitude();

				double tr_latitude_end = tr_end.getLatitude();
				double tr_longitude_end = tr_end.getLongitude();
				// 发送消息
				Intent tr_Intent = new Intent();
				Bundle tr_Bundle = new Bundle();
				tr_Bundle.putParcelable(StaticData.CAR_PATH, drivePath);
				tr_Bundle.putDouble(StaticData.START_LATITUDE,
						tr_latitude_start);
				tr_Bundle.putDouble(StaticData.START_LONGITUDE,
						tr_longitude_start);
				tr_Bundle.putDouble(StaticData.END_LATITUDE, tr_latitude_end);
				tr_Bundle.putDouble(StaticData.END_LONGITUDE, tr_longitude_end);

				tr_Intent.putExtras(tr_Bundle);

				setResult(StaticData.CAR_OK, tr_Intent);
				finish();
			} else {
				ToastShow("未搜索到结果");
			}
		} else if (rCode == 27) {
			ToastShow("网络错误");
		} else if (rCode == 32) {
			ToastShow("key不存在");
		} else {
			ToastShow("其他错误");
		}
	}

	// 步行搜索结果回调
	@Override
	public void onWalkRouteSearched(WalkRouteResult result, int rCode) {
		ProgressDismiss();
		if (rCode == 0) {
			if (result != null && result.getPaths() != null
					&& result.getPaths().size() > 0) {
				walkRouteResult = result;
				StorageData(startstr, endstr);
				WalkPath walkPath = walkRouteResult.getPaths().get(0);
				// 发送消息
				LatLonPoint tr_start = walkRouteResult.getStartPos();
				LatLonPoint tr_end = walkRouteResult.getTargetPos();

				double tr_latitude_start = tr_start.getLatitude();
				double tr_longitude_start = tr_start.getLongitude();

				double tr_latitude_end = tr_end.getLatitude();
				double tr_longitude_end = tr_end.getLongitude();
				// 发送消息
				Intent tr_Intent = new Intent();
				Bundle tr_Bundle = new Bundle();
				tr_Bundle.putParcelable(StaticData.WALK_PATH, walkPath);
				tr_Bundle.putDouble(StaticData.START_LATITUDE,
						tr_latitude_start);
				tr_Bundle.putDouble(StaticData.START_LONGITUDE,
						tr_longitude_start);
				tr_Bundle.putDouble(StaticData.END_LATITUDE, tr_latitude_end);
				tr_Bundle.putDouble(StaticData.END_LONGITUDE, tr_longitude_end);

				tr_Intent.putExtras(tr_Bundle);

				setResult(StaticData.WALK_OK, tr_Intent);
				finish();
			} else {
				ToastShow("未搜索到结果");
			}
		} else if (rCode == 27) {
			ToastShow("网络错误");
		} else if (rCode == 32) {
			ToastShow("key不存在");
		} else {
			ToastShow("其他错误");
		}
	}

	@Override
	public void onPoiItemDetailSearched(PoiItemDetail arg0, int arg1) {

	}

	// poi搜索结果回调
	@Override
	public void onPoiSearched(PoiResult result, int rCode) {
		ProgressDismiss();
		if (rCode == 0) {
			if (result != null && result.getQuery() != null
					&& result.getPois() != null && result.getPois().size() > 0) {
				if (result.getQuery().queryEquals(startSearchQuery)) {
					// 如果返回的结果为起始点搜索的结果
					List<PoiItem> poiItems = result.getPois();// 取得poiitem数据
					RouteSearchPoiDialog dialog = new RouteSearchPoiDialog(
							NavigationAty.this, poiItems);// 通过构造函数来构建dialog，ListView内容由搜索到的内容进行填充
					dialog.setTitle("您要找的起点是:");
					dialog.show();
					dialog.setOnListClickListener(new OnListItemClick() {

						@Override
						public void onListItemClick(
								RouteSearchPoiDialog dialog,
								PoiItem startpoiItem) {
							startPoint = startpoiItem.getLatLonPoint();
							startstr = startpoiItem.getTitle();
							start_text.setText(startstr);
							endSearchResult();// 开始搜终点
						}
					});
				} else if (result.getQuery().queryEquals(endSearchQuery)) {
					// 如果返回的结果为结束点搜索的结果
					List<PoiItem> poiItems = result.getPois();// 取得poiitem数据
					RouteSearchPoiDialog dialog = new RouteSearchPoiDialog(
							NavigationAty.this, poiItems);
					dialog.setTitle("您要找的终点是:");
					dialog.show();
					dialog.setOnListClickListener(new OnListItemClick() {
						public void onListItemClick(
								RouteSearchPoiDialog dialog, PoiItem endpoiItem) {
							endPoint = endpoiItem.getLatLonPoint();
							endstr = endpoiItem.getTitle();
							end_text.setText(endstr);
							// 进行路径规划搜索
							searchRouteResult(startPoint, endPoint);
						}
					});
				}
			} else {
				ToastShow("未搜索到结果");
			}
		} else if (rCode == 27) {
			ToastShow("网络错误");
		} else if (rCode == 32) {
			ToastShow("key不存在");
		} else {
			System.out.println(rCode);
			ToastShow("其他错误");
		}

	}

	private void startSearchResult() {
		startstr = start_text.getText().toString().trim();
		if (startstr != null && startstr.length() != 0) {
			ProgressShow();
			startSearchQuery = new PoiSearch.Query(startstr, "", "010"); // 第一个参数表示查询关键字，第二参数表示poi搜索类型，第三个参数表示城市区号或者城市名
			startSearchQuery.setPageNum(0);// 设置查询第几页，第一页从0开始
			startSearchQuery.setPageSize(20);// 设置每页返回多少条数据
			PoiSearch poiSearch = new PoiSearch(NavigationAty.this,
					startSearchQuery);
			poiSearch.setOnPoiSearchListener(this);
			poiSearch.searchPOIAsyn();// 异步poi查询
		} else if (startPoint != null) {
			endSearchResult();
		}
	}

	public void endSearchResult() {
		endstr = end_text.getText().toString().trim();
		if (endPoint != null && endstr.equals("地图上的终点")) {
			searchRouteResult(startPoint, endPoint);
		} else {
			ProgressShow();
			endSearchQuery = new PoiSearch.Query(endstr, "", "010"); // 第一个参数表示查询关键字，第二参数表示poi搜索类型，第三个参数表示城市区号或者城市名
			endSearchQuery.setPageNum(0);// 设置查询第几页，第一页从0开始
			endSearchQuery.setPageSize(20);// 设置每页返回多少条数据

			PoiSearch poiSearch = new PoiSearch(NavigationAty.this,
					endSearchQuery);
			poiSearch.setOnPoiSearchListener(this);
			poiSearch.searchPOIAsyn(); // 异步poi查询
		}
	}

	public void searchRouteResult(LatLonPoint start, LatLonPoint end) {
		ProgressShow();
		final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
				start, end);
		if (mode == 0) {// 公交路径规划
			BusRouteQuery query = new BusRouteQuery(fromAndTo, busMode, "北京", 0);// 第一个参数表示路径规划的起点和终点，第二个参数表示公交查询模式，第三个参数表示公交查询城市区号，第四个参数表示是否计算夜班车，0表示不计算
			routeSearch.calculateBusRouteAsyn(query);// 异步路径规划公交模式查询
		} else if (mode == 1) {// 驾车路径规划
			DriveRouteQuery query = new DriveRouteQuery(fromAndTo, drivingMode,
					null, null, "");// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
			routeSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询
		} else if (mode == 2) {// 步行路径规划
			WalkRouteQuery query = new WalkRouteQuery(fromAndTo, walkMode);
			routeSearch.calculateWalkRouteAsyn(query);// 异步路径规划步行模式查询
		}
	}

	// **************************相关历史记录存储功能************************
	public void StorageData(String startname, String endname) {
		SharedPreferences pref = getSharedPreferences(
				StaticData.SMALL_DOG_PREFERENCE, Context.MODE_PRIVATE);
		Editor e = pref.edit();

		int temp_length = 0;
		if (pref.getBoolean(StaticData.FULL, false) == true) {
			temp_length = StaticData.MAX_STORAGE_LENGTH;
		} else {
			temp_length = pref.getInt(StaticData.INDEX, 0);
		}

		boolean isSame = ExcludeSameItems(startname, endname, temp_length, pref);

		if (isSame == false) {
			index = pref.getInt(StaticData.INDEX, 0);
			if (startname == null || startname.length() == 0) {
				e.putString(StaticData.START_NAME + index, "当前位置");
			} else {
				e.putString(StaticData.START_NAME + index, startname);
			}
			e.putString(StaticData.END_NAME + index, endname);
			// SharedPrefrences中最多能存的条数为MAX_STORAGE_LENGTH长度为30，超过之后从头覆盖
			index++;
			if (index == StaticData.MAX_STORAGE_LENGTH) {
				full = true;
				e.putBoolean(StaticData.FULL, full);
			}
			index = index % (StaticData.MAX_STORAGE_LENGTH);
			e.putInt(StaticData.INDEX, index);
			e.commit();
		}
	}

	public boolean ExcludeSameItems(String startname, String endname,
			int length, SharedPreferences pref) {
		String temp = null;
		if (startname == null || startname.length() == 0) {
			temp = "当前位置";
		} else {
			temp = startname;
		}
		for (int i = 0; i < length; i++) {
			temp_start = pref.getString(StaticData.START_NAME + i, null);
			temp_end = pref.getString(StaticData.END_NAME + i, null);
			if (temp.equals(temp_start) && endname.equals(temp_end)) {
				return true;
			}
		}
		return false;
	}

	private void RemoveAllPreferences() {
		SharedPreferences pref = getSharedPreferences(
				StaticData.SMALL_DOG_PREFERENCE, Context.MODE_PRIVATE);
		Editor e = pref.edit();
		int index = pref.getInt(StaticData.INDEX, 0);
		boolean full = pref.getBoolean(StaticData.FULL, false);
		int length = 0;
		if (full == true) {
			length = StaticData.MAX_STORAGE_LENGTH;
		} else {
			length = index;
		}
		// 寻找搜索
		for (int i = 0; i < length; i++) {
			if (adapter.isEmpty() == false)
				adapter.removeItem(0);
			if (pref.contains(StaticData.START_NAME + i)) {
				e.remove(StaticData.START_NAME + i);
				e.remove(StaticData.END_NAME + i);
			}
		}

		index = 0;
		full = false;
		e.putBoolean(StaticData.FULL, full);
		e.putInt(StaticData.INDEX, index);
		e.commit();
	}

	private void initListView() {
		SharedPreferences pref = getSharedPreferences(
				StaticData.SMALL_DOG_PREFERENCE, Context.MODE_PRIVATE);
		if (pref.getInt(StaticData.INDEX, 0) != 0
				|| pref.getBoolean(StaticData.FULL, false) == true) {

			if (pref.getBoolean(StaticData.FULL, false) == true) {
				length = StaticData.MAX_STORAGE_LENGTH;
			} else {
				length = pref.getInt(StaticData.INDEX, 0);
			}
			for (int i = 0; i < length; i++) {
				HistoryPathItem data = new HistoryPathItem(pref.getString(
						StaticData.START_NAME + i, null), pref.getString(
						StaticData.END_NAME + i, null));
				adapter.addData(data);
			}
			history.setAdapter(adapter);
		}
		history.setOnItemClickListener(this);
	}
}
