/*
 * Copyright (C) 2010 Eric Harlow
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * References:
 * https://github.com/ericharlow/TICEWidgets/tree/master/DragNDropList
 * https://github.com/ericharlow/TICEWidgets/blob/master/DragNDropList/src/com/ericharlow/DragNDrop/DragNDropListView.java
 */
package com.phonesettings.myassistant;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

public class DragAndDropListView extends ListView {
    private static final String TAG = "DragAndDropListView";

    boolean mDragMode;
    boolean longPressed;

    int mStartPosition;
    int mEndPosition;
    int mDragPointOffset; //Used to adjust drag view location

    ImageView mDragView;

    DropListener mDropListener;
    RemoveListener mRemoveListener;
    DragListener mDragListener;

    private int mUpperBound; // scroll the view when dragging point is moving out of this bound
    private int mLowerBound; // scroll the view when dragging point is moving out of this bound
    int mBackgroundColor = Color.GRAY;
    int END_OF_LIST_POSITION = -2;

    public DragAndDropListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        longPressed = false;
        setOnItemLongClickListener(mLongClick);
    }

    OnItemLongClickListener mLongClick = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            longPressed = true;
            return true;
        }
    };
    /*
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        if (longPressed) {
            mDragMode = true;
            mStartPosition = myPointToPosition(x,y);
            if (mStartPosition != INVALID_POSITION) {
                int mItemPosition = mStartPosition - getFirstVisiblePosition();
                mDragPointOffset = y - getChildAt(mItemPosition).getTop();
                mDragPointOffset -= ((int)ev.getRawY()) - y;

                lastPos = myPointToPosition(x,y);
                //
                int height = getHeight();
                mUpperBound = Math.min(y, height / 3);
                mLowerBound = Math.max(y, height * 2 / 3);
                //
                startDrag(mItemPosition,y);
                drag(0,y);// replace 0 with x if desired
            }
            longPressed = false;
        }

        if (!mDragMode)
            return super.onTouchEvent(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                drag(0,y);// replace 0 with x if desired
                changePosition(x,y);
                scrollList(y);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            default:
                mDragMode = false;
                longPressed = false;
                mEndPosition = myPointToPosition(x,y);
                Log.e(TAG, "start=" + mStartPosition + "; end=" + mEndPosition);
                if(mStartPosition != INVALID_POSITION){
                    stopDrag(mStartPosition-getFirstVisiblePosition());
                    if (mDropListener != null && mEndPosition != INVALID_POSITION && mEndPosition != END_OF_LIST_POSITION){
                        getChildAt(mEndPosition-getFirstVisiblePosition()).setVisibility(VISIBLE);
                        mDropListener.onDrop(mStartPosition, mEndPosition);
                    }else{// fix for visibility the last position
                        int currentLastPos = getLastVisiblePosition() - getFirstVisiblePosition();
                        if(getChildAt(currentLastPos) != null){
                            getChildAt(currentLastPos).setVisibility(View.VISIBLE);
                            Log.e(TAG, "mEndPosition="+mEndPosition);
                            mDropListener.onDrop(mStartPosition, mEndPosition);
                        }
                    }
                }
                break;
        }
        return true;
    }

    private void resetScrollBounds(int y) {
        int height = getHeight();
        if (y >= height / 3) {
            mUpperBound = height / 3;
        }
        if (y <= height * 2 / 3) {
            mLowerBound = height * 2 / 3;
        }
    }

    private void scrollList(int y) {
        resetScrollBounds(y);

        int height = getHeight();
        int speed = 0;
        if (y > mLowerBound) {
            // scroll the list up a bit
            speed = y > (height + mLowerBound) / 2 ? 16 : 4;
        } else if (y < mUpperBound) {
            // scroll the list down a bit
            speed = y < mUpperBound / 2 ? -16 : -4;
        }
        if (speed != 0) {
            int ref = pointToPosition(0, height / 2);
            if (ref == AdapterView.INVALID_POSITION) {
                //we hit a divider or an invisible view, check somewhere else
                ref = pointToPosition(0, height / 2 + getDividerHeight() + 64);
            }
            View v = getChildAt(ref - getFirstVisiblePosition());
            if (v != null) {
                int pos = v.getTop();
                setSelectionFromTop(ref, pos - speed);
            }
        }
    }

    /* pointToPosition() doesn't consider invisible views, but we need to, so implement a slightly different version. */
    private int myPointToPosition(int x, int y) {
        if (y < 0) {
            return getFirstVisiblePosition();
        }
        Rect frame = new Rect();
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(x, y)) {
                return getFirstVisiblePosition() + i;
            }
        }
        if ((x >= frame.left) && (x < frame.right) && (y >= frame.bottom)) {
            return END_OF_LIST_POSITION;
        }
        return INVALID_POSITION;
    }


    int lastPos;
    private void changePosition(int x, int y){
        int to = myPointToPosition(x,y);
        //Log.e(TAG, "lastPos="+lastPos+"; to="+to+"; firstVisible="+getFirstVisiblePosition()+"; lastVisible="+getLastVisiblePosition());
        if (to == INVALID_POSITION || to == END_OF_LIST_POSITION || lastPos == to) {
            return;
        }

        mDragListener.onChangePosition(lastPos, to);
        int fvp=getFirstVisiblePosition();
        getChildAt(to - fvp).setVisibility(View.INVISIBLE);
        getChildAt(lastPos - fvp).setVisibility(View.VISIBLE);
        lastPos = to;
    }

    // move the drag view
    private void drag(int x, int y) {
        if (mDragView != null) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mDragView.getLayoutParams();
            layoutParams.x = x;
            layoutParams.y = y - mDragPointOffset;
            WindowManager mWindowManager = (WindowManager) getContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            mWindowManager.updateViewLayout(mDragView, layoutParams);

        }
    }

    // enable the drag view for dragging
    private void startDrag(int itemIndex, int y) {
        stopDrag(itemIndex);

        View item = getChildAt(itemIndex);
        if (item == null) {
            return;
        }
        item.setDrawingCacheEnabled(true);
        item.setVisibility(View.INVISIBLE);
        int defaultBackgroundColor = item.getDrawingCacheBackgroundColor();
        item.setBackgroundColor(mBackgroundColor);

        // Create a copy of the drawing cache so that it does not get recycled
        // by the framework when the list tries to clean up memory
        Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
        item.destroyDrawingCache();
        item.setBackgroundColor(defaultBackgroundColor);

        WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP;
        mWindowParams.x = 0;
        mWindowParams.y = y - mDragPointOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;
        mWindowParams.alpha = 0.65f;

        Context context = getContext();
        ImageView v = new ImageView(context);
        v.setImageBitmap(bitmap);

        WindowManager mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
    }

    // destroy drag view
    private void stopDrag(int itemIndex) {
        if (mDragView != null) {
            mDragView.setVisibility(GONE);
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }
    }

    public void enableSorting(boolean enabled){
        if(enabled){
            longPressed = true;
        } else{
            longPressed = false;
        }
    }

    public void setDropListener(DropListener l) {
        mDropListener = l;
    }

    public void setRemoveListener(RemoveListener l) {
        mRemoveListener = l;
    }

    public void setDragListener(DragListener l) {
        mDragListener = l;
    }

    public interface DropListener {
        void onDrop(int from, int to);
    }
    public interface RemoveListener {
        void onRemove(int index);
    }
    public interface DragListener {
        void onChangePosition(int from, int to);
    }
}