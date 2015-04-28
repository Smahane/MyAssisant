package com.phonesettings.myassistant.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.example.myassistant.R;
import com.phonesettings.myassistant.db.DatabaseHelper;
import com.phonesettings.myassistant.utils.C;

public class EditSituationAdapter extends SimpleCursorAdapter {
	private final LayoutInflater mInflater;

	private final int mTitleIndex;
	private final int mSubTitleIndex;

	private final class ListViewHolder {
		public ImageView icon;
		public TextView title;
		public TextView subTitle;
	}

	public EditSituationAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, boolean aboutCondition) {
		super(context, layout, c, from, to);
		this.mInflater = LayoutInflater.from(context);

		if (aboutCondition) {
			mTitleIndex = c
					.getColumnIndex(DatabaseHelper.TABLE_CONDITION.TITLE);
			mSubTitleIndex = c
					.getColumnIndex(DatabaseHelper.TABLE_CONDITION.DESCRIPTION);
		} else {
			mTitleIndex = c.getColumnIndex(DatabaseHelper.TABLE_SETTING.TITLE);
			mSubTitleIndex = c
					.getColumnIndex(DatabaseHelper.TABLE_SETTING.DESCRIPTION);
		}
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		// Retrieve the view holder object from the view
		// (it was put there in newView() method)
		ListViewHolder viewHolder = (ListViewHolder) view.getTag();

		String title = cursor.getString(mTitleIndex);
		int mTitle = context.getResources().getIdentifier(title, "string",
				context.getPackageName());

		int mIconId = C.getInstance().getIconIdByTitle(mTitle);
		String mSubTitle = cursor.getString(mSubTitleIndex);

		viewHolder.icon.setImageDrawable(context.getResources().getDrawable(
				mIconId));
		viewHolder.title.setText(context.getString(mTitle));
		viewHolder.title.setHint(title);
		viewHolder.subTitle.setText(mSubTitle);

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.row_edit, parent, false);

		ListViewHolder viewHolder = new ListViewHolder();
		viewHolder.icon = (ImageView) view.findViewById(R.id.edit_icon);
		viewHolder.title = (TextView) view.findViewById(R.id.edit_title);
		viewHolder.subTitle = (TextView) view.findViewById(R.id.edit_subtitle);

		view.setTag(viewHolder);

		return view;
	}

}
