package com.phonesettings.myassistant.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.myassistant.R;
import com.phonesettings.myassistant.db.DatabaseHelper;

public class MainAdapter extends SimpleCursorAdapter {
    private final LayoutInflater mInflater;

    private final int mNameIndex     ;
    private final int mActiveIndex   ;
    private final int mRunStatusIndex;


    private final class ListViewHolder
    {
        public TextView  name      ;
        public ToggleButton toggle ;
        public ImageView remove;
    }

    public MainAdapter(Context context, int layout, Cursor c, String[] from, int[] to)
    {
        super(context, layout, c, from, to);
        this.mInflater = LayoutInflater.from(context);

        mNameIndex      = c.getColumnIndex( DatabaseHelper.TABLE_SITUATION.SITUATION_NAME );
        mActiveIndex    = c.getColumnIndex( DatabaseHelper.TABLE_SITUATION.IS_ACTIVE );
        mRunStatusIndex = c.getColumnIndex( DatabaseHelper.TABLE_SITUATION.RUN_STATUS );
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor)
    {
        //Retrieve the view holder object from the view
        //(it was put there in newView() method)
        ListViewHolder viewHolder = (ListViewHolder)view.getTag();

        String mName = cursor.getString(mNameIndex);
        boolean mActive = (cursor.getInt(mActiveIndex)==0 ? false : true);
        boolean mRuning = (cursor.getInt(mRunStatusIndex)==0 ? false : true);

        //view.setBackgroundColor(Color.TRANSPARENT);
        viewHolder.name.setText(mName);
        viewHolder.toggle.setChecked(mActive);
        viewHolder.remove.setVisibility(View.VISIBLE);

        if(mRuning){
            viewHolder.name.setTypeface(null, Typeface.BOLD);
            viewHolder.name.setTextColor(Color.GREEN);
        } else {
            viewHolder.name.setTypeface(null, Typeface.NORMAL);
            viewHolder.name.setTextColor(Color.BLACK);
        }

        if(mName.equals(context.getString(R.string.defaults))){
            viewHolder.remove.setVisibility(View.GONE);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        View view = mInflater.inflate(R.layout.row_main_situations, parent, false);

        ListViewHolder viewHolder = new ListViewHolder();
        viewHolder.name    = (TextView) view.findViewById( R.id.situations_name  );
        viewHolder.toggle = (ToggleButton)view.findViewById( R.id.toggle_button );
        viewHolder.remove = (ImageView) view.findViewById(R.id.delete_image);
        view.setTag(viewHolder);

        return view;
    }


}
