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

import android.graphics.RectF
import android.view.View.OnLongClickListener
import android.widget.ImageView


interface IPhotoView {
    /**
     * Returns true if the PhotoView is set to allow zooming of Photos.
     *
     * @return true if the PhotoView allows zooming.
     */
    fun canZoom(): Boolean

    /**
     * Gets the Display Rectangle of the currently displayed Drawable. The
     * Rectangle is relative to this View and includes all scaling and
     * translations.
     *
     * @return - RectF of Displayed Drawable
     */
    val displayRect: RectF?

    /**
     * @return The current minimum scale level. What this value represents depends on the current [ImageView.ScaleType].
     */
    /**
     * Sets the minimum scale level. What this value represents depends on the current [ImageView.ScaleType].
     */
    var minScale: Float

    /**
     * @return The current middle scale level. What this value represents depends on the current [ImageView.ScaleType].
     */
    /**
     * Sets the middle scale level. What this value represents depends on the current [ImageView.ScaleType].
     */
    var midScale: Float

    /**
     * @return The current maximum scale level. What this value represents depends on the current [ImageView.ScaleType].
     */
    /**
     * Sets the maximum scale level. What this value represents depends on the current [ImageView.ScaleType].
     */
    var maxScale: Float

    /**
     * Returns the current scale value
     *
     * @return float - current scale value
     */
    val scale: Float

    /**
     * Return the current scale type in use by the ImageView.
     */
    /**
     * Controls how the image should be resized or moved to match the size of
     * the ImageView. Any scaling or panning will happen within the confines of
     * this [ImageView.ScaleType].
     *
     * @param scaleType - The desired scaling mode.
     */
    var scaleType: ImageView.ScaleType?

    /**
     * Whether to allow the ImageView's parent to intercept the touch event when the image is scroll to it's horizontal edge.
     */
    fun setAllowParentInterceptOnEdge(allow: Boolean)

    /**
     * Register a callback to be invoked when the Photo displayed by this view is long-pressed.
     *
     * @param listener - Listener to be registered.
     */
    fun setOnLongClickListener(listener: OnLongClickListener?)

    /**
     * Register a callback to be invoked when the Matrix has changed for this
     * View. An example would be the user panning or scaling the Photo.
     *
     * @param listener - Listener to be registered.
     */
    fun setOnMatrixChangeListener(listener: PhotoViewAttacher.OnMatrixChangedListener?)

    /**
     * Register a callback to be invoked when the Photo displayed by this View
     * is tapped with a single tap.
     *
     * @param listener - Listener to be registered.
     */
    fun setOnPhotoTapListener(listener: PhotoViewAttacher.OnPhotoTapListener?)

    /**
     * Register a callback to be invoked when the View is tapped with a single
     * tap.
     *
     * @param listener - Listener to be registered.
     */
    fun setOnViewTapListener(listener: PhotoViewAttacher.OnViewTapListener?)

    /**
     * Allows you to enable/disable the zoom functionality on the ImageView.
     * When disable the ImageView reverts to using the FIT_CENTER matrix.
     *
     * @param zoomable - Whether the zoom functionality is enabled.
     */
    fun setZoomable(zoomable: Boolean)

    /**
     * Zooms to the specified scale, around the focal point given.
     *
     * @param scale  - Scale to zoom to
     * @param focalX - X Focus Point
     * @param focalY - Y Focus Point
     */
    fun zoomTo(scale: Float, focalX: Float, focalY: Float)
}