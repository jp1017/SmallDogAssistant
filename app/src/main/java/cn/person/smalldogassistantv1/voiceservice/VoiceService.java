package cn.person.smalldogassistantv1.voiceservice;

import android.content.Context;
import cn.person.smalldogassistantv1.R;

import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.model.AMapNaviLocation;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

public class VoiceService implements SynthesizerListener, AMapNaviListener {

	public static VoiceService VoiceManager;
	private Context mContext;
	boolean isfinish = true;
	// 合成对象.
	private SpeechSynthesizer mSpeechSynthesizer;

	public VoiceService(Context context) {
		mContext = context;
	}

	public static VoiceService getInstance(Context context) {
		if (VoiceManager == null) {
			VoiceManager = new VoiceService(context);
		}
		return VoiceManager;
	}

	public void init() {
		SpeechUtility.createUtility(mContext, SpeechConstant.APPID
				+ "=请输入申请到的科大讯飞语音的key");
		// 初始化合成对象.
		mSpeechSynthesizer = SpeechSynthesizer
				.createSynthesizer(mContext, null);
		initSpeechSynthesizer();
	}

	/**
	 * 使用SpeechSynthesizer合成语音，不弹出合成Dialog.
	 * 
	 * @param
	 */
	public void playText(String playText) {
		if (!isfinish) {
			return;
		}
		if (null == mSpeechSynthesizer) {
			// 创建合成对象.
			mSpeechSynthesizer = SpeechSynthesizer.createSynthesizer(mContext,
					null);
			initSpeechSynthesizer();
		}
		// 进行语音合成.
		mSpeechSynthesizer.startSpeaking(playText, this);

	}

	public void stopSpeaking() {
		if (mSpeechSynthesizer != null)
			mSpeechSynthesizer.stopSpeaking();
	}

	private void initSpeechSynthesizer() {
		// 设置发音人
		mSpeechSynthesizer.setParameter(SpeechConstant.VOICE_NAME,
				mContext.getString(R.string.preference_default_tts_role));
		// 设置语速
		mSpeechSynthesizer.setParameter(SpeechConstant.SPEED,
				"" + mContext.getString(R.string.preference_key_tts_speed));
		// 设置音量
		mSpeechSynthesizer.setParameter(SpeechConstant.VOLUME,
				"" + mContext.getString(R.string.preference_key_tts_volume));
		// 设置语调
		mSpeechSynthesizer.setParameter(SpeechConstant.PITCH,
				"" + mContext.getString(R.string.preference_key_tts_pitch));
		mSpeechSynthesizer.setParameter(SpeechConstant.TTS_AUDIO_PATH,
				"./sdcard/SmallDogVoice/voice.pcm");
		mSpeechSynthesizer.startSpeaking("初始化完成", mSynListener);
	}

	private SynthesizerListener mSynListener = new SynthesizerListener() {

		@Override
		public void onSpeakResumed() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSpeakProgress(int arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSpeakPaused() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSpeakBegin() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onCompleted(SpeechError arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onBufferProgress(int arg0, int arg1, int arg2, String arg3) {
			// TODO Auto-generated method stub

		}
	};

	public void destroy() {
		if (mSpeechSynthesizer != null) {
			mSpeechSynthesizer.stopSpeaking();
		}
	}

	@Override
	public void onArriveDestination() {
		// TODO Auto-generated method stub
		this.playText("到达目的地");
	}

	@Override
	public void onArrivedWayPoint(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCalculateRouteFailure(int arg0) {
		// TODO Auto-generated method stub
		this.playText("路径计算失败，请检查网络或输入参数");
	}

	@Override
	public void onCalculateRouteSuccess() {
		// TODO Auto-generated method stub
		String calculateResult = "路径计算就绪";

		this.playText(calculateResult);
	}

	@Override
	public void onEndEmulatorNavi() {
		// TODO Auto-generated method stub
		this.playText("导航结束");
	}

	@Override
	public void onGetNavigationText(int arg0, String arg1) {
		// TODO Auto-generated method stub
		this.playText(arg1);
	}

	@Override
	public void onGpsOpenStatus(boolean arg0) {

	}

	@Override
	public void onInitNaviFailure() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onInitNaviSuccess() {
		// this.playText("小狗助手为您导航");
	}

	@Override
	public void onLocationChange(AMapNaviLocation location) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReCalculateRouteForTrafficJam() {
		// TODO Auto-generated method stub
		this.playText("前方路线拥堵，路线重新规划");
	}

	@Override
	public void onReCalculateRouteForYaw() {
		// TODO Auto-generated method stub
		this.playText("您已偏航");
	}

	@Override
	public void onStartNavi(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTrafficStatusUpdate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBufferProgress(int arg0, int arg1, int arg2, String arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCompleted(SpeechError arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSpeakProgress(int arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSpeakBegin() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSpeakPaused() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSpeakResumed() {
		// TODO Auto-generated method stub

	}

}
