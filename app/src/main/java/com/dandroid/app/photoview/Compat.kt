package com.dandroid.app.photoview

import android.os.Build.VERSION
import android.view.View

object Compat {
    private const val SIXTY_FPS_INTERVAL = 1000 / 60
    @JvmStatic
	fun postOnAnimation(view: View, runnable: Runnable?) {
        if (VERSION.SDK_INT >= 16) {
            SDK16.postOnAnimation(view, runnable)
        } else {
            view.postDelayed(runnable, SIXTY_FPS_INTERVAL.toLong())
        }
    }
}