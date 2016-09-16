package cn.person.smalldogassistantv1.navigation;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import cn.person.smalldogassistantv1.R;
import cn.person.smalldogassistantv1.staticdata.StaticData;

public class HistroyAdapter extends BaseAdapter {
	private List<HistoryPathItem> list = new ArrayList<HistoryPathItem>();
	private Context context;
	private LayoutInflater mInflater;
	private int index = 0;

	private boolean full = false;
	private int length = 0;
	private final int MAX_STORAGE_LENGTH = 20;
	private String temp_start1 = null;
	private String temp_end1 = null;
	private String temp_start2 = null;
	private String temp_end2 = null;

	public HistroyAdapter(Context context) {
		this.context = context;
		mInflater = LayoutInflater.from(context);
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public void addData(HistoryPathItem item) {
		list.add(item);
		notifyDataSetChanged();
	}

	public void removeItem(HistoryPathItem item) {
		list.remove(item);
		notifyDataSetChanged();
	}

	public void removeItem(int position) {
		list.remove(list.get(position));
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public HistoryPathItem getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	static class ViewHolder {
		ImageButton delete;
		TextView start_text;
		TextView end_text;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;

		final int location = position;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.history_text, null);
			holder.delete = (ImageButton) convertView
					.findViewById(R.id.btn_delete);
			holder.start_text = (TextView) convertView
					.findViewById(R.id.start_name);
			holder.end_text = (TextView) convertView
					.findViewById(R.id.end_name);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.delete.setFocusable(false);
		holder.delete.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				removeItem(location);
				RemovePreferences(location);

			}
		});
		holder.start_text.setText(list.get(position).getStartname());
		holder.end_text.setText(list.get(position).getEndname());

		return convertView;
	}

	private void RemovePreferences(int position) {
		SharedPreferences pref = context.getSharedPreferences(
				StaticData.SMALL_DOG_PREFERENCE, Context.MODE_PRIVATE);
		Editor e = pref.edit();
		index = pref.getInt(StaticData.INDEX, 0);
		full = pref.getBoolean(StaticData.FULL, false);

		if (full == true) {
			length = MAX_STORAGE_LENGTH;
		} else {
			length = index;
		}

		// 寻找搜索
		for (int i = position; i < length; i++) {
			if (i + 1 < length) {
				temp_start1 = pref.getString(StaticData.START_NAME + i, null);
				temp_end1 = pref.getString(StaticData.END_NAME + i, null);
				temp_start2 = pref.getString(StaticData.START_NAME + (i + 1),
						null);
				temp_end2 = pref.getString(StaticData.END_NAME + (i + 1), null);
				temp_start1 = temp_start2;
				temp_end1 = temp_end2;
				e.putString(StaticData.START_NAME + i, temp_start1);
				e.putString(StaticData.END_NAME + i, temp_end1);
				e.commit();
			}
		}

		length--;
		index = length;
		if (length < MAX_STORAGE_LENGTH)
			full = false;
		e.remove(StaticData.START_NAME + length);
		e.remove(StaticData.END_NAME + length);
		e.putBoolean(StaticData.FULL, full);
		e.putInt(StaticData.INDEX, index);
		e.commit();
	}
}
