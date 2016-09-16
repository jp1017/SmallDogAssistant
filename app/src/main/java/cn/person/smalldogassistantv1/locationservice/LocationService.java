package cn.person.smalldogassistantv1.locationservice;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import cn.person.smalldogassistantv1.staticdata.StaticData;
import cn.person.smalldogassistantv1.voiceservice.VoiceService;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.location.LocationProviderProxy;
import com.amap.api.maps.LocationSource;

public class LocationService extends Service implements AMapLocationListener,
		LocationSource {

	private LocationManagerProxy mAMapLocationManager;
	private Timer timer = null;
	private TimerTask task = null;
	private double latitude, longitude, speed;
	private Intent ServiceIntent = null;
	private double distance;
	private OnLocationChangedListener mListener;
	private boolean change = false;
	private AMapLocation mLocation;
	private VoiceService voice;
	private String provider = null;

	// ******************鍐呴儴瀹氭椂鍣�,瀹氭椂鍙戦�佸畾浣嶇浉鍏虫暟*******************
	public void startTimer() {
		if (timer == null) {
			timer = new Timer();
			task = new TimerTask() {

				@Override
				public void run() {

					if (ServiceIntent == null) {
						ServiceIntent = new Intent(
								StaticData.LOCATION_DATA_BRODCAST_INTENT);
					}

					ServiceIntent.putExtra(StaticData.LATITUDE, latitude);
					ServiceIntent.putExtra(StaticData.LONGITUDE, longitude);
					ServiceIntent.putExtra(StaticData.SPEED, speed);
					ServiceIntent.putExtra(StaticData.PROVIDER, provider);
					sendBroadcast(ServiceIntent);
				}
			};
			timer.schedule(task, 1000, 1000);
		}
	}

	public void stopTimer() {
		if (timer != null) {
			task.cancel();
			timer.cancel();
			timer = null;
			task = null;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return servicebinder;
	}

	public final LocationServiceBinder servicebinder = new LocationServiceBinder();

	public class LocationServiceBinder extends Binder {

		public LocationService getLocationService() {

			return LocationService.this;
		}

	}

	@Override
	public void onCreate() {
		super.onCreate();
		startTimer();
		VoiceService VoiceManager = VoiceService.getInstance(this);// 鍒濆鍖栬闊虫ā鍧�
		VoiceManager.init();
		voice = VoiceManager;
		mAMapLocationManager = LocationManagerProxy.getInstance(this);
		mAMapLocationManager.requestLocationData(
				LocationProviderProxy.AMapNetwork, 5 * 1000, 10, this);
		mAMapLocationManager.setGpsEnable(true);
	}

	@Override
	public void onDestroy() {
		stopTimer();
		deactivate();
		super.onDestroy();
	}

	// service 閲嶅惎鎴栬�呭紑濮嬫椂浣跨敤鐨勭浉鍏虫搷浣�
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mAMapLocationManager != null) {
			startTimer();
			mAMapLocationManager.requestLocationData(
					LocationProviderProxy.AMapNetwork, 5 * 1000, 10, this);
		}
		if (voice == null) {
			VoiceService VoiceManager = VoiceService.getInstance(this);// 鍒濆鍖栬闊虫ā鍧�
			VoiceManager.init();
			voice = VoiceManager;
		}
		return START_STICKY;
	}

	// 鑾峰彇褰撳墠鍧愭爣鍙傛暟
	public OnLocationChangedListener getonLocationChanged() {

		return mListener;
	}

	public AMapLocation getLocaion() {
		return mLocation;
	}

	public VoiceService getVoice() {
		return voice;
	}

	// **********************閲嶅啓鐩稿叧瀹氫綅鏂规硶**********************
	@Override
	public void onLocationChanged(Location location) {
		// 搴熷純涓嶇敤锛屼娇鐢ㄧ敱楂樺痉鎻愪緵鐨勪綅缃洃鍚�
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

	@Override
	public void onLocationChanged(AMapLocation aLocation) {
		if (mListener != null && aLocation != null
				&& aLocation.getAMapException().getErrorCode() == 0) {
			latitude = aLocation.getLatitude();
			longitude = aLocation.getLongitude();
			if (aLocation.getProvider().equals("gps")) {
				speed = aLocation.getSpeed();
				provider = "gps";
			} else if (aLocation.getProvider().equals("lbs")) {
				provider = "lbs";
			}
			mListener.onLocationChanged(aLocation);
		}
	}

	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
		if (mAMapLocationManager == null) {
			mAMapLocationManager = LocationManagerProxy.getInstance(this);
			mAMapLocationManager.requestLocationData(
					LocationProviderProxy.AMapNetwork, 5 * 1000, 10, this);
			mAMapLocationManager.setGpsEnable(true);
		}
	}

	@Override
	public void deactivate() {
		mListener = null;
		if (mAMapLocationManager != null) {
			mAMapLocationManager.setGpsEnable(false);
			mAMapLocationManager.removeUpdates(this);
			mAMapLocationManager.destory();
		}
		mAMapLocationManager = null;
	}
}
