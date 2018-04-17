package com.cry.copylinearlayout.relative

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.util.ArrayMap
import android.support.v4.util.Pools
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewDebug
import android.view.ViewGroup
import com.cry.copylinearlayout.R
import java.util.*

/**
 * 1 LayoutParams内，添加rule的属性。和left right等
 * 2.onLayout内只是简单的将 layoutParam内的属性进行布局
 * 3.在measure内进行计算
 *
 * Created by Administrator on 2018/4/17 0017.
 */
class SimpleDoubleRelativeLayout : ViewGroup {
    companion object Rules {
        const val TRUE = -1
        /**
         * Rule that aligns a child's right edge with another child's left edge.
         */
        const val LEFT_OF = 0
        /**
         * Rule that aligns a child's left edge with another child's right edge.
         */
        const val RIGHT_OF = 1
        /**
         * Rule that aligns a child's bottom edge with another child's top edge.
         */
        const val ABOVE = 2
        /**
         * Rule that aligns a child's top edge with another child's bottom edge.
         */
        const val BELOW = 3

        /**
         * Rule that aligns a child's baseline with another child's baseline.
         */
        const val ALIGN_BASELINE = 4
        /**
         * Rule that aligns a child's left edge with another child's left edge.
         */
        const val ALIGN_LEFT = 5
        /**
         * Rule that aligns a child's top edge with another child's top edge.
         */
        const val ALIGN_TOP = 6
        /**
         * Rule that aligns a child's right edge with another child's right edge.
         */
        const val ALIGN_RIGHT = 7
        /**
         * Rule that aligns a child's bottom edge with another child's bottom edge.
         */
        const val ALIGN_BOTTOM = 8

        /**
         * Rule that aligns the child's left edge with its RelativeLayout
         * parent's left edge.
         */
        const val ALIGN_PARENT_LEFT = 9
        /**
         * Rule that aligns the child's top edge with its RelativeLayout
         * parent's top edge.
         */
        const val ALIGN_PARENT_TOP = 10
        /**
         * Rule that aligns the child's right edge with its RelativeLayout
         * parent's right edge.
         */
        const val ALIGN_PARENT_RIGHT = 11
        /**
         * Rule that aligns the child's bottom edge with its RelativeLayout
         * parent's bottom edge.
         */
        const val ALIGN_PARENT_BOTTOM = 12

        /**
         * Rule that centers the child with respect to the bounds of its
         * RelativeLayout parent.
         */
        const val CENTER_IN_PARENT = 13
        /**
         * Rule that centers the child horizontally with respect to the
         * bounds of its RelativeLayout parent.
         */
        const val CENTER_HORIZONTAL = 14
        /**
         * Rule that centers the child vertically with respect to the
         * bounds of its RelativeLayout parent.
         */
        const val CENTER_VERTICAL = 15
        /**
         * Rule that aligns a child's end edge with another child's start edge.
         */
        const val START_OF = 16
        /**
         * Rule that aligns a child's start edge with another child's end edge.
         */
        const val END_OF = 17
        /**
         * Rule that aligns a child's start edge with another child's start edge.
         */
        const val ALIGN_START = 18
        /**
         * Rule that aligns a child's end edge with another child's end edge.
         */
        const val ALIGN_END = 19
        /**
         * Rule that aligns the child's start edge with its RelativeLayout
         * parent's start edge.
         */
        const val ALIGN_PARENT_START = 20
        /**
         * Rule that aligns the child's end edge with its RelativeLayout
         * parent's end edge.
         */
        const val ALIGN_PARENT_END = 21

        const val VERB_COUNT = 22

        //还需要将vertical和 horizontal的分开
        private val RULES_VERTICAL = intArrayOf(ABOVE, BELOW, ALIGN_BASELINE, ALIGN_TOP, ALIGN_BOTTOM)

        private val RULES_HORIZONTAL = intArrayOf(LEFT_OF, RIGHT_OF, ALIGN_LEFT, ALIGN_RIGHT, START_OF, END_OF, ALIGN_START, ALIGN_END)

        private val VALUE_NOT_SET = Integer.MIN_VALUE
    }


    private var mDirtyHierarchy = true
    private var mSortedVerticalChildren: Array<View?>? = null
    private var mSortedHorizontalChildren: Array<View?>? = null
    private val mGraph = DependencyGraph()

    //在requestLayout这里标记成需要重新布局
    override fun requestLayout() {
        super.requestLayout()
        mDirtyHierarchy = true
    }

    /**
     * 从两个维度来梳理相互之间的依赖关系。vertical和horizontal
     */
    private fun sortChildren() {
        //得到childCount
        var childCount = childCount
        //将两个维度的数组进行重置
        if (mSortedVerticalChildren == null || mSortedVerticalChildren!!.size != childCount) {
            mSortedVerticalChildren = arrayOfNulls(childCount)
        }

        if (mSortedHorizontalChildren == null || mSortedHorizontalChildren!!.size != childCount) {
            mSortedHorizontalChildren = arrayOfNulls(childCount)
        }

        //所有的这些依赖，都放到 DependencyGraph这个类中
        val graph = mGraph
        graph.clear()

        //先将所有的节点添加到依赖图中
        for (childIndex in 0 until childCount) {
            graph.add(getChildAt(childIndex))
        }

        //再来筛选相互关系
        graph.getSortedViews(mSortedVerticalChildren!!, RULES_VERTICAL)
        graph.getSortedViews(mSortedHorizontalChildren!!, RULES_HORIZONTAL)
    }

    private var mBaselineView: View? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mDirtyHierarchy) {
            mDirtyHierarchy = false
            //梳理相互的依赖关系
            sortChildren()
        }

        //开始真正的操作

        var offsetHorizontalAxis = false
        var offsetVerticalAxis = false

        //这两个是temp
        var myWidth = -1
        var myHeight = -1

        //这里是标记最后的w,h
        var width = 0
        var height = 0

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)


        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        //将需要的尺寸记录下来
        if (widthMode != MeasureSpec.UNSPECIFIED) {
            myWidth = widthSize
        }

        if (heightMode != MeasureSpec.UNSPECIFIED) {
            myHeight = heightSize
        }

        //如果是Exactly的话，就没有悬念了
        if (widthMode == MeasureSpec.EXACTLY) {
            width = myWidth
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = myHeight
        }

        //忽略 ignore的View

        //默认情况下，将l,t,r,b定义到最大
        var left = Integer.MAX_VALUE
        var top = Integer.MAX_VALUE
        var right = Integer.MIN_VALUE
        var bottom = Integer.MIN_VALUE


        //只要不Exactly的模式，都表示，需要用当前的view来包裹最后的view
        val isWrapContentWidth = widthMode != View.MeasureSpec.EXACTLY
        val isWrapContentHeight = heightMode != View.MeasureSpec.EXACTLY

        //同样，忽略掉 layoutDirection的支持


        //开始横向的计算
        var views = mSortedHorizontalChildren

        views?.forEach {
            it?.apply {
                if (visibility != View.GONE) {
                    val layoutParams = layoutParams
                    if (layoutParams is LayoutParams) {
                        val rules = layoutParams.mRules
                        //将横向的属性应用到child上
                        applyHorizontalSizeRules(layoutParams, myWidth, rules)
                        //经过上面。大概确定了param的left和right。下面接着计算
                        measureChildHorizontal(this, layoutParams, myWidth, myHeight)

                        //最后还要计算中轴的移动。因为在上一步已经去计算了child的宽和高了。所以下一步也能拿到MeasureWidth
                        if (positionChildHorizontal(this, layoutParams, myWidth, isWrapContentWidth)) {
                            offsetHorizontalAxis = true
                        }
                    }
                }
            }
        }

        //相同的操作对Vertical方向再来一遍
        views = mSortedVerticalChildren

        views?.forEach {
            it?.apply {
                if (visibility != View.GONE) {
                    val layoutParams = layoutParams
                    if (layoutParams is LayoutParams) {
                        val rules = layoutParams.mRules
                        //将横向的属性应用到child上
                        applyVerticalSizeRules(layoutParams, myWidth, rules)
                        //经过上面。大概确定了param的left和right。下面接着计算
                        measureChild(this, layoutParams, myWidth, myHeight)

                        //最后还要计算中轴的移动。因为在上一步已经去计算了child的宽和高了。所以下一步也能拿到MeasureWidth
                        if (positionChildVertical(this, layoutParams, myWidth, isWrapContentHeight)) {
                            offsetVerticalAxis = true
                        }


                        if (isWrapContentWidth) {
                            //进行版本兼容的操作。在4.4只有，当前的宽度，就包括rightMargin了
//                            if (isLayoutRtl()) {
//                                if (targetSdkVersion < Build.VERSION_CODES.KITKAT) {
//                                    width = Math.max(width, myWidth - params.mLeft)
//                                } else {
//                                    width = Math.max(width, myWidth - params.mLeft - params.leftMargin)
//                                }
//                            } else {
//                                if (targetSdkVersion < Build.VERSION_CODES.KITKAT) {
//                                    width = Math.max(width, params.mRight)
//                                } else {
//                                    width = Math.max(width, params.mRight + params.rightMargin)
//                                }
//                            }
                        }

                        if (isWrapContentHeight) {
                            //进行版本兼容的操作。在4.4只有，当前的宽度，就包括bottomMargin了
//                            if (targetSdkVersion < Build.VERSION_CODES.KITKAT) {
//                                height = Math.max(height, params.mBottom)
//                            } else {
//                                height = Math.max(height, params.mBottom + params.bottomMargin)
//                            }
                        }

                        //ignore的操作忽略
//                        if (child !== ignore || verticalGravity) {
//                            left = Math.min(left, params.mLeft - params.leftMargin)
//                            top = Math.min(top, params.mTop - params.topMargin)
//                        }
//
//                        if (child !== ignore || horizontalGravity) {
//                            right = Math.max(right, params.mRight + params.rightMargin)
//                            bottom = Math.max(bottom, params.mBottom + params.bottomMargin)
//                        }
                    }
                }
            }
        }

        //忽略baseLine的等一系列操作

        //确定baseLine???
        var baselineView: View? = null
        var baselineParams: LayoutParams? = null
        for (i in 0 until childCount) {
            val child = views!![i]
            if (child!!.visibility != View.GONE) {
                var childParams = child.layoutParams
                if (childParams is LayoutParams) {
                    if (baselineView == null || baselineParams == null
                            || compareLayoutPosition(childParams, baselineParams) < 0) {
                        baselineView = child
                        baselineParams = childParams
                    }
                }
            }
        }
        mBaselineView = baselineView

        if (isWrapContentWidth) {
            width += 0

            if (layoutParams != null && layoutParams.width >= 0) {
                width = Math.max(width, layoutParams.width)
            }

            width = Math.max(width, suggestedMinimumWidth)
            //注意!!!这个方便函数都会用到。就是协调当前自己计算的width和widthMeasureSpec
            width = View.resolveSize(width, widthMeasureSpec)
        }

        if (isWrapContentHeight) {
//            // Height already has top padding in it since it was calculated by looking at
            // the bottom of each child view
            height += 0

            if (layoutParams != null && layoutParams.height >= 0) {
                height = Math.max(height, layoutParams.height)
            }

            height = Math.max(height, suggestedMinimumHeight)
            height = View.resolveSize(height, heightMeasureSpec)
        }


//
//        if (isWrapContentWidth) {
//            // Width already has left padding in it since it was calculated by looking at
//            // the right of each child view
//            width += mPaddingRight
//
//            if (mLayoutParams != null && mLayoutParams.width >= 0) {
//                width = Math.max(width, mLayoutParams.width)
//            }
//
//            width = Math.max(width, suggestedMinimumWidth)
//            width = View.resolveSize(width, widthMeasureSpec)
//
//            if (offsetHorizontalAxis) {
//                for (i in 0 until count) {
//                    val child = views[i]
//                    if (child.getVisibility() != View.GONE) {
//                        val params = child.getLayoutParams() as LayoutParams
//                        val rules = params.getRules(layoutDirection)
//                        if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_HORIZONTAL] != 0) {
//                            centerHorizontal(child, params, width)
//                        } else if (rules[ALIGN_PARENT_RIGHT] != 0) {
//                            val childWidth = child.getMeasuredWidth()
//                            params.mLeft = width - mPaddingRight - childWidth
//                            params.mRight = params.mLeft + childWidth
//                        }
//                    }
//                }
//            }
//        }
//
//        if (isWrapContentHeight) {
//            // Height already has top padding in it since it was calculated by looking at
//            // the bottom of each child view
//            height += mPaddingBottom
//
//            if (mLayoutParams != null && mLayoutParams.height >= 0) {
//                height = Math.max(height, mLayoutParams.height)
//            }
//
//            height = Math.max(height, suggestedMinimumHeight)
//            height = View.resolveSize(height, heightMeasureSpec)
//
//            if (offsetVerticalAxis) {
//                for (i in 0 until count) {
//                    val child = views[i]
//                    if (child.getVisibility() != View.GONE) {
//                        val params = child.getLayoutParams() as LayoutParams
//                        val rules = params.getRules(layoutDirection)
//                        if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_VERTICAL] != 0) {
//                            centerVertical(child, params, height)
//                        } else if (rules[ALIGN_PARENT_BOTTOM] != 0) {
//                            val childHeight = child.getMeasuredHeight()
//                            params.mTop = height - mPaddingBottom - childHeight
//                            params.mBottom = params.mTop + childHeight
//                        }
//                    }
//                }
//            }
//        }
//
//        if (horizontalGravity || verticalGravity) {
//            val selfBounds = mSelfBounds
//            selfBounds.set(mPaddingLeft, mPaddingTop, width - mPaddingRight,
//                    height - mPaddingBottom)
//
//            val contentBounds = mContentBounds
//            Gravity.apply(mGravity, right - left, bottom - top, selfBounds, contentBounds,
//                    layoutDirection)
//
//            val horizontalOffset = contentBounds.left - left
//            val verticalOffset = contentBounds.top - top
//            if (horizontalOffset != 0 || verticalOffset != 0) {
//                for (i in 0 until count) {
//                    val child = views[i]
//                    if (child.getVisibility() != View.GONE && child !== ignore) {
//                        val params = child.getLayoutParams() as LayoutParams
//                        if (horizontalGravity) {
//                            params.mLeft += horizontalOffset
//                            params.mRight += horizontalOffset
//                        }
//                        if (verticalGravity) {
//                            params.mTop += verticalOffset
//                            params.mBottom += verticalOffset
//                        }
//                    }
//                }
//            }
//        }
//
//        if (isLayoutRtl()) {
//            val offsetWidth = myWidth - width
//            for (i in 0 until count) {
//                val child = views[i]
//                if (child.getVisibility() != View.GONE) {
//                    val params = child.getLayoutParams() as LayoutParams
//                    params.mLeft -= offsetWidth
//                    params.mRight -= offsetWidth
//                }
//            }
//        }

        //这个方法记得调用!!
        setMeasuredDimension(width, height)
    }


    private fun compareLayoutPosition(p1: LayoutParams, p2: LayoutParams): Int {
        val topDiff = p1.mTop - p2.mTop
        return if (topDiff != 0) {
            topDiff
        } else p1.mLeft - p2.mLeft
    }

    //将原来不确定的值，根据child计算的大小确定下来
    private fun positionChildHorizontal(child: View, params: LayoutParams, myWidth: Int,
                                        wrapContent: Boolean): Boolean {

        val rules = params.mRules

        //一边确定一边不确定
        if (params.mLeft == VALUE_NOT_SET && params.mRight != VALUE_NOT_SET) {
            // Right is fixed, but left varies
            params.mLeft = params.mRight - child.measuredWidth
        } else if (params.mLeft != VALUE_NOT_SET && params.mRight == VALUE_NOT_SET) {
            // Left is fixed, but right varies
            params.mRight = params.mLeft + child.measuredWidth
        } else if (params.mLeft == VALUE_NOT_SET && params.mRight == VALUE_NOT_SET) {
            //两边都不确定
            if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_HORIZONTAL] != 0) {
                if (!wrapContent) {
                    centerHorizontal(child, params, myWidth)
                } else {
                    params.mLeft = 0 + params.leftMargin
                    params.mRight = params.mLeft + child.measuredWidth
                }
                return true
            } else {
                params.mLeft = 0 + params.leftMargin
                params.mRight = params.mLeft + child.measuredWidth

            }
        }
        return rules[ALIGN_PARENT_END] != 0
    }


    private fun positionChildVertical(child: View, params: LayoutParams, myWidth: Int,
                                      wrapContent: Boolean): Boolean {

        val rules = params.mRules

        //一边确定一边不确定
        if (params.mTop == VALUE_NOT_SET && params.mBottom != VALUE_NOT_SET) {
            // Right is fixed, but left varies
            params.mTop = params.mBottom - child.measuredHeight
        } else if (params.mTop != VALUE_NOT_SET && params.mBottom == VALUE_NOT_SET) {
            // Left is fixed, but right varies
            params.mBottom = params.mTop + child.measuredHeight
        } else if (params.mTop == VALUE_NOT_SET && params.mBottom == VALUE_NOT_SET) {
            //两边都不确定
            if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_HORIZONTAL] != 0) {
                if (!wrapContent) {
                    centerHorizontal(child, params, myWidth)
                } else {
                    params.mTop = 0 + params.topMargin
                    params.mBottom = params.mTop + child.measuredHeight
                }
                return true
            } else {
                params.mTop = 0 + params.topMargin
                params.mBottom = params.mTop + child.measuredHeight

            }
        }
        return rules[ALIGN_PARENT_END] != 0
    }

    private fun centerHorizontal(child: View, params: LayoutParams, myWidth: Int) {
        val childWidth = child.measuredWidth
        val left = (myWidth - childWidth) / 2

        params.mLeft = left
        params.mRight = left + childWidth
    }

    //这里势必会导致，什么依赖都没有的View会计算两次？，是需要计算两次的，从两个维度上
    private fun measureChildHorizontal(child: View, params: LayoutParams, myWidth: Int, myHeight: Int) {
        params.apply {
            val childMeasureSpec = getChildMeasureSpec(
                    mLeft, mRight,
                    width,
                    leftMargin, rightMargin,
                    0, 0, myWidth)
            val childHeightMeasureSpec: Int
            if (myHeight < 0) {
                if (params.height >= 0) {
                    childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                            params.height, View.MeasureSpec.EXACTLY)
                } else {
                    //否则，就不确定。交给下一步去计算
                    childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                            0, View.MeasureSpec.UNSPECIFIED)
                }
            } else {  //如果当前的View的高度固定了，就可以Child就不可以超过这个高度了
                val maxHeight = Math.max(0, myHeight)

                val heightMode: Int = if (params.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                    View.MeasureSpec.EXACTLY
                } else {
                    View.MeasureSpec.AT_MOST
                }
                childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxHeight, heightMode)
            }

            //最后交给他计算就可以了
            child.measure(childMeasureSpec, childHeightMeasureSpec)
        }


    }


    private fun measureChild(child: View, params: LayoutParams, myWidth: Int, myHeight: Int) {
        params.apply {
            val childWidthMeasureSpec = getChildMeasureSpec(params.mLeft,
                    params.mRight, params.width,
                    params.leftMargin, params.rightMargin,
                    0, 0,
                    myWidth)
            //因为WidthMeasureSpec已经计算过了，就不用再次计算了
            val childHeightMeasureSpec = getChildMeasureSpec(params.mTop,
                    params.mBottom, params.height,
                    params.topMargin, params.bottomMargin,
                    0, 0,
                    myHeight)

            //最后交给他计算就可以了
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }


    }

    @SuppressLint("WrongConstant")
    private fun getChildMeasureSpec(childStart: Int, childEnd: Int,
                                    childSize: Int,
                                    startMargin: Int, endMargin: Int,
                                    startPadding: Int, endPadding: Int, mySize: Int): Int {
        var childSpecMode = 0
        var childSpecSize = 0

        // Negative values in a mySize value in RelativeLayout
        // measurement is code for, "we got an unspecified mode in the
        // RelativeLayout's measure spec."
        val isUnspecified = mySize < 0
        if (isUnspecified) {
            //如果已经有值，表示已经找到锚点
            if (childStart != VALUE_NOT_SET && childEnd != VALUE_NOT_SET) {
                // Constraints fixed both edges, so child has an exact size.
                childSpecSize = Math.max(0, childEnd - childStart)
                childSpecMode = View.MeasureSpec.EXACTLY
            } else if (childSize >= 0) {//宽度大于0,则表示也是被设定了固定的宽度了
                // The child specified an exact size.
                childSpecSize = childSize
                childSpecMode = View.MeasureSpec.EXACTLY
            } else {
                //如果上面的情况都没有遇到。则想要多大，给多大
                // Allow the child to be whatever size it wants.
                childSpecSize = 0
                childSpecMode = View.MeasureSpec.UNSPECIFIED
            }

            return View.MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode)
        }

        //不是这种unspecifed模式的话
        // Figure out start and end bounds.
        var tempStart = childStart
        var tempEnd = childEnd

        //计算出最大可以用的空间
        // If the view did not express a layout constraint for an edge, use
        // view's margins and our padding
        if (tempStart == VALUE_NOT_SET) {
            tempStart = startPadding + startMargin
        }
        if (tempEnd == VALUE_NOT_SET) {
            tempEnd = mySize - endPadding - endMargin
        }

        // Figure out maximum size available to this view
        val maxAvailable = tempEnd - tempStart

        //优先判断自己的left和right这属性
        if (childStart != VALUE_NOT_SET && childEnd != VALUE_NOT_SET) {
            //如果left和right已经确定，
            childSpecMode = if (isUnspecified) View.MeasureSpec.UNSPECIFIED else View.MeasureSpec.EXACTLY
            childSpecSize = Math.max(0, maxAvailable)
        } else {
            //进入常规的判断
            if (childSize >= 0) {
                //大于0，就是表示需要确定的尺寸。但是是不让大于我们自己的可以用尺寸的
                childSpecMode = View.MeasureSpec.EXACTLY
                //maxAvailable<0，则表示自己也没有确定的大小。
                if (maxAvailable >= 0) {
                    childSpecSize = Math.min(maxAvailable, childSize)
                } else {
                    childSpecSize = childSize
                }
            } else if (childSize == ViewGroup.LayoutParams.MATCH_PARENT) {
                //给他尽可能的空间
                childSpecMode = if (isUnspecified) View.MeasureSpec.UNSPECIFIED else View.MeasureSpec.EXACTLY
                childSpecSize = Math.max(0, maxAvailable)
            } else if (childSize == ViewGroup.LayoutParams.WRAP_CONTENT) {
                // Child wants to wrap content. Use AT_MOST to communicate
                // available space if we know our max size.
                if (maxAvailable >= 0) {
                    // We have a maximum size in this dimension.
                    childSpecMode = View.MeasureSpec.AT_MOST
                    childSpecSize = maxAvailable
                } else {
                    // We can grow in this dimension. Child can be as big as it
                    // wants.
                    childSpecMode = View.MeasureSpec.UNSPECIFIED
                    childSpecSize = 0
                }
            }
        }

        return View.MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode)
    }

    //通过这方法来计算left和right
    private fun applyHorizontalSizeRules(childParams: LayoutParams, myWidth: Int, childRules: IntArray) {

        childParams.mLeft = VALUE_NOT_SET
        childParams.mRight = VALUE_NOT_SET

        var anchorParams: LayoutParams?

        //找到当前View对应的第一个LeftOf点
        anchorParams = getRelatedViewParams(childRules, LEFT_OF)

        if (anchorParams != null) {
            childParams.mRight = anchorParams.mLeft - (anchorParams.leftMargin + childParams.rightMargin)
        }

        //当前只支持上面的。所以省略一大堆代码

    }

    private fun applyVerticalSizeRules(childParams: LayoutParams, myHeight: Int, childRules: IntArray) {
        //初始化参数
        childParams.mTop = VALUE_NOT_SET
        childParams.mBottom = VALUE_NOT_SET

        var anchorParams: LayoutParams?

        //找到当前View对应的第一个ABOVE点
        anchorParams = getRelatedViewParams(childRules, ABOVE)

        if (anchorParams != null) {
            childParams.mTop = anchorParams.mTop - (anchorParams.topMargin + childParams.bottomMargin)
        }

        //当前只支持上面的。所以省略一大堆代码

    }

    private fun getRelatedViewParams(childRules: IntArray, relation: Int): LayoutParams? {
        val view = getRelatedView(childRules, relation)
        view?.apply {
            val layoutParams = layoutParams
            if (layoutParams is LayoutParams) {
                return layoutParams
            }
        }
        return null
    }

    private fun getRelatedView(childRules: IntArray, relation: Int): View? {
        //取出id,判断当前的rule是否有view
        val id = childRules[relation]

        if (id != 0) {
            //然后去依赖图中，取得相应的node
            val node = mGraph.mKeyNodes[id]

            node?.also {
                var v = it.view
                v?.apply {
                    //找到链上第一个可见的View返回
                    while (visibility == View.GONE) {
                        val layoutParams = layoutParams
                        if (layoutParams is LayoutParams) {
                            val innerRules = layoutParams.mRules
                            //如果为null，说明找不到下一个了，就直接返回了
                            val innerNode = mGraph.mKeyNodes[innerRules[relation]] ?: return null
                            v = innerNode.view
                        }
                    }
                }
                return v
            }
        }

        return null
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (changed) {
            var count = childCount
            for (index in 0 until count) {
                val child = getChildAt(index)
                if (child.visibility != View.GONE) {
                    val layoutParams = child.layoutParams
                    if (layoutParams is SimpleDoubleRelativeLayout.LayoutParams) {
                        val childLeft = layoutParams.mLeft
                        val childRight = layoutParams.mRight
                        val childTop = layoutParams.mTop
                        val childBottom = layoutParams.mBottom
                        child.layout(childLeft, childTop, childRight, childBottom)
                    }
                }
            }
        }
    }

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}


    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return SimpleDoubleRelativeLayout.LayoutParams(p!!)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return SimpleDoubleRelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return SimpleDoubleRelativeLayout.LayoutParams(context, attrs!!)
    }

    class LayoutParams : ViewGroup.MarginLayoutParams {
        @ViewDebug.ExportedProperty(
                category = "layout",
                resolveId = true,
                indexMapping =
                arrayOf(
                        ViewDebug.IntToString(
                                from = ABOVE, to = "above"),
                        ViewDebug.IntToString(from = ALIGN_BASELINE, to = "alignBaseline"),
                        ViewDebug.IntToString(from = ALIGN_BOTTOM, to = "alignBottom"),
                        ViewDebug.IntToString(from = ALIGN_LEFT, to = "alignLeft"),
                        ViewDebug.IntToString(from = ALIGN_PARENT_BOTTOM, to = "alignParentBottom"),
                        ViewDebug.IntToString(from = ALIGN_PARENT_LEFT, to = "alignParentLeft"),
                        ViewDebug.IntToString(from = ALIGN_PARENT_RIGHT, to = "alignParentRight"),
                        ViewDebug.IntToString(from = ALIGN_PARENT_TOP, to = "alignParentTop"), ViewDebug.IntToString(from = ALIGN_RIGHT, to = "alignRight"),
                        ViewDebug.IntToString(from = ALIGN_TOP, to = "alignTop"), ViewDebug.IntToString(from = BELOW, to = "below"),
                        ViewDebug.IntToString(from = CENTER_HORIZONTAL, to = "centerHorizontal"), ViewDebug.IntToString(from = CENTER_IN_PARENT, to = "center"),
                        ViewDebug.IntToString(from = CENTER_VERTICAL, to = "centerVertical"), ViewDebug.IntToString(from = LEFT_OF, to = "leftOf"),
                        ViewDebug.IntToString(from = RIGHT_OF, to = "rightOf"), ViewDebug.IntToString(from = ALIGN_START, to = "alignStart"),
                        ViewDebug.IntToString(from = ALIGN_END, to = "alignEnd"),
                        ViewDebug.IntToString(from = ALIGN_PARENT_START, to = "alignParentStart"),
                        ViewDebug.IntToString(from = ALIGN_PARENT_END, to = "alignParentEnd"),
                        ViewDebug.IntToString(from = START_OF, to = "startOf"),
                        ViewDebug.IntToString(from = END_OF, to = "endOf")), mapping = arrayOf(ViewDebug.IntToString(from = TRUE, to = "true"),
                ViewDebug.IntToString(from = 0, to = "false/NO_ID")))
        val mRules = IntArray(VERB_COUNT)
        val mInitialRules = IntArray(VERB_COUNT)

        private var mRulesChanged = false

        //layout的时候，会根据他进行摆放。所以在进行操作
        var mLeft = 0
        var mRight = 0
        var mTop = 0
        var mBottom = 0


        constructor(w: Int, h: Int) : super(w, h)
        constructor(source: ViewGroup.LayoutParams) : super(source)
        constructor(source: ViewGroup.MarginLayoutParams) : super(source)
        constructor(source: LayoutParams) : super(source) {
//            this.mIsRtlCompatibilityMode = source.mIsRtlCompatibilityMode
//            this.mRulesChanged = source.mRulesChanged
//            this.alignWithParent = source.alignWithParent

            System.arraycopy(source.mRules, LEFT_OF, this.mRules, LEFT_OF, VERB_COUNT)
            System.arraycopy(
                    source.mInitialRules, LEFT_OF, this.mInitialRules, LEFT_OF, VERB_COUNT)

        }

        constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SimpleDoubleRelativeLayout)

            val rules = mRules
            val initialRules = mInitialRules

            //遍历取出.如果存在，则讲对应的id存放到rules数组里面
            val N = typedArray.indexCount
            for (index in 0 until N) {
                val attr = typedArray.getIndex(index)
                when (attr) {
                    R.styleable.SimpleDoubleRelativeLayout_layout_toLeftOf -> rules[LEFT_OF] = typedArray.getResourceId(attr, 0)
                    R.styleable.SimpleDoubleRelativeLayout_layout_above -> rules[ABOVE] = typedArray.getResourceId(attr, 0)
                }
            }

            System.arraycopy(rules, LEFT_OF, initialRules, LEFT_OF, VERB_COUNT)
            mRulesChanged = true
            typedArray.recycle()
        }
    }

    private open class DependencyGraph {
        //所有的在视图中的View
        var mNodes = ArrayList<Node>()

        /*
        这里得keyNodes 的key 是 id value是Node.
        存放的是所有有id的node.也是只有有id的View才能进行布局
         */
        var mKeyNodes = SparseArray<Node>()

        /*
        这里是根节点。临时的缓存
        由于官方更推荐使用AarryDeque用作栈和队列 循环数组
         */
        var mRoots = ArrayDeque<Node>()

        fun clear(): Unit {
            val nodes = mNodes
            val count = nodes.size
            for (index in 0 until count) {
                nodes[index].release()
            }
            nodes.clear()
            mKeyNodes.clear()
            mRoots.clear()
        }

        /*
        添加个View,就是去获取节点。添加节点
         */
        fun add(view: View) {
            val id = view.id
            val node = Node.acquire(view)
            if (id != View.NO_ID) {
                mKeyNodes.put(id, node)
            }
            mNodes.add(node)
        }

        /*
        对传入的数组进行过滤。计算成view组成的节点之间的依赖关系
         */
        fun getSortedViews(sorted: Array<View?>, rules: IntArray): Unit {
            val roots = findRoots(rules)
            var index = 0

            var node: Node?
            node = roots.pollLast()
            do {
                node?.also {
                    val view = it.view
                    val key = view!!.id

                    sorted[index++] = view

                    val dependents = it.dependents
                    val count = dependents.size
                    for (i in 0 until count) {
                        //将依赖的点的依赖移除掉
                        val dependent = dependents.keyAt(i)
                        val dependencies = dependent.dependencies
                        dependencies.remove(key)
                        if (dependencies.size() == 0) { //只要发生这种情况，下面就会报异常。因为当前因为是循环依赖的情况
                            roots.add(dependent)
                        }
                    }
                }
                node = roots.pollLast()
            } while (node != null)

            if (index < sorted.size) {
                throw IllegalStateException("Circular dependencies cannot exist in RelativeLayout")
            }

        }

        /*
        根据filter内的规则，找到所有的根节点。根节点是 dependencies为空的节点
         */
        private fun findRoots(rulesFilter: IntArray): ArrayDeque<Node> {
            //得到所有可能可以进行布局的node
            val keyNodes = mKeyNodes
            //所有的node
            val nodes = mNodes

            val count = nodes.size

            //开始之前，需要将所有的依赖关系都清空
            for (index in 0 until count) {
                val node = nodes[index]
                node.dependencies.clear()
                node.dependents.clear()
            }

            for (index in 0 until count) {
                val node = nodes[index]
                //取出LayoutParams，对rule进行操作
                val layoutParams = node.view!!.layoutParams
                if (layoutParams is LayoutParams) {
                    val rules = layoutParams.mRules
                    val rulesCount = rulesFilter.size

                    for (filterIndex in 0 until rulesCount) {
                        //取出对应的rule是否存在id
                        val ruleIndex = rulesFilter[filterIndex]
                        val id = rules[ruleIndex]

                        if (id > 0) {   //存在id
                            //从KeyNode中取到对应的Node.这个Node就是当前Node的依赖
                            val dependency = keyNodes[id]
                            if (dependency == null || dependency == node) {
                                //如果为空，或者就是自己，就过掉
                                continue
                            }
                            //建立相互的关系。将依赖的 dependents关联到当前
                            //将 当前的依赖指向 dependency
                            dependency.dependents.put(node, this)
                            node.dependencies.put(id, dependency)
                        }

                    }
                }

            }

            /*
            处理完相互的关系后，重新填词roots数组
             */
            val roots = mRoots
            roots.clear()

            for (index in 0 until count) {
                val node = nodes[index]
                if (node.dependencies.size() == 0) {
                    roots.addLast(node)
                }
            }

            return roots
        }

        //这里面放了所有的视图的节点
        @SuppressLint("SticFieldLeak")
        class Node private constructor() {
            //表达这个node中的View
            var view: View? = null

            //所有的依赖于这个node的关系node
            var dependents = ArrayMap<Node, DependencyGraph>()

            //这个节点依赖的Node
            var dependencies = SparseArray<Node>()

            //是否所有的缓存
            fun release() {
                view = null
                dependents.clear()
                dependencies.clear()

                sPool.release(this)
            }

            //存放一个Node的缓存池
            companion object POOL {
                const val POOL_LIMIT = 100
                val sPool = Pools.SynchronizedPool<Node>(POOL_LIMIT)

                fun acquire(view: View): Node {
                    var node = sPool.acquire()
                    if (node == null) {
                        node = Node()
                    }
                    node.view = view
                    return node
                }
            }


        }

    }


}