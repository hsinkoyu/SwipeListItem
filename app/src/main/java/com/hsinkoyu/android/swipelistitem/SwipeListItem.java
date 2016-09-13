/*
 * hsinkoyu@gmail.com
 */

package com.hsinkoyu.android.swipelistitem;

import android.content.Context;
import android.view.View;
import java.lang.IllegalArgumentException;
import android.util.Log;
import java.lang.Math;
import android.view.MotionEvent;
import android.widget.ListView;
import android.os.Handler;
import android.view.GestureDetector;
import android.widget.AdapterView;

public class SwipeListItem implements View.OnTouchListener {
    private static final String TAG = "SwipeListItem";

    public static final int TYPE_PULL_OUT = 0;
    public static final int TYPE_OPEN_PAGE = 1;

    public static final int SWIPE_STILL = 0x00;
    // Accepted directions
    public static final int SWIPE_UP    = 0x01; // Not supported yet
    public static final int SWIPE_DOWN  = 0x02; // Not supported yet
    public static final int SWIPE_LEFT  = 0x04;
    public static final int SWIPE_RIGHT = 0x08;

    private final int H_SWIPING_THRESHOLD = 20; // horizontal swiping threshold
    private final int V_SWIPING_THRESHOLD = 20; // vertical swiping threshold
    private final float H_SWIPED_THRESHOLD_RATIO = 2.0f / 3.0f; // 2/3 of the list item width
    private final float V_SWIPED_THRESHOLD_RATIO = 1.0f / 2.0f; // half of the list item height
    private final int SMOOTH_SWIPING_DELAY = 1; // 1 millisecond
    private final int SMOOTH_SWIPING_STEP = 50; // 50 pixels

    /*
     *                  Type 1: Pull Out
     *
     *      Offset both the foreground (mCenter) and 
     *      backgrounds (mTop | mBottom | mRight | mLeft)
     *      views' location.
     *
     *                  +----------------+
     *                  |                |
     *                  |      mTop      |
     *                  |                |
     * +----------------+----------------+----------------+
     * |                |<-Screen width->|                |
     * |     mLeft      |     mCenter    |     mRight     |
     * |                |Ex. A list item |                |
     * +----------------+----------------+----------------+
     *                  |                |
     *                  |     mBottom    |
     *                  |                |
     *                  +----------------+
     *
     *
     *                  Type 2: Open Page
     *
     *        Set the visibility of backgrounds, and
     *        offset the foreground view's location.
     *
     * +------------------------------------------------+
     * |<--------------- Screen width ----------------->|
     * |                                                |
     * |                                                |
     * |                                                |
     * |             Foreground:  mCenter               |
     * |                                                |
     * |             Backgrounds: mTop                  |
     * |                          mBottom               |
     * |                          mRight                |
     * |                          mLeft                 |
     * |                                                |
     * |                                                |
     * |                                                |
     * +------------------------------------------------+
     */

    private View mMotherView;
    private View mCenter; // R.id.center; shown on SWIPE_STILL
    private View mTop;    // R.id.top;    shown on SWIPE_DOWN
    private View mBottom; // R.id.bottom; shown on SWIPE_UP
    private View mLeft;   // R.id.left;   shown on SWIPE_RIGHT
    private View mRight;  // R.id.right;  shown on SWIPE_LEFT

    private int mItemHeight;
    private int mItemWidth;
    private int mSwipedThresholdH;
    private int mSwipedThresholdV;

    private float xDown, yDown, xUp, yUp, xMove, yMove;

    private Context mContext;
    private ListView mListView;
    private int mAcceptedDirections;
    private int mDirection;
    private OnSwipeListener mListener;
    private int mType;
    private GestureDetector mGestureDetector;
    private InterestedGesture mGestureCb;

    private final Handler mHandler = new Handler();
    private boolean mOnCancellingOrSwiping = false;
    private int mCancellingOrSwipingDistance = 0;
    private Runnable mCancellingRunner = new Runnable() {
        public void run() {
            goCancelling();
        }
    };
    private Runnable mSwipingRunner = new Runnable() {
        public void run() {
            goSwiping();
        }
    };

    public interface OnSwipeListener {
        public void onStart(int direction, int distance);
        public void onMove(int direction, int distance);
        public void onGoCancelling(int direction, int distance);
        public void onCancelled(int direction, int distance);
        public void onGoSwiping(int direction, int distance);
        public void onSwiped(int direction, int distance);
    }

    public static SwipeListItem accept(Context context, View view, ListView listView, int acceptedDirections, int type, InterestedGesture gestureCb, OnSwipeListener listener) {
        SwipeListItem swipeListItem = new SwipeListItem(context, view, listView, acceptedDirections, type, gestureCb, listener);
        view.setOnTouchListener(swipeListItem);
        return swipeListItem;
    }

    public static void reject(View view) {
        view.setOnTouchListener(null);
    }

    public SwipeListItem(Context context, View view, ListView listView, int acceptedDirections, int type, InterestedGesture gestureCb, OnSwipeListener listener) {
        mContext = context;
        mMotherView = view;
        mListView = listView;
        mAcceptedDirections = acceptedDirections;
        mType = type;
        mGestureCb = gestureCb;
        mListener = listener;

        if (context == null) {
            throw new IllegalArgumentException("Null context");
        }

        if (listener == null) {
            throw new IllegalArgumentException("Null listener");
        }

        if (view == null) {
            throw new IllegalArgumentException("Null view");
        }

        if (listView == null) {
            throw new IllegalArgumentException("Null listView");
        }

        mCenter = view.findViewById(R.id.center);
        if (mCenter == null) {
            throw new IllegalArgumentException("No center child view");
        }

        mTop = view.findViewById(R.id.top);
        if (mTop == null) {
            throw new IllegalArgumentException("No top child view");
        }

        mBottom = view.findViewById(R.id.bottom);
        if (mBottom == null) {
            throw new IllegalArgumentException("No bottom child view");
        }

        mLeft = view.findViewById(R.id.left);
        if (mLeft == null) {
            throw new IllegalArgumentException("No left child view");
        }

        mRight = view.findViewById(R.id.right);
        if (mRight == null) {
            throw new IllegalArgumentException("No right child view");
        }

        mGestureDetector = new GestureDetector(mContext, new SwipeDetector());
    }

    public void setWidth(int width) {
        mItemWidth = width;
        mSwipedThresholdH = (int)(mItemWidth * H_SWIPED_THRESHOLD_RATIO);
    }

    public void setHeight(int height) {
        mItemHeight = height;
        mSwipedThresholdV = (int)(mItemHeight * V_SWIPED_THRESHOLD_RATIO);
    }

    public boolean onTouch(View v, MotionEvent event) {
        boolean handled = true;
        int xDistance;
        int yDistance;
        int distance = 0;

        if (mOnCancellingOrSwiping) {
            // During cancelling or swiping, do nothing to touch events.
            return handled;
        }

        // A convenience object to listen for a subset of all the gestures.
        mGestureDetector.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDown = event.getX();
                yDown = event.getY();
                mDirection = SWIPE_STILL;
                // To let mCenter have the chance selecting background colour via selector. Ex. android:background="@drawable/notification_selector"
                mCenter.setPressed(true);
                break;
            case MotionEvent.ACTION_MOVE:
                xMove = event.getX();
                yMove = event.getY();
                xDistance = (int)(xMove - xDown);
                yDistance = (int)(yMove - yDown);
                if (mDirection == SWIPE_STILL) {
                    // Decide the direction
                    if ((Math.abs(xDistance) > H_SWIPING_THRESHOLD) && (xDistance > 0) && ((mAcceptedDirections & SWIPE_RIGHT) != 0)) {
                        distance = xDistance;
                        mDirection = SWIPE_RIGHT;
                    } else if ((Math.abs(xDistance) > H_SWIPING_THRESHOLD) && (xDistance < 0) && ((mAcceptedDirections & SWIPE_LEFT) != 0)) {
                        distance = xDistance;
                        mDirection = SWIPE_LEFT;
                    } else if ((Math.abs(yDistance) > V_SWIPING_THRESHOLD) && (yDistance > 0) && ((mAcceptedDirections & SWIPE_DOWN) != 0)) {
                        distance = yDistance;
                        mDirection = SWIPE_DOWN;
                    } else if ((Math.abs(yDistance) > V_SWIPING_THRESHOLD) && (yDistance < 0) && ((mAcceptedDirections & SWIPE_UP) != 0)) {
                        distance = yDistance;
                        mDirection = SWIPE_UP;
                    }

                    if (mDirection != SWIPE_STILL) {
                        setSwipeView(mDirection);

                        mListView.requestDisallowInterceptTouchEvent(true);
                        swipe(mDirection, distance);
                        mListener.onStart(mDirection, distance);

                        mCenter.setPressed(false);
                    }
                } else {
                    if (mDirection == SWIPE_RIGHT || mDirection == SWIPE_LEFT) {
                        distance = xDistance;
                    } else {
                        distance = yDistance;
                    }

                    swipe(mDirection, distance);
                    mListener.onMove(mDirection, distance);
                }
                break;
            case MotionEvent.ACTION_UP:
                xUp = event.getX();
                yUp = event.getY();
                xDistance = (int)(xUp - xDown);
                yDistance = (int)(yUp - yDown);
                if (mDirection != SWIPE_STILL) {
                    boolean goSwiping = false;
                    if ((Math.abs(xDistance) > mSwipedThresholdH) && (mDirection == SWIPE_RIGHT || mDirection == SWIPE_LEFT)) {
                        goSwiping = true;
                    } else if ((Math.abs(yDistance) > mSwipedThresholdV) && (mDirection == SWIPE_DOWN || mDirection == SWIPE_UP)) {
                        goSwiping = true;
                    }

                    if (mDirection == SWIPE_RIGHT || mDirection == SWIPE_LEFT) {
                        distance = xDistance;
                    } else {
                        distance = yDistance;
                    }

                    mOnCancellingOrSwiping = true;
                    mCancellingOrSwipingDistance = distance;
                    if (goSwiping) {
                        goSwiping();
                    } else {
                        goCancelling();
                    }
                }
                mCenter.setPressed(false);
                break;
            case MotionEvent.ACTION_CANCEL:
                // The current gesture has been aborted. You will not receive any more points in it. You should treat this as an 
                // up event, but not perform any action that you normally would.
                Log.v(TAG, "onTouch MotionEvent.ACTION_CANCEL");
                mCenter.setPressed(false);
                break;
            default:
                Log.v(TAG, "onTouch MotionEvent.ACTION_? = " + event.getAction());
                handled = false;
                break;
        }

        return handled;
    }

    private void setSwipeView(int direction) {
        mTop.setVisibility(View.INVISIBLE);
        mBottom.setVisibility(View.INVISIBLE);
        mLeft.setVisibility(View.INVISIBLE);
        mRight.setVisibility(View.INVISIBLE);

        if (direction == SWIPE_DOWN) {
            mTop.setVisibility(View.VISIBLE);
        } else if (direction == SWIPE_UP) {
            mBottom.setVisibility(View.VISIBLE);
        } else if (direction == SWIPE_RIGHT) {
            mLeft.setVisibility(View.VISIBLE);
        } else if (direction == SWIPE_LEFT) {
            mRight.setVisibility(View.VISIBLE);
        }
    }

    private void onCancelledOrSwiped() {
        mOnCancellingOrSwiping = false;
        mDirection = SWIPE_STILL;
        mListView.requestDisallowInterceptTouchEvent(false);
    }

    private void goCancelling() {
        if (mDirection == SWIPE_LEFT) {
            if (mCancellingOrSwipingDistance < 0) {
                swipe(mDirection, mCancellingOrSwipingDistance);
                mListener.onGoCancelling(mDirection, mCancellingOrSwipingDistance);
                mCancellingOrSwipingDistance += SMOOTH_SWIPING_STEP;

                mHandler.postDelayed(mCancellingRunner, SMOOTH_SWIPING_DELAY);
            } else {
                mCancellingOrSwipingDistance = 0;
                swipe(mDirection, mCancellingOrSwipingDistance);
                mListener.onCancelled(mDirection, mCancellingOrSwipingDistance);

                onCancelledOrSwiped();
            }
        } else if (mDirection == SWIPE_RIGHT) {
            if (mCancellingOrSwipingDistance > 0) {
                swipe(mDirection, mCancellingOrSwipingDistance);
                mListener.onGoCancelling(mDirection, mCancellingOrSwipingDistance);
                mCancellingOrSwipingDistance -= SMOOTH_SWIPING_STEP;

                mHandler.postDelayed(mCancellingRunner, SMOOTH_SWIPING_DELAY);
            } else {
                mCancellingOrSwipingDistance = 0;
                swipe(mDirection, mCancellingOrSwipingDistance);
                mListener.onCancelled(mDirection, mCancellingOrSwipingDistance);

                onCancelledOrSwiped();
            }
        } else {
            // SWIPE_UP and SWIPE_DOWN are not supported yet.
        }
    }

    private void goSwiping() {
        if (mDirection == SWIPE_LEFT) {
            if (mCancellingOrSwipingDistance > -mItemWidth) {
                swipe(mDirection, mCancellingOrSwipingDistance);
                mListener.onGoSwiping(mDirection, mCancellingOrSwipingDistance);
                mCancellingOrSwipingDistance -= SMOOTH_SWIPING_STEP;

                mHandler.postDelayed(mSwipingRunner, SMOOTH_SWIPING_DELAY);
            } else {
                mCancellingOrSwipingDistance = -mItemWidth;
                swipe(mDirection, mCancellingOrSwipingDistance);
                mListener.onSwiped(mDirection, mCancellingOrSwipingDistance);

                onCancelledOrSwiped();
            }
        } else if (mDirection == SWIPE_RIGHT) {
            if (mCancellingOrSwipingDistance < mItemWidth) {
                swipe(mDirection, mCancellingOrSwipingDistance);
                mListener.onGoSwiping(mDirection, mCancellingOrSwipingDistance);
                mCancellingOrSwipingDistance += SMOOTH_SWIPING_STEP;

                mHandler.postDelayed(mSwipingRunner, SMOOTH_SWIPING_DELAY);
            } else {
                mCancellingOrSwipingDistance = mItemWidth;
                swipe(mDirection, mCancellingOrSwipingDistance);
                mListener.onSwiped(mDirection, mCancellingOrSwipingDistance);

                onCancelledOrSwiped();
            }
        } else {
           // SWIPE_UP and SWIPE_DOWN are not supported yet.
        }
    }

    private void swipe(int direction, int distance) {
        if (direction == SWIPE_LEFT) {
            if (distance > 0) {
                distance = 0;
            }
            mCenter.offsetLeftAndRight(distance - mCenter.getLeft());
            if (mType == TYPE_PULL_OUT) {
                mRight.offsetLeftAndRight(distance - mRight.getLeft() + mItemWidth);
            }
        } else if (direction == SWIPE_RIGHT) {
            if (distance < 0) {
                distance = 0;
            }
            mCenter.offsetLeftAndRight(distance - mCenter.getLeft());
            if (mType == TYPE_PULL_OUT) {
                mLeft.offsetLeftAndRight(distance - mLeft.getLeft() - mItemWidth);
            }
        } else {
            // SWIPE_UP and SWIPE_DOWN are not supported yet.
        }
    }

    private boolean performListViewItemClick(MotionEvent e) {
        if (mListView instanceof AdapterView) {
            AdapterView view = (AdapterView) mListView;
            int pos = view.getPositionForView(mMotherView);
            if (pos != AdapterView.INVALID_POSITION) {
                return view.performItemClick(view.getChildAt(pos - view.getFirstVisiblePosition()), pos, view.getAdapter().getItemId(pos));
            }
        }
        return false;
    }

    class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mGestureCb != null) {
                mGestureCb.onSingleTapUp();
                return true;
            } else {
                return performListViewItemClick(e);
            }
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // Unlike onSingleTapUp(MotionEvent), this will only be called after the detector is confident that the user's first tap is not followed by a second
            // tap leading to a double-tap gesture.
            return true;
        }
    }

    public interface InterestedGesture {
        public void onSingleTapUp();

    }
}
