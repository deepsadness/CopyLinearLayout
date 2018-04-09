package com.cry.copylinearlayout

import android.content.Context
import android.support.annotation.IntDef
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/**
 * 简单的LinearLayout模仿
 * v0:
 * 没有weight版本。只有VERTICAL
 *
 * 1. 主要需要重写 onLayout measure geneateLayoutParams等方法
 * 2. onLayout 方法中的重点是 child.layout()，身为viewGroup必须要手动去布置childView的位置 可以调用measureChildWithMargins的便携函数来帮助我们layout
 *
 *      a. 累加的方式就可以处理了
 *
 * 3. onMeasure方法中的重点是 setMeasuredDimension来确定本身计算的宽和高。  可以调用view.resolveSizeAndState来做便携的命令
 *
 *      a. VERTICAL模式下,需要确定的是 最大的宽度。和如果Child是Match的话，需要强制将其设置成我们自己的宽度
 *      b. 计算最大的高度，是在调用完Child之后添加上
 *
 *
 * todo:
 * 1. 完成HORIZONTAL的版本
 * 2. 加上weight
 *
 * Created by Administrator on 2018/4/8 0008.
 */
class CopyLinearLayout : ViewGroup {
    //    mOrientation == VERTICAL
    private var mTotalLength: Int = 0

    private @OrientationMode
    var mOrientation = VERTICAL

    companion object {
        const val HORIZONTAL = 0L
        const val VERTICAL = 1L

        @IntDef(HORIZONTAL, VERTICAL)
        @Retention(AnnotationRetention.SOURCE)
        annotation class OrientationMode

    }

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mOrientation == VERTICAL) {
            measureVertical(widthMeasureSpec, heightMeasureSpec)
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec)
        }

    }

    private fun measureHorizontal(widthMeasureSpec: Int, heightMeasureSpec: Int) {

    }

    /**
     * measure方法内最关键的是调用ziview的什么方法呢？
     *
     * 1. 重点1：去确认当前给的mode.如果vertical的话，就看height
     * 2. 重点2：child.measure(childWidthMeasureSpec, childHeightMeasureSpec); 先计算一遍child
     * 3. 重点3：setMeasuredDimension 来设置自己的大小
     */
    private fun measureVertical(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //因为是垂直的走，所以宽度只能取一个最大值。但是这个最大值，如何去小于parent的值呢？
        var maxWidth = 0
        //可以取到的总高度。
        var totalHeight = 0
        //这个view的总长度。
        mTotalLength = 0

        var allFillParent = true
        var alternativeMaxWidth = 0
        var matchWidthLocally = false
        var matchWidth = false

        //先取到高宽的数据。和总数
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        val childCount = childCount

        //第一遍去遍历，看看能取到多少的空间
        for (childIndex in 0 until childCount) {
            val childView = getChildAt(childIndex)
            if (childView == null) {
                mTotalLength += 0
                continue
            }
            //先不管是否可见
            if (childView.visibility == View.GONE) {
                continue
            }

            //得到对应的layoutParams
            val layoutParams = childView.layoutParams as LinearLayout.LayoutParams

            //得到计算的使用高度
            val usedHeight = mTotalLength

            //先对子View进行measure一次
            measureChildBeforeLayout(childView, childIndex, widthMeasureSpec, 0, heightMeasureSpec, usedHeight);

            //计算后，在得到高度
            val childHeight = childView.measuredHeight

            val totalLength = mTotalLength
            //最后的高度还需要加上margin
            mTotalLength = Math.max(totalLength, totalLength + childHeight
                    + layoutParams.topMargin
                    + layoutParams.bottomMargin
            )


            if (widthMode != MeasureSpec.EXACTLY && layoutParams.width == LayoutParams.MATCH_PARENT) {
                //如果不是exactly模式的话，至少有一个child要和我们的大小一样大。所以至少得测量自身的大小
                matchWidth = true
                matchWidthLocally = true
            }

            //接下来去得到margin
            val margin = layoutParams.leftMargin + layoutParams.rightMargin
            val measureWidth = childView.measuredWidth + margin
            //来得到我们的最大值
            maxWidth = Math.max(maxWidth, measureWidth)

            //childState到底是什么呢？

            //判断是否是match
            allFillParent = allFillParent && layoutParams.width == LayoutParams.MATCH_PARENT
            var tempWidth = 0
            if (matchWidthLocally) {
                tempWidth = margin
            } else {
                tempWidth = measuredWidth
            }
            alternativeMaxWidth = Math.max(alternativeMaxWidth, tempWidth)

        }

        //到外面来要加padding
        var heightSize = mTotalLength
        //确定最大的高度
        heightSize = Math.max(heightSize, suggestedMinimumHeight)

        val heightSizeAndState = View.resolveSizeAndState(heightSize, heightMeasureSpec, 0)
        //通过位运算取出高度
        heightSize = heightSizeAndState and View.MEASURED_SIZE_MASK

        //如果不是match的也不是exactly的话最大宽度
        if (!allFillParent && widthMode != View.MeasureSpec.EXACTLY) {
            maxWidth = alternativeMaxWidth
        }

        //最大宽度同时也要支持
        maxWidth = Math.max(maxWidth, suggestedMinimumWidth)

        //最重要的是，设置该view的Dimension
        setMeasuredDimension(View.resolveSizeAndState(maxWidth, widthMeasureSpec, 0), heightSizeAndState)


        if (matchWidth) {
            forceUniformWidth(childCount, heightMeasureSpec)
        }
    }

    //强制子View的宽度都和自己一样
    private fun forceUniformWidth(childCount: Int, heightMeasureSpec: Int) {
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

        }
    }

    /**
     * totalWidth  额外需要的宽度
     * totalHeight 额外需要的高度
     */
    private fun measureChildBeforeLayout(
            child: View,
            childIndex: Int,
            widthMeasureSpec: Int,
            totalWidth: Int,
            heightMeasureSpec: Int,
            totalHeight: Int) {
        //直接交给viewGroup来处理margin layoutParams
        measureChildWithMargins(child, widthMeasureSpec, totalWidth,
                heightMeasureSpec, totalHeight)

    }

    //ViewGroup必须重写的是onLayout方法。
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        //linearLayout是两个方向的
        layoutVertical(l, t, r, b)
    }

    private fun layoutVertical(left: Int, top: Int, right: Int, bottom: Int) {
        var childTop = 0
        var childLeft = 0
        // Where right end of child should go
        val width = right - left

        val count = childCount

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child == null) {
                childTop += 0
            } else if (child.visibility != View.GONE) { //只会布局不可见的
                val measuredWidth = child.measuredWidth
                val measuredHeight = child.measuredHeight

                var layoutParams = child.layoutParams as LinearLayout.LayoutParams
                //topMargin加在头部
                childTop += layoutParams.topMargin

                setChildFrame(child, childLeft + 0, childTop,
                        measuredWidth, measuredHeight)

                childTop += measuredHeight + layoutParams.bottomMargin
            }
        }
    }

    private fun setChildFrame(child: View, left: Int, top: Int, width: Int, height: Int) {
        child.layout(left, top, left + width, top + height)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LinearLayout.LayoutParams(context, attrs)
    }
}