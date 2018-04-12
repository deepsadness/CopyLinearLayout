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
 *      b. 因为是marginParams，所以需要支持这个参数，就需要考虑margin
 *
 * 3. onMeasure方法中的重点是 setMeasuredDimension来确定本身计算的宽和高。  可以调用view.resolveSizeAndState来做便携的命令
 *
 *      a. VERTICAL模式下,需要确定的是 最大的宽度。和如果Child是Match的话，需要强制将其设置成我们自己的宽度
 *      b. 计算最大的高度，是在调用完Child之后添加上
 *
 *
 * 4. childState 是 宽和高的状态合成一个32位的bit int值
 *      宽度的状态位在常规的位置
 *      高度的状态位在偏移后的位置
 *      我们知道mMeasuredHeight或mMeasuredWidth都是32位的int值,
 *      但这个值并不是一个表示宽高的实际大小的值,而是一个由宽高的状态和实际大小所组合的值.
 *      这里的高8位就表示状态(STATE),而低24位表示的是实际的尺寸大小(SIZE)
 *
 *      最后得到的结果：
 *      高8 宽度的State >>8:0 >>8:高度的State >>8:0
 *
 *      这个childState现在基本没有用处。可能是为了以后留的state
 *
 * 5. 如果有Weight会有什么变化呢
 *      a. 明确一点。weight的话，需要重新测绘去占据.
 *      b. 规则是去占用根据weight去占用excessSpace+自身的高度
 *      c. 完成child的绘制的方式不变。都是去得到 MeasureSpec去传过他
 *      d. 小技巧是。如果高度是0，就先用wrap去测。而且如果全部是0的情况，也不会导致二度的测绘
 *      e. resolveSizeAndState 这个方法，可以让当前自己计算和高度和父ViewGroup给的高度，来做协调
 *          其实是封装了一个对mode判断的方便函数。通过mask可以取到size
 *      f. getChildMeasureSpec 也是一个方便函数。可以在保持传入的mode的情况下，得到正确的size(这里是用作不关注的那一part)
 *
 * 6. 如何去画分割线？
 *      1. setWillNotDraw 让viewGroup有绘制的能力
 *      2. 计算布局的的地方？，还需要你加上分割线的宽和高
 *      3. 如果需要阴影就关掉硬件加速。
 *      4. idea?各种需要不同形状的阴影布局。是不是可以通过从ChildView中取获取需要勾勒的形状。来进行描绘？？这个想法和ViewOutLineProvider不谋而合？！
 *
 * 7. 额外的问题？OverLayoutCompat是怎么是写的？
 *
 * todo:
 * 0. 分割线？
 * 1. 完成HORIZONTAL的版本
 *
 *
 * 疑问：
 * 1. 这里有个疑问。 Measure的State是什么？
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
     * onMeasure方法的意义就在与计算自己的高度和宽度。所以要遍历Child来累计高度。和计算出最大的宽度。
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

            val weight = layoutParams.weight

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
                tempWidth = measureWidth
            }
            alternativeMaxWidth = Math.max(alternativeMaxWidth, tempWidth)

        }
        //遍历之后，就得到了高度和宽度了。这个时候来生成MeasureSpec，设置自己的高宽

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