package cn.person.smalldogassistantv1.personelaty;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import cn.person.smalldogassistantv1.R;
import cn.person.smalldogassistantv1.staticdata.StaticData;

public class PersonelAty extends Activity implements OnClickListener {
	private Button btn_back;
	private ListView details;
	private ArrayAdapter<String> adapter;
	private float totaldistance;
	private long h, m, s;
	private float maxspeed;
	private DecimalFormat df = new DecimalFormat("#0.00");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_layout);
		init();
	}

	private void init() {
		btn_back = (Button) findViewById(R.id.btn_record_back);
		btn_back.setOnClickListener(this);

		details = (ListView) findViewById(R.id.person_listView);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_expandable_list_item_1);
		getData();
		adapter.add("总共骑行距离：" + df.format(totaldistance) + "km");
		adapter.add("总共骑行时间：" + h + "h" + m + "m" + s + "s");
		adapter.add("最大骑行速度：" + df.format(maxspeed) + "km/h");
		details.setAdapter(adapter);
	}

	@Override
	public void onBackPressed() {
		finish();
		super.onBackPressed();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_record_back:
			finish();
			break;
		}
	}

	private void getData() {
		SharedPreferences pre = getSharedPreferences(
				StaticData.SMALL_DOG_PREFERENCE, 0);
		try {
			totaldistance = pre.getFloat(StaticData.TOTAL_DISTANCE, 0);
		} catch (Exception e) {

		}
		try {
			h = pre.getLong(StaticData.H, 0);
		} catch (Exception e) {

		}

		try {
			m = pre.getLong(StaticData.M, 0);
		} catch (Exception e) {
		}

		try {
			s = pre.getLong(StaticData.S, 0);
		} catch (Exception e) {
		}

		try {
			maxspeed = pre.getFloat(StaticData.MAX_SPEED, 0);
		} catch (Exception e) {
		}
	}
}
