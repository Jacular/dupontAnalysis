package com.dandroid.app.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

object ScreenCapture {
    /**
     * view截图，webview和scrollview(scrollview需要传入子view)之类的view能够截取整个长度的bitmap，
     * 如果webview内容很多，view.draw(Canvas)方法会很耗时，在子进程中操作会有额外的问题，所以会暂时阻塞
     */
    fun viewShot(view: View?): Bitmap? {
        if (view == null) return null
        view.isDrawingCacheEnabled = true
        view.buildDrawingCache()
        val measureSpec = View.MeasureSpec.makeMeasureSpec(
            0,
            View.MeasureSpec.UNSPECIFIED
        )
        view.measure(measureSpec, measureSpec)
        if (view.measuredWidth <= 0 || view.measuredHeight <= 0) {
            return null
        }
        val bm: Bitmap = try {
            Bitmap.createBitmap(
                view.measuredWidth,
                view.measuredHeight,
                Bitmap.Config.ARGB_8888
            )
        } catch (e: OutOfMemoryError) {
            System.gc()
            try {
                Bitmap.createBitmap(
                    view.measuredWidth,
                    view.measuredHeight,
                    Bitmap.Config.ARGB_8888
                )
            } catch (ee: OutOfMemoryError) {
                return null
            }
        }
        val bigCanvas = Canvas(bm)
        val paint = Paint()
        val iHeight = bm.height
        bigCanvas.drawBitmap(bm, 0f, iHeight.toFloat(), paint)
        view.draw(bigCanvas)
        return bm
    }
}