package com.cry.copylinearlayout.relative

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

/**
 *  1. addRule 手动设置RelativeLayout的相互关系。
 *      添加后是怎么处理的？
 *  2. 内部的DependencyGraph
 *
 *
 *
 * Created by Administrator on 2018/4/10 0010.
 */
class DoubleRelativeLayout : ViewGroup {
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

    }

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

}