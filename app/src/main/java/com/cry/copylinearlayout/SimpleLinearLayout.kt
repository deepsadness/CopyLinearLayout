package com.cry.copylinearlayout

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 *
 *
 * Created by Administrator on 2018/4/10 0010.
 */
class SimpleLinearLayout : ViewGroup {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {}
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {}


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureVertical(widthMeasureSpec, heightMeasureSpec)
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        layoutVertical(left, top, right, bottom)
    }

    private fun measureVertical(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //先得到一些需要用的变量
        var totalHeight = 0 //总高度
        var totalWidth = 0 //总宽度

        var allFillParent = true
        var matchWidthLocally = false
        var matchWidth = false
        var childState = 0

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        //这里是去计算各个Child的高度。
        val childCount = childCount
        for (childIndex in 0 until childCount) {
            val childView = getChildAt(childIndex)
            if (childView == null) {
                continue
            }

            if (childView.visibility == View.GONE) {
                continue
            }
            val lp = childView.layoutParams as LinearLayout.LayoutParams
            var widthUsed = 0
            var heightUsed = totalHeight

            //这样的话。先去计算一次Child的值
            measureChildWithMargins(childView, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed)

            //这里就可以得到child的高宽了
            val childHeight = childView.measuredHeight
            val childWidth = childView.measuredWidth

            //进行累加
            totalHeight += childHeight + lp.topMargin + lp.bottomMargin

            //宽度需要判断当前的宽度模式。如果不是Exactly的话。就可以使用下面这个。
            if (widthMode != MeasureSpec.EXACTLY && lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                matchWidth = true
                matchWidthLocally = true
            }
            //这里可以得到child一共需要的宽度
            val margin = lp.leftMargin + lp.rightMargin
            val childNeedWidth = childWidth + margin

            //先重新计算一次最大值
            totalWidth = Math.max(totalWidth, childNeedWidth)

            //childState也是看总所的childState
            childState = View.combineMeasuredStates(childState, childView.measuredState)

            //判断是否 match_parent，也是看所有的child的情况
            allFillParent = allFillParent && lp.width == ViewGroup.LayoutParams.MATCH_PARENT
        }

        //然后开始汇总
        var heightSize = totalHeight
        //还需要对比各自的最小
        heightSize = Math.max(totalHeight, suggestedMinimumHeight)

        //重新让spec和size一直
        val resolveMeasureHeight = View.resolveSizeAndState(heightSize, heightMeasureSpec, 0)

        //然后取出resolve之后的size
        heightSize = resolveMeasureHeight and View.MEASURED_SIZE_MASK

        //最大宽度同时也要支持
        totalWidth = Math.max(totalWidth, suggestedMinimumWidth)
        val resolveMeasureWidth = View.resolveSizeAndState(totalWidth, widthMeasureSpec, childState)

        setMeasuredDimension(resolveMeasureWidth, resolveMeasureHeight)

        //不要忘记一点。因为是vertical所以所有的child的最大宽度应该是一直的
        if (matchWidth) {
            forceUniformWidth(childCount, heightMeasureSpec)
        }

    }

    //因为需要重写去measureChild，所以计算完之后的高度要传入。
    private fun forceUniformWidth(childCount: Int, heightMeasureSpec: Int) {
        //因为是去同意，所以强行将其设置成exactly
        val uniformMeasureSpec = View.MeasureSpec.makeMeasureSpec(measuredWidth,
                View.MeasureSpec.EXACTLY)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child?.apply {
                if (this.visibility != View.GONE) {
                    val lp = this.layoutParams as LinearLayout.LayoutParams
                    //如果宽度是match的话，就使用当前的width
                    if (lp.width == LayoutParams.MATCH_PARENT) {
                        // Temporarily force children to reuse their old measured height
                        // FIXME: this may not be right for something like wrapping text?
                        val oldHeight = lp.height
                        lp.height = this.measuredHeight
                        // Remeasue with new dimensions
                        measureChildWithMargins(child, uniformMeasureSpec, 0, heightMeasureSpec, 0)
                        lp.height = oldHeight
                    }
                }
            }

        }    }

    /*
    这里还有两个疑问
    1. 不限制layout的高度。因为是vertical的
    2. 不限制layout的宽度吗？
     */
    private fun layoutVertical(left: Int, top: Int, right: Int, bottom: Int) {
        //布置child的高度
        var childTop = 0
        var childLeft = left
        val childCount = childCount
        for (childIndex in 0 until childCount) {
            val childView = getChildAt(childIndex)
            if (childView == null) {
                childTop += 0
                continue
            }
            if (childView.visibility == View.GONE) {
                continue
            }
            //开始累加计算
            val lp = childView.layoutParams as LinearLayout.LayoutParams

            var childHeight = childView.measuredHeight
            var childWidth = childView.measuredWidth

            childTop += lp.topMargin

            childView.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)

            childTop += lp.bottomMargin+childHeight
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LinearLayout.LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return LinearLayout.LayoutParams(p)
    }
}