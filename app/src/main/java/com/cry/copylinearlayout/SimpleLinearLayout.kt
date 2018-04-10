package com.cry.copylinearlayout

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 *
 *  测试的想法：
 *  1. 先实现大家都有weight的情况
 *      如果有weight>0的话，就不计算正常的高度了。最后更具weight来计算高度
 *
 *  考虑最简单的情况。 child的高度都是0,没有margin. parent为match
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

        //第一次预计占用
        var consumedExcessSpace = 0

        var weightSum = 0.0f

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
            val lp = childView.layoutParams as android.widget.LinearLayout.LayoutParams

            val weight = lp.weight
            weightSum += weight


            //得去看看lp内的高
            var height = lp.height
            if (height == ViewGroup.LayoutParams.WRAP_CONTENT) {

            } else if (height == 0) {

            }

            //当height=0 而且标了weight>0时，则使用额外的高度
            val useExcessSpace = height == 0 && weight > 0
            if (heightMode == MeasureSpec.EXACTLY && useExcessSpace) {
                //这种情况下。如果linearLayout又是exactly的模式，则不用去测量。
                // 因为child的高度，是viewGroup最后分配的。
                // 而viewGroup的高度是确定的，所以就不需要去计算他了

                //如果不是exactly模式的话，viewGroup的高度，还是需要先从child中来计算

            } else {
                if (useExcessSpace) {   //viewGroup不是确定的高度的话.这地方很聪明。就先把他设置成wrap来测量viewGroup的高度
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }


                var widthUsed = 0
                var heightUsed = totalHeight

                //这样的话。先去计算一次Child的值
                measureChildWithMargins(childView, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed)

                //这里就可以得到child的高宽了
                val childHeight = childView.measuredHeight
                val childWidth = childView.measuredWidth

                //计算后，需要回复回来
                if (useExcessSpace) {
                    lp.height = 0
                    consumedExcessSpace += childHeight
                }

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

                if (weight > 0) {

                }

            }
        }

        //然后开始汇总
        var heightSize = totalHeight
        //还需要对比各自的最小
        heightSize = Math.max(totalHeight, suggestedMinimumHeight)

        //这里要合并一次，取得真实的高度
        // Reconcile our calculated size with the heightMeasureSpec
        val heightSizeAndState = View.resolveSizeAndState(heightSize, heightMeasureSpec, 0)
        heightSize = heightSizeAndState and View.MEASURED_SIZE_MASK

        //通过这段来进行对比。之前的假象是否需要继续需要保留的额外的空间。！！这个很重要。这个是weight情况下计算高度的重点~~
        var remainingExcess = (heightSize - totalHeight + consumedExcessSpace) * 1f

        if (remainingExcess > 0 && weightSum > 0.0f) {  //这个时候，需要重新去布局我们需要的高度
            //todo 除了布局中计算的child中的weight sum，还可以设置总的weight.所以我们需要考虑
            var remainingWeightSum = weightSum
            totalHeight = 0
            //这里就重写计算一遍。每个child的高度。因为已经拿到total的高度了。如果是weight的布局可以直接分配
            for (childIndex in 0 until childCount) {
                val childView = getChildAt(childIndex)
                if (childView == null) {
                    continue
                }
                if (childView.visibility != View.GONE) {
                    val lp = childView.layoutParams as android.widget.LinearLayout.LayoutParams
                    val childWeight = lp.weight

                    if (childWeight > 0) {
                        //从剩下的部分去计算自己还可以占用多少空间
                        val childExcessSpace = remainingExcess * childWeight / remainingWeightSum
                        remainingExcess -= childExcessSpace
                        remainingWeightSum -= childWeight

                        var childWeightHeight = 0f

                        if (lp.height == 0 && heightMode == View.MeasureSpec.EXACTLY) {
                            //之前跳过的部分.这种情况，就完全是分配的高度
                            childWeightHeight = childExcessSpace
                        } else {
                            childWeightHeight = childView.measuredHeight + childExcessSpace
                        }

                        val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                                Math.max(0f, childWeightHeight).toInt(), View.MeasureSpec.EXACTLY
                        )
                        val childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width)

                        childView.measure(childWidthMeasureSpec,childHeightMeasureSpec)

                        //算出childState.取得是高的state
                        childState= View.combineMeasuredStates(childState,childView.measuredState and (View.MEASURED_STATE_MASK shr View.MEASURED_HEIGHT_STATE_SHIFT))

                    }

                    //正常的逻辑重复一遍

                    val margin = lp.leftMargin + lp.rightMargin
                    val measuredWidth = childView.measuredWidth + margin
                    totalWidth = Math.max(totalWidth, measuredWidth)

                    val matchWidthLocally = widthMode != View.MeasureSpec.EXACTLY && lp.width == LayoutParams.MATCH_PARENT

//                    alternativeMaxWidth = Math.max(alternativeMaxWidth,
//                            if (matchWidthLocally) margin else measuredWidth)

                    allFillParent = allFillParent && lp.width == LayoutParams.MATCH_PARENT

                    val totalLength = totalHeight
                    totalHeight = Math.max(totalLength, totalLength + childView.getMeasuredHeight() +
                            lp.topMargin + lp.bottomMargin)
                }
            }

        }

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
                    val lp = this.layoutParams as android.widget.LinearLayout.LayoutParams
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
            val lp = childView.layoutParams as android.widget.LinearLayout.LayoutParams

            var childHeight = childView.measuredHeight
            var childWidth = childView.measuredWidth

            childTop += lp.topMargin

            childView.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)

            childTop += lp.bottomMargin + childHeight
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return android.widget.LinearLayout.LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return android.widget.LinearLayout.LayoutParams(p)
    }
}