package com.digitallive.leddisplay.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.Timer
import java.util.TimerTask
import kotlin.math.ceil


/**
 * The `TickerView` class is a complex view which contains a `LinearLayout` which may contain any number of `View`s passed to it,
 * to be shown in horizontal layout.
 *
 *
 * These child `View`s scroll horizontally in the main view holder, from left to right.
 * The speed of the view scrolling can be controlled by setting up the `displacement` value of views. Also can be controlled by user by finger gesture / sling motion.
 *
 * @see HorizontalScrollView
 */
class TickerView : HorizontalScrollView {

    private var displacement = 1
    private var scrollPos = 0
    private var scrollTimer: Timer? = null
    private var scrollerSchedule: TimerTask? = null
    private var lastTicker: TextView? = null
    private var childViews: MutableList<View?>? = null
    private var linearLayout: LinearLayout? = null
    private var onTickerScrollCompleteListener: OnTickerScrollCompleteListener? = null

    constructor(context: Context?) : super(context) {
        init(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, null)
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, null)
    }

    /**
     * If the views are added to the container view, the tickers start showing up. This method calls `showTickers()`
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        showTickers()
    }

    /**
     * If the views are added to the container view, this method, removed all scheduled `TimerTask`s by calling `destroyAllScheduledTasks()`
     */
    override fun onDetachedFromWindow() {
        destroyAllScheduledTasks()
        super.onDetachedFromWindow()
    }


    /**
     * This method initialized the View container by adding a horizontal `LinearLayout` onside the root view.
     *
     * @param context      `Context`
     * @param attributeSet `AttributeSet`
     */
    private fun init(context: Context?, attributeSet: AttributeSet?) {
        val linearLayout = LinearLayout(context)
        linearLayout.setLayoutParams(
            LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )
        linearLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.gravity = Gravity.CENTER_VERTICAL
        isHorizontalFadingEdgeEnabled = false
        isVerticalFadingEdgeEnabled = false
        foregroundGravity = Gravity.CENTER_VERTICAL
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.background_dark))
        setDisplacement(displacement)
    }

    /**
     * Use this method to get the speed of sticker movement, displacement speed of ticker views.
     *
     * @return the speed of view's rate of displacement
     */
    fun getDisplacement(): Int {
        return displacement
    }

    /**
     * This method is used to set the variable speed of displacement of auto-scrolling of views.
     *
     * @param displacement value by which the auto-scrolling displacement occurs
     */
    fun setDisplacement(displacement: Int) {
        this.displacement = ceil((displacement) * 5.0 / 100.0).toInt()
    }


    /**
     * Saves the views collection to be plotted in the ticker view.
     *
     * @param childViews `List<View>` which contains all the views to be added into the `TickerView`
     */
    fun setChildViews(childViews: MutableList<View?>?) {
        this.childViews = childViews
    }

    /**
     * User may individually add views to be shown into the `TickerView`
     *
     * @param childView `View` to be added as child to the `TickerView`.
     */
    fun addChildView(childView: View?) {
        if (childViews == null) {
            childViews = ArrayList<View?>()
        }
        this.childViews!!.add(childView)
    }

    /**
     * Sets the listener to be called when the scrolling of views is completed.
     *
     * @param listener `OnTickerScrollCompleteListener`
     */
    fun setOnTickerScrollCompleteListener(listener: OnTickerScrollCompleteListener?) {
        this.onTickerScrollCompleteListener = listener
    }

    /**
     * This method to show the child views added by `addChildView()` or `setChildViews(List<View> childViews)` methods.
     * Method Details:
     * 1. This method first removes any older child views already shown inside the `TickerView`
     * 2. Adds all the passed views to the `TickerView` by the methods `addChildView()` or `setChildViews(List<View> childViews)`.
     * 3. Corresponding Layout params are set for all the views, the first and last components are set with width equal to screen width, so that the view scrooling starts at left side of screen and the last component completes the cycle at rigjt end of screen, then only the new cycle begins.
     * 4. An empty marker view is placed at the end of the view list so that, as soon as the ScrollBounds of that view comes into the `Rect`, we start a new cycle and starts scrolling form the first record.
     * 5. We add `ViewTreeObserver.OnGlobalLayoutListener` to intercept changes in global layout and start auto scrolling by calling `startAutoScrolling()` method.
     */
    @Suppress("deprecation")
    fun showTickers() {
        if (linearLayout != null) {
            linearLayout!!.removeAllViews()
        }
        removeAllViewsInLayout()
        linearLayout = LinearLayout(context)

        val lpPar = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val lpPar0 = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val lpParLast = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        linearLayout!!.setLayoutParams(
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        lpPar.setMargins(10, 3, 5, 3)
        lpPar0.setMargins(
            (context as Activity).windowManager.defaultDisplay.width, 3, 5, 3
        )
        lpParLast.setMargins(
            5,
            3,
            (context as Activity).windowManager.defaultDisplay.width,
            3
        )
        if (childViews != null && childViews!!.isNotEmpty()) {
            if (childViews!!.size == 1) {
                // Add the one real view
                linearLayout!!.addView(childViews!![0], lpPar0)

                // Add a transparent/invisible filler to enable scrolling logic
                val fillerView = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                    )
                    text = " "
                    alpha = 0f
                }
                linearLayout!!.addView(fillerView, lpParLast)
            } else {
                for (index in childViews!!.indices) {
                    when (index) {
                        0 -> linearLayout!!.addView(childViews!![index], lpPar0)
                        childViews!!.size - 1 -> linearLayout!!.addView(childViews!![index], lpParLast)
                        else -> linearLayout!!.addView(childViews!![index], lpPar)
                    }
                }
            }

            lastTicker = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )
                visibility = INVISIBLE
            }
            linearLayout!!.addView(lastTicker, lpPar)

            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    startAutoScrolling()
                }
            })

            addView(linearLayout)
        }
    }

    /**
     * This method initiates scheduler for auto scrolling, If there is already a scheduled `Timer`, this method will first cancel the timer and reschedule it. On Regular intervals,
     * this `Timer` runs a `moveScrollView()` method on UI thread to scroll the views by the provided `getDisplacement()` value.
     *
     *
     * This only schedules a timer if the `getDisplacement()` value is greater than 0.
     */
    fun startAutoScrolling() {
        scrollTimer?.cancel()
        scrollTimer = Timer()

        val timerTick: Runnable = object : Runnable {
            override fun run() {
                moveScrollView()
            }
        }

        scrollerSchedule?.cancel()
        scrollerSchedule = null

        scrollerSchedule = object : TimerTask() {
            override fun run() {
                try {
                    (context as Activity).runOnUiThread(timerTick)
                } catch (e: Exception) {
                    Log.e("TickerView", "run: ", e)
                    this.cancel()
                }
            }
        }

        if (displacement > 0) {
            scrollTimer!!.schedule(scrollerSchedule, 30, 30)
        }
    }

    /**
     * All the smooth scrolling/moving logic is implemented in this method. This method moves the Ticker views by the provided displacement value.
     *
     *
     * The logic in place is to get a `Rect` and set it for custom last marker ticker(invisible). An additional `Rect` object is created whose horizontal bounds are
     * for how much the view has scrolled added to its width. Once we have both of these objects, we simply check if the `Rect`object for last custom ticker view (invisible)
     * intersects with  the current screen `Rect` object, as soon as the condition is met, which means the last hidden view has scrolled on to the screen, which in tern means all the ticker items have been shown, we set the scroll position to 0 again,, and the scrolling starts from the first Tivker view again.
     */
    fun moveScrollView() {
        try {
            scrollPos = (scrollX + displacement)

            val bounds = Rect()
            lastTicker!!.getHitRect(bounds)

            val scrollBounds = Rect(scrollX, scrollY, scrollX + width, scrollY + height)

            if (Rect.intersects(scrollBounds, bounds)) {
                // Last item is visible, reset the scroll position and restart scrolling
                scrollPos = 0
                scrollTo(scrollPos, 0)

                // Notify the listener that all items have finished scrolling
                onTickerScrollCompleteListener?.onTickerScrollComplete();
            } else {
                smoothScrollTo(scrollPos, 0)
            }
        } catch (e: Exception) {
            Log.e("TickerView", "moveScrollView: ", e)
        }
    }

    /**
     * This method resets the scroll position to 0.
     */
    fun resetScrollPosition() {
        scrollPos = 0
        scrollTo(0, 0)
    }

    /**
     * This method cancels `Timer` and `TimerTask` and there callback.
     */
    fun destroyAllScheduledTasks() {
        clearTimerTask(scrollerSchedule)
        clearTimers(scrollTimer)

        scrollerSchedule = null
        scrollTimer = null
    }

    /**
     * This method cancels `Timer`.
     */
    private fun clearTimers(timer: Timer?) {
        timer?.cancel()
    }

    /**
     * This method cancels `TimerTask`.
     */
    private fun clearTimerTask(timerTask: TimerTask?) {
        timerTask?.cancel()
    }

    // Prevent touch interaction
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean = false
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = false

    interface OnTickerScrollCompleteListener {
        fun onTickerScrollComplete()
    }
}
