package com.hsinkoyu.android.swipelistitem;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

/**
 * Created by HsinkoYu on 2016/9/2.
 */
public class ContactListAdapter extends SimpleCursorAdapter {
    private final String TAG = "ContactListAdapter";

    private SwipeListItem mLastSwipeItem;

    public ContactListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final View thisView = super.newView(context, cursor, parent);
        final SwipeListItem thisItem = SwipeListItem.accept(context, thisView, ((ListActivity)context).getListView(), SwipeListItem.SWIPE_LEFT, SwipeListItem.TYPE_PULL_OUT, null, null);

        thisItem.setOnSwipeListener(new SwipeListItem.OnSwipeListener() {
            // touch down event
            public void onReady() {
                if (mLastSwipeItem != null && mLastSwipeItem != thisItem) {
                    mLastSwipeItem.restoreView();
                }
                mLastSwipeItem = thisItem;
            }

            public void onStart(int direction, int distance) {
                //Log.v(TAG, "SwipeListItem onStart() direction = " + direction + " distance = " + distance);

            }

            public void onMove(int direction, int distance) {
                //Log.v(TAG, "SwipeListItem onMove() direction = " + direction + " distance = " + distance);

            }

            public void onGoCancelling(int direction, int distance) {
                //Log.v(TAG, "SwipeListItem onGoCancelling() direction = " + direction + " distance = " + distance);

            }

            public void onCancelled(int direction, int distance) {
                //Log.v(TAG, "SwipeListItem onCancelled() direction = " + direction + " distance = " + distance);

            }

            public void onGoSwiping(int direction, int distance) {
                //Log.v(TAG, "SwipeListItem onGoSwiping() direction = " + direction + " distance = " + distance);

            }

            public void onSwiped(int direction, int distance) {
                //Log.v(TAG, "SwipeListItem onSwiped() direction = " + direction + " distance = " + distance);

            }
        });

        return thisView;
    }
}
