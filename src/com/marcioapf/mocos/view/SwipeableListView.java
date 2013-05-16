package com.marcioapf.mocos.view;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.*;

public class SwipeableListView extends ListView implements SwipeableViewDelegate.OnSwipeListener {

    private final WeakHashMap<View, SwipeableViewWrapper> mWrapperHashMap =
            new WeakHashMap<View, SwipeableViewWrapper>();

    private SwipeableListListener mSwipeableListListener;
    private ListAdapterWrapper mListAdapterWrapper;
    private SwipeableDataSetObserver mDataSetObserver;

    public SwipeableListView(Context context) {
        this(context, null);
    }

    public SwipeableListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (mListAdapterWrapper != null) {
            mListAdapterWrapper.mInnerAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        mDataSetObserver = new SwipeableDataSetObserver();
        adapter.registerDataSetObserver(mDataSetObserver);
        mListAdapterWrapper = new ListAdapterWrapper(adapter);
        super.setAdapter(mListAdapterWrapper);
    }

    public void setSwipeableListListener(SwipeableListListener swipeableListListener) {
        mSwipeableListListener = swipeableListListener;
    }

    @Override
    public void onSwipeOut(View v) {
        if (!(v instanceof SwipeableViewWrapper))
            return;
        SwipeableViewWrapper wrapper = (SwipeableViewWrapper) v;
        if (mSwipeableListListener != null)
            mSwipeableListListener.onViewSwipedOut(wrapper.getWrappedView());
    }

    @Override
    public void onStartTracking(View v, MotionEvent event) {
    }

    @Override
    public void onSwipeBack(View v) {
    }

    public interface SwipeableListListener {

        void onViewSwipedOut(View view);
    }

    private class SwipeableDataSetObserver extends DataSetObserver {

        private LinkedHashSet<Object> mObservedObjects;

        @Override
        public void onChanged() {
            int size = mListAdapterWrapper.getCount();
            LinkedHashSet<Object> newObservedObjects = new LinkedHashSet<Object>(size);
            for (int i = 0; i < size; i++)
                newObservedObjects.add(mListAdapterWrapper.getItem(i));

            mObservedObjects = newObservedObjects;
            mListAdapterWrapper.notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            int size = mListAdapterWrapper.getCount();
            mObservedObjects = new LinkedHashSet<Object>(size);
            for (int i = 0; i < size; i++)
                mObservedObjects.add(mListAdapterWrapper.getItem(i));

            mListAdapterWrapper.notifyDataSetInvalidated();
        }
    }

    private class SwipeableViewWrapper extends FrameLayout {

        private final SwipeableViewDelegate mSwipeableDelegate;
        private final View mWrappedView;

        public SwipeableViewWrapper(Context context, View wrappedView) {
            super(context);

            mWrappedView = wrappedView;
            mSwipeableDelegate = new SwipeableViewDelegate(this);
            mSwipeableDelegate.setOnSwipeListener(SwipeableListView.this);

            mWrapperHashMap.put(mWrappedView, this);

            addView(wrappedView);
        }

        public View getWrappedView() {
            return mWrappedView;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            return mSwipeableDelegate.handleTouch(ev);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return mSwipeableDelegate.handleTouch(event);
        }
    }

    private class ListAdapterWrapper extends BaseAdapter {
        private final ListAdapter mInnerAdapter;

        private ListAdapterWrapper(ListAdapter innerAdapter) {
            mInnerAdapter = innerAdapter;
        }

        @Override
        public int getCount() {
            return mInnerAdapter.getCount();
        }

        @Override
        public Object getItem(int position) {
            return mInnerAdapter.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return mInnerAdapter.getItemId(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView instanceof SwipeableViewWrapper) {
                View convertWrappedView = ((SwipeableViewWrapper) convertView).getWrappedView();
                View fromInner = mInnerAdapter.getView(position, convertWrappedView, parent);
                if (convertWrappedView == fromInner)
                    return convertView;
                else
                    return new SwipeableViewWrapper(parent.getContext(), fromInner);
            }
            return new SwipeableViewWrapper(parent.getContext(),
                    mInnerAdapter.getView(position, convertView, parent));
        }
    }
}
