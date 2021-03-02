/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dandroid.app.photoview

import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.graphics.Matrix.ScaleToFit
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.*
import android.view.GestureDetector.OnDoubleTapListener
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.RelativeLayout
import com.dandroid.app.R
import com.dandroid.app.photoview.Compat.postOnAnimation
import java.lang.ref.WeakReference

class PhotoViewAttacher(
    imageView: ImageView,
    fromChatActivity: Boolean,
    context: Context
) : IPhotoView, OnTouchListener, VersionedGestureDetector.OnGestureListener, OnDoubleTapListener,
    OnGlobalLayoutListener {
    private var mMinScale = DEFAULT_MIN_SCALE
    private var mMidScale = DEFAULT_MID_SCALE
    private var mMaxScale = DEFAULT_MAX_SCALE
    private var mAllowParentInterceptOnEdge = true
    private var mImageView: WeakReference<ImageView>?
    private var mViewTreeObserver: ViewTreeObserver?

    // Gesture Detectors
    private var mGestureDetector: GestureDetector? = null
    private var mScaleDragDetector: VersionedGestureDetector? = null

    // These are set so we don't keep allocating them on the heap
    private val mBaseMatrix = Matrix()
    private val mDrawMatrix = Matrix()
    private val mSuppMatrix = Matrix()
    private val mDisplayRect = RectF()
    private val mMatrixValues = FloatArray(9)

    // Listeners
    private var mMatrixChangeListener: OnMatrixChangedListener? = null
    private var mPhotoTapListener: OnPhotoTapListener? = null
    private var mViewTapListener: OnViewTapListener? = null
    private var mLongClickListener: OnLongClickListener? = null
    private var mIvTop = 0
    private var mIvRight = 0
    private var mIvBottom = 0
    private var mIvLeft = 0
    private var mCurrentFlingRunnable: FlingRunnable? =
        null
    private var mScrollEdge = EDGE_BOTH
    private var mZoomEnabled = false
    private var mScaleType = ScaleType.FIT_XY
    private val mContext: Context
    private val mFromChatActivity: Boolean
    private var mTitleBarVisible = true
    override fun canZoom(): Boolean {
        return mZoomEnabled
    }

    /**
     * Clean-up the resources attached to this object. This needs to be called
     * when the ImageView is no longer used. A good example is from
     * or from .
     */
    fun cleanup() {
        if (null != mImageView) {
            mImageView!!.get()!!.viewTreeObserver.removeGlobalOnLayoutListener(this)
        }
        mViewTreeObserver = null

        // Clear listeners too
        mMatrixChangeListener = null
        mPhotoTapListener = null
        mViewTapListener = null

        // Finally, clear ImageView
        mImageView = null
    }

    override val displayRect: RectF?
        get() {
            checkMatrixBounds()
            return getDisplayRect(displayMatrix)
        }

    // If we don't have an ImageView, call cleanup()
    val imageView: ImageView
        get() {
            var imageView: ImageView? = null
            if (null != mImageView) {
                imageView = mImageView!!.get()
            }

            // If we don't have an ImageView, call cleanup()
            if (null == imageView) {
                cleanup()
                throw IllegalStateException(
                    "ImageView no longer exists. You should not use this PhotoViewAttacher any more."
                )
            }
            return imageView
        }

    override var minScale: Float
        get() = mMinScale
        set(minScale) {
            checkZoomLevels(minScale, mMidScale, mMaxScale)
            mMinScale = minScale
        }

    override var midScale: Float
        get() = mMidScale
        set(midScale) {
            checkZoomLevels(mMinScale, midScale, mMaxScale)
            mMidScale = midScale
        }

    override var maxScale: Float
        get() = mMaxScale
        set(maxScale) {
            checkZoomLevels(mMinScale, mMidScale, maxScale)
            mMaxScale = maxScale
        }

    override val scale: Float
        get() = getValue(mSuppMatrix, Matrix.MSCALE_X)

    // Finally update
    override var scaleType: ScaleType?
        get() = mScaleType
        set(scaleType) {
            if (isSupportedScaleType(scaleType) && scaleType != mScaleType) {
                mScaleType = scaleType!!

                // Finally update
                update()
            }
        }

    override fun onDoubleTap(ev: MotionEvent): Boolean {
        try {
            val scale = scale
            val x = ev.x
            val y = ev.y
            if (scale < mMidScale) {
                zoomTo(mMidScale, x, y)
            } else if (scale >= mMidScale && scale < mMaxScale) {
                zoomTo(mMaxScale, x, y)
            } else {
                zoomTo(mMinScale, x, y)
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            // Can sometimes happen when getX() and getY() is called
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        // Wait for the confirmed onDoubleTap() instead
        return false
    }

    override fun onDrag(dx: Float, dy: Float) {
        if (DEBUG) {
            Log.d(
                LOG_TAG,
                String.format("onDrag: dx: %.2f. dy: %.2f", dx, dy)
            )
        }
        val imageView: ImageView? = imageView
        if (null != imageView && hasDrawable(imageView)) {
            mSuppMatrix.postTranslate(dx, dy)
            checkAndDisplayMatrix()
            /**
             * Here we decide whether to let the ImageView's parent to start
             * taking over the touch event.
             *
             * First we check whether this function is enabled. We never want the
             * parent to take over if we're scaling. We then check the edge we're
             * on, and the direction of the scroll (i.e. if we're pulling against
             * the edge, aka 'overscrolling', let the parent take over).
             */
            if (mAllowParentInterceptOnEdge && !mScaleDragDetector!!.isScaling) {
                if (mScrollEdge == EDGE_BOTH || mScrollEdge == EDGE_LEFT && dx >= 1f
                    || mScrollEdge == EDGE_RIGHT && dx <= -1f
                ) {
                    imageView.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
    }

    override fun onFling(
        startX: Float,
        startY: Float,
        velocityX: Float,
        velocityY: Float
    ) {
        if (DEBUG) {
            Log.d(
                LOG_TAG,
                "onFling. sX: $startX sY: $startY Vx: $velocityX Vy: $velocityY"
            )
        }
        val imageView = imageView
        if (hasDrawable(imageView)) {
            mCurrentFlingRunnable =
                FlingRunnable(imageView.context)
            mCurrentFlingRunnable!!.fling(
                imageView.width,
                imageView.height,
                velocityX.toInt(),
                velocityY.toInt()
            )
            imageView.post(mCurrentFlingRunnable)
        }
    }

    override fun onGlobalLayout() {
        val imageView: ImageView? = imageView
        if (null != imageView && mZoomEnabled) {
            val top = imageView.top
            val right = imageView.right
            val bottom = imageView.bottom
            val left = imageView.left
            /**
             * We need to check whether the ImageView's bounds have changed.
             * This would be easier if we targeted API 11+ as we could just use
             * View.OnLayoutChangeListener. Instead we have to replicate the
             * work, keeping track of the ImageView's bounds and then checking
             * if the values change.
             */
            if (top != mIvTop || bottom != mIvBottom || left != mIvLeft || right != mIvRight) {
                // Update our base matrix, as the bounds have changed
                updateBaseMatrix(imageView.drawable)

                // Update values as something has changed
                mIvTop = top
                mIvRight = right
                mIvBottom = bottom
                mIvLeft = left
            }
        }
    }

    override fun onScale(
        scaleFactor: Float,
        focusX: Float,
        focusY: Float
    ) {
        if (DEBUG) {
            Log.d(
                LOG_TAG,
                String.format(
                    "onScale: scale: %.2f. fX: %.2f. fY: %.2f",
                    scaleFactor,
                    focusX,
                    focusY
                )
            )
        }
        if (hasDrawable(imageView) && (scale < mMaxScale || scaleFactor < 1f)) {
            mSuppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            checkAndDisplayMatrix()
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        val activity = mContext as Activity
        if (mFromChatActivity) {
            activity.finish()
        } else {
            val titleRl =
                activity.findViewById<View>(R.id.title_bar_rl) as RelativeLayout
            val attrs = activity.window.attributes
            //如果标题栏，菜单栏可见，单击后隐藏并设置全屏模式
            if (mTitleBarVisible) {
                attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
                activity.window.attributes = attrs
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                titleRl.visibility = View.GONE
                mTitleBarVisible = false
                //否则显示标题栏、菜单栏，并取消全屏
            } else {
                attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
                activity.window.attributes = attrs
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                titleRl.visibility = View.VISIBLE
                mTitleBarVisible = true
            }
        }

//        ImageView imageView = getImageView();
//		if (null != imageView) {
//			if (null != mPhotoTapListener) {
//				final RectF displayRect = getDisplayRect();
//
//				if (null != displayRect) {
//					final float x = e.getX(), y = e.getY();
//
//					// Check to see if the user tapped on the image
//					if (displayRect.contains(x, y)) {
//
//						float xResult = (x - displayRect.left) / displayRect.width();
//						float yResult = (y - displayRect.top) / displayRect.height();
//
//						mPhotoTapListener.onPhotoTap(imageView, xResult, yResult);
//						return true;
//					}
//				}
//			}
//			if (null != mViewTapListener) {
//				mViewTapListener.onViewTap(imageView, e.getX(), e.getY());
//			}
//		}
        return false
    }

    override fun onTouch(v: View, ev: MotionEvent): Boolean {
        var handled = false
        if (mZoomEnabled) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    // First, disable the Parent from intercepting the touch
                    // event
                    v.parent.requestDisallowInterceptTouchEvent(true)

                    // If we're flinging, and the user presses down, cancel
                    // fling
                    cancelFling()
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP ->                    // If the user has zoomed less than min scale, zoom back
                    // to min scale
                    if (scale < mMinScale) {
                        val rect = displayRect
                        if (null != rect) {
                            v.post(
                                AnimatedZoomRunnable(
                                    scale,
                                    mMinScale,
                                    rect.centerX(),
                                    rect.centerY()
                                )
                            )
                            handled = true
                        }
                    }
            }

            // Check to see if the user double tapped
            if (null != mGestureDetector && mGestureDetector!!.onTouchEvent(ev)) {
                handled = true
            }

            // Finally, try the Scale/Drag detector
            if (null != mScaleDragDetector && mScaleDragDetector!!.onTouchEvent(ev)) {
                handled = true
            }
        }
        return handled
    }

    override fun setAllowParentInterceptOnEdge(allow: Boolean) {
        mAllowParentInterceptOnEdge = allow
    }

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        mLongClickListener = listener
    }

    override fun setOnMatrixChangeListener(listener: OnMatrixChangedListener?) {
        mMatrixChangeListener = listener
    }

    override fun setOnPhotoTapListener(listener: OnPhotoTapListener?) {
        mPhotoTapListener = listener
    }

    override fun setOnViewTapListener(listener: OnViewTapListener?) {
        mViewTapListener = listener
    }

    override fun setZoomable(zoomable: Boolean) {
        mZoomEnabled = zoomable
        update()
    }

    fun update() {
        val imageView: ImageView? = imageView
        if (null != imageView) {
            if (mZoomEnabled) {
                // Make sure we using MATRIX Scale Type
                setImageViewScaleTypeMatrix(imageView)

                // Update the base matrix using the current drawable
                updateBaseMatrix(imageView.drawable)
            } else {
                // Reset the Matrix...
                resetMatrix()
            }
        }
    }

    override fun zoomTo(
        scale: Float,
        focalX: Float,
        focalY: Float
    ) {
        val imageView: ImageView? = imageView
        imageView?.post(AnimatedZoomRunnable(scale, scale, focalX, focalY))
    }

    protected val displayMatrix: Matrix
        protected get() {
            mDrawMatrix.set(mBaseMatrix)
            mDrawMatrix.postConcat(mSuppMatrix)
            return mDrawMatrix
        }

    private fun cancelFling() {
        if (null != mCurrentFlingRunnable) {
            mCurrentFlingRunnable!!.cancelFling()
            mCurrentFlingRunnable = null
        }
    }

    /**
     * Helper method that simply checks the Matrix, and then displays the result
     */
    private fun checkAndDisplayMatrix() {
        checkMatrixBounds()
        setImageViewMatrix(displayMatrix)
    }

    private fun checkImageViewScaleType() {
        val imageView: ImageView? = imageView
        /**
         * PhotoView's getScaleType() will just divert to this.getScaleType() so
         * only call if we're not attached to a PhotoView.
         */
        if (null != imageView && imageView !is PhotoView) {
            check(imageView.scaleType == ScaleType.MATRIX) { "The ImageView's ScaleType has been changed since attaching a PhotoViewAttacher" }
        }
    }

    private fun checkMatrixBounds() {
        val imageView = imageView ?: return
        val rect = getDisplayRect(displayMatrix) ?: return
        val height = rect.height()
        val width = rect.width()
        var deltaX = 0f
        var deltaY = 0f
        val viewHeight = imageView.height
        if (height <= viewHeight) {
            deltaY = when (mScaleType) {
                ScaleType.FIT_START -> -rect.top
                ScaleType.FIT_END -> viewHeight - height - rect.top
                else -> (viewHeight - height) / 2 - rect.top
            }
        } else if (rect.top > 0) {
            deltaY = -rect.top
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom
        }
        val viewWidth = imageView.width
        if (width <= viewWidth) {
            deltaX = when (mScaleType) {
                ScaleType.FIT_START -> -rect.left
                ScaleType.FIT_END -> viewWidth - width - rect.left
                else -> (viewWidth - width) / 2 - rect.left
            }
            mScrollEdge = EDGE_BOTH
        } else if (rect.left > 0) {
            mScrollEdge = EDGE_LEFT
            deltaX = -rect.left
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right
            mScrollEdge = EDGE_RIGHT
        } else {
            mScrollEdge = EDGE_NONE
        }

        // Finally actually translate the matrix
        mSuppMatrix.postTranslate(deltaX, deltaY)
    }

    /**
     * Helper method that maps the supplied Matrix to the current Drawable
     *
     * @param matrix - Matrix to map Drawable against
     * @return RectF - Displayed Rectangle
     */
    private fun getDisplayRect(matrix: Matrix): RectF? {
        val imageView: ImageView? = imageView
        if (null != imageView) {
            val d = imageView.drawable
            if (null != d) {
                mDisplayRect[0f, 0f, d.intrinsicWidth.toFloat()] = d.intrinsicHeight.toFloat()
                matrix.mapRect(mDisplayRect)
                return mDisplayRect
            }
        }
        return null
    }

    /**
     * Helper method that 'unpacks' a Matrix and returns the required value
     *
     * @param matrix - Matrix to unpack
     * @param whichValue - Which value from Matrix.M* to return
     * @return float - returned value
     */
    private fun getValue(matrix: Matrix, whichValue: Int): Float {
        matrix.getValues(mMatrixValues)
        return mMatrixValues[whichValue]
    }

    /**
     * Resets the Matrix back to FIT_CENTER, and then displays it.s
     */
    private fun resetMatrix() {
        mSuppMatrix.reset()
        setImageViewMatrix(displayMatrix)
        checkMatrixBounds()
    }

    private fun setImageViewMatrix(matrix: Matrix) {
        val imageView: ImageView? = imageView
        if (null != imageView) {
            checkImageViewScaleType()
            imageView.imageMatrix = matrix

            // Call MatrixChangedListener if needed
            if (null != mMatrixChangeListener) {
                val displayRect = getDisplayRect(matrix)
                if (null != displayRect) {
                    mMatrixChangeListener!!.onMatrixChanged(displayRect)
                }
            }
        }
    }

    /**
     * Calculate Matrix for FIT_CENTER
     *
     * @param d - Drawable being displayed
     */
    private fun updateBaseMatrix(d: Drawable?) {
        val imageView: ImageView? = imageView
        if (null == imageView || null == d) {
            return
        }
        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()
        val drawableWidth = d.intrinsicWidth
        val drawableHeight = d.intrinsicHeight
        mBaseMatrix.reset()
        val widthScale = viewWidth / drawableWidth
        val heightScale = viewHeight / drawableHeight
        if (mScaleType == ScaleType.CENTER) {
            mBaseMatrix.postTranslate(
                (viewWidth - drawableWidth) / 2f,
                (viewHeight - drawableHeight) / 2f
            )
        } else if (mScaleType == ScaleType.CENTER_CROP) {
            val scale = Math.max(widthScale, heightScale)
            mBaseMatrix.postScale(scale, scale)
            mBaseMatrix.postTranslate(
                (viewWidth - drawableWidth * scale) / 2f,
                (viewHeight - drawableHeight * scale) / 2f
            )
        } else if (mScaleType == ScaleType.CENTER_INSIDE) {
            val scale =
                Math.min(1.0f, Math.min(widthScale, heightScale))
            mBaseMatrix.postScale(scale, scale)
            mBaseMatrix.postTranslate(
                (viewWidth - drawableWidth * scale) / 2f,
                (viewHeight - drawableHeight * scale) / 2f
            )
        } else {
            val mTempSrc = RectF(0F, 0F, drawableWidth.toFloat(), drawableHeight.toFloat())
            val mTempDst = RectF(0F, 0F, viewWidth, viewHeight)
            when (mScaleType) {
                ScaleType.FIT_CENTER -> mBaseMatrix.setRectToRect(
                    mTempSrc,
                    mTempDst,
                    ScaleToFit.CENTER
                )
                ScaleType.FIT_START -> mBaseMatrix.setRectToRect(
                    mTempSrc,
                    mTempDst,
                    ScaleToFit.START
                )
                ScaleType.FIT_END -> mBaseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.END)
                ScaleType.FIT_XY -> mBaseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.FILL)
                else -> {
                }
            }
        }
        resetMatrix()
    }

    /**
     * Interface definition for a callback to be invoked when the internal
     * Matrix has changed for this View.
     *
     * @author Chris Banes
     */
    interface OnMatrixChangedListener {
        /**
         * Callback for when the Matrix displaying the Drawable has changed.
         * This could be because the View's bounds have changed, or the user has
         * zoomed.
         *
         * @param rect - Rectangle displaying the Drawable's new bounds.
         */
        fun onMatrixChanged(rect: RectF?)
    }

    /**
     * Interface definition for a callback to be invoked when the Photo is
     * tapped with a single tap.
     *
     * @author Chris Banes
     */
    interface OnPhotoTapListener {
        /**
         * A callback to receive where the user taps on a image. You will only
         * receive a callback if the user taps on the actual image, tapping on
         * 'whitespace' will be ignored.
         *
         * @param view - View the user tapped.
         * @param x - where the user tapped from the of the Drawable, as
         * percentage of the Drawable width.
         * @param y - where the user tapped from the top of the Drawable, as
         * percentage of the Drawable height.
         */
        fun onPhotoTap(view: View?, x: Float, y: Float)
    }

    /**
     * Interface definition for a callback to be invoked when the ImageView is
     * tapped with a single tap.
     *
     * @author Chris Banes
     */
    interface OnViewTapListener {
        /**
         * A callback to receive where the user taps on a ImageView. You will
         * receive a callback if the user taps anywhere on the view, tapping on
         * 'whitespace' will not be ignored.
         *
         * @param view - View the user tapped.
         * @param x - where the user tapped from the left of the View.
         * @param y - where the user tapped from the top of the View.
         */
        fun onViewTap(view: View?, x: Float, y: Float)
    }

    private inner class AnimatedZoomRunnable(
        currentZoom: Float, private val mTargetZoom: Float, private val mFocalX: Float,
        private val mFocalY: Float
    ) : Runnable {
        private var mDeltaScale = 0f
        override fun run() {
            val imageView: ImageView? = imageView
            if (null != imageView) {
                mSuppMatrix.postScale(mDeltaScale, mDeltaScale, mFocalX, mFocalY)
                checkAndDisplayMatrix()
                val currentScale = scale
                if (mDeltaScale > 1f && currentScale < mTargetZoom
                    || mDeltaScale < 1f && mTargetZoom < currentScale
                ) {
                    // We haven't hit our target scale yet, so post ourselves
                    // again
                    postOnAnimation(imageView, this)
                } else {
                    // We've scaled past our target zoom, so calculate the
                    // necessary scale so we're back at target zoom
                    val delta = mTargetZoom / currentScale
                    mSuppMatrix.postScale(delta, delta, mFocalX, mFocalY)
                    checkAndDisplayMatrix()
                }
            }
        }

        init {
            mDeltaScale = if (currentZoom < mTargetZoom) {
                1.07f
            } else {
                0.93f
            }
        }
    }

    private inner class FlingRunnable(context: Context?) :
        Runnable {
        private val mScroller: ScrollerProxy
        private var mCurrentX = 0
        private var mCurrentY = 0
        fun cancelFling() {
            if (DEBUG) {
                Log.d(LOG_TAG, "Cancel Fling")
            }
            mScroller.forceFinished(true)
        }

        fun fling(viewWidth: Int, viewHeight: Int, velocityX: Int, velocityY: Int) {
            val rect = displayRect ?: return
            val startX = Math.round(-rect.left)
            val minX: Int
            val maxX: Int
            val minY: Int
            val maxY: Int
            if (viewWidth < rect.width()) {
                minX = 0
                maxX = Math.round(rect.width() - viewWidth)
            } else {
                maxX = startX
                minX = maxX
            }
            val startY = Math.round(-rect.top)
            if (viewHeight < rect.height()) {
                minY = 0
                maxY = Math.round(rect.height() - viewHeight)
            } else {
                maxY = startY
                minY = maxY
            }
            mCurrentX = startX
            mCurrentY = startY
            if (DEBUG) {
                Log.d(
                    LOG_TAG,
                    "fling. StartX:$startX StartY:$startY MaxX:$maxX MaxY:$maxY"
                )
            }

            // If we actually can move, fling the scroller
            if (startX != maxX || startY != maxY) {
                mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0)
            }
        }

        override fun run() {
            val imageView: ImageView? = imageView
            if (null != imageView && mScroller.computeScrollOffset()) {
                val newX = mScroller.currX
                val newY = mScroller.currY
                if (DEBUG) {
                    Log.d(
                        LOG_TAG,
                        "fling run(). CurrentX:" + mCurrentX + " CurrentY:" + mCurrentY + " NewX:" + newX
                                + " NewY:" + newY
                    )
                }
                mSuppMatrix.postTranslate(mCurrentX - newX.toFloat(), mCurrentY - newY.toFloat())
                setImageViewMatrix(displayMatrix)
                mCurrentX = newX
                mCurrentY = newY

                // Post On animation
                postOnAnimation(imageView, this)
            }
        }

        init {
            mScroller = ScrollerProxy.getScroller(context)
        }
    }

    companion object {
        const val LOG_TAG = "PhotoViewAttacher"

        // let debug flag be dynamic, but still Proguard can be used to remove from release builds
        val DEBUG =
            Log.isLoggable(LOG_TAG, Log.DEBUG)
        const val EDGE_NONE = -1
        const val EDGE_LEFT = 0
        const val EDGE_RIGHT = 1
        const val EDGE_BOTH = 2
        const val DEFAULT_MAX_SCALE = 3.0f
        const val DEFAULT_MID_SCALE = 1.75f
        const val DEFAULT_MIN_SCALE = 0.7f
        private fun checkZoomLevels(
            minZoom: Float,
            midZoom: Float,
            maxZoom: Float
        ) {
            require(minZoom < midZoom) { "MinZoom should be less than MidZoom" }
            require(midZoom < maxZoom) { "MidZoom should be less than MaxZoom" }
        }

        /**
         * @return true if the ImageView exists, and it's Drawable existss
         */
        private fun hasDrawable(imageView: ImageView?): Boolean {
            return null != imageView && null != imageView.drawable
        }

        /**
         * @return true if the ScaleType is supported.
         */
        private fun isSupportedScaleType(scaleType: ScaleType?): Boolean {
            return if (null == scaleType) {
                false
            } else when (scaleType) {
                ScaleType.MATRIX -> throw IllegalArgumentException(scaleType.name + " is not supported in PhotoView")
                else -> true
            }
        }

        /**
         * Set's the ImageView's ScaleType to Matrix.
         */
        private fun setImageViewScaleTypeMatrix(imageView: ImageView?) {
            if (null != imageView) {
                if (imageView is PhotoView) {
                    /**
                     * PhotoView sets it's own ScaleType to Matrix, then diverts all
                     * calls setScaleType to this.setScaleType. Basically we don't
                     * need to do anything here
                     */
                } else {
                    imageView.scaleType = ScaleType.MATRIX
                }
            }
        }
    }

    init {
        mImageView = WeakReference(imageView)
        mFromChatActivity = fromChatActivity
        mContext = context
        imageView.setOnTouchListener(this)
        mViewTreeObserver = imageView.viewTreeObserver
        mViewTreeObserver?.addOnGlobalLayoutListener(this)

        // Make sure we using MATRIX Scale Type
        setImageViewScaleTypeMatrix(imageView)
        if (!imageView.isInEditMode) {
            // Create Gesture Detectors...
            mScaleDragDetector = VersionedGestureDetector.newInstance(imageView.context, this)
            mGestureDetector = GestureDetector(imageView.context,
                object : SimpleOnGestureListener() {
                    // forward long click listener
                    override fun onLongPress(e: MotionEvent) {
                        if (null != mLongClickListener) {
                            mLongClickListener!!.onLongClick(mImageView!!.get())
                        }
                    }
                })
            mGestureDetector?.setOnDoubleTapListener(this)

            // Finally, update the UI so that we're zoomable
            setZoomable(true)
        }
    }
}