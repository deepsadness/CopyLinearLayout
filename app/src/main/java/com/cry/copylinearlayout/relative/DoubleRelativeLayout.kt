package com.cry.copylinearlayout.relative

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

/*
1. 不同于LinearLayout
RelativeLayout 在 onLayout内。只是简单的调用child.layout方法。进行放置。计算在onMeasure内已经完成了

2. LayoutParam
主要是对rule的支持

3.onMeasure方法

    1. mDirtyHierarchy 表示是否需要重写计算。需要的话，就会去sortChildren。
        sortChildren 里面是什么操作？
        两个维度的数组。Vertical和HHorizontal
        创建DependencyGraph。并将child都添加进去。然后进行sort

        SynchronizedPool同步的缓存池。可以通过acquire来获取
        每一个Node都有自己的View和被依赖的 dependents和依赖的dependencies


       a. getSortedViews 构建相互的依赖关系图。理清依赖关系。得到sorted的根节点。并将根节点的依赖中，移除自己？！
        防止自己依赖自己的情况出现。

            先得到所有的根节点。（只有dependents 没有 dependencies的节点），通过这步也将基本的依赖关系给确定下来了。
                先清除原来的缓存点。然后通过rules存放的位置，来找到对应的ViewId。然后构建node之间的关系

       b. 接着就对Relative的高度和宽度进行判断
        我们只要对应的三种模式
            MeasureSpec.UNSPECIFIED 爱咋咋地
            MeasureSpec.EXACTLY 听你的

       c. 计算Gravity
       b. 分别取出和计算横向和竖向的
              applyHorizontalSizeRules(params, myWidth, rules);
              measureChildHorizontal(child, params, myWidth, myHeight);

             applyHorizontalSizeRules 将rules应用上
                1. 得到的一个LEFT_OF的跟节点->left
                2. RIGHT_OF-》right
                3. ALIGN_LEFT-》right
                4. ALIGN_RIGHT-》right
                5.ALIGN_PARENT_LEFT
                6.ALIGN_PARENT_RIGHT


             measureChildHorizontal
                getChildMeasureSpec()
                从这个方法中有学习到什么思路？
                一般都是计算后，如果是确定的大小。就会直接将其修改成Exactly模式。
             child.measure()方法，让Child自己计算自己

             flag isWrapContentWidth
          positionChildHorizontal 确定之前未确定的部分


          ...








      end. setMeasuredDimension(width, height); 千万不要忘记使用这个方法！！
 */
class DoubleRelativeLayout : ViewGroup {
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

    }

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

}