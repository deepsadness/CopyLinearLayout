package com.cry.copylinearlayout.listView

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

/**
 *
 * 1. onLayout方法。LayoutParams?
 * 2. 缓存管理的 Recycler? makeChildDirty?
 * 3.
 * 4. 处理点击事件。焦点处理。滑动
 *
 * Created by Administrator on 2018/4/13 0013.
 */
class CopyListView : ViewGroup {
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

    }

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

}