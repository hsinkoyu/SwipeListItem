package com.hsinkoyu.android.swipelistitem;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.SimpleCursorAdapter;

/**
 * Created by HsinkoYu on 2016/9/2.
 */
public class ContactListAdapter extends SimpleCursorAdapter {
    private final String TAG = "ContactListAdapter";

    public ContactListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final View view = super.newView(context, cursor, parent);
        final SwipeListItem swipeListItem = SwipeListItem.accept(context, view, ((ListActivity)context).getListView(), SwipeListItem.SWIPE_LEFT, SwipeListItem.TYPE_PULL_OUT, null,
                new SwipeListItem.OnSwipeListener() {
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
                }
        );

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                swipeListItem.setWidth(view.getWidth());
                swipeListItem.setHeight(view.getHeight());
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        return view;
    }
}
