package com.cry.copylinearlayout.relative;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 *
 *
 * Created by Administrator on 2018/4/10 0010.
 */
public class SimpleRelativeLayout extends ViewGroup {
    public SimpleRelativeLayout(Context context) {
        super(context);
    }

    public SimpleRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SimpleRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }
}
