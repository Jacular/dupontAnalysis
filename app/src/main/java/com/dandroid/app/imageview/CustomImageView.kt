package com.dandroid.app.imageview

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.dandroid.app.R
import java.io.File

/**
 * 自定义imageView，支持读取网络图片，本地图片 ,file
 */
open class CustomImageView : AppCompatImageView {
    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!,
        attrs
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context!!, attrs, defStyleAttr) {
    }

    fun setImageUrl(url: String?) {
        try {
            val options = RequestOptions()
            options.placeholder(R.mipmap.ic_launcher)
            options.error(R.mipmap.ic_launcher)
            Glide.with(context)
                .load(url)
                .apply(options)
                .into(this)
        } catch (e: Exception) {
            setImageResource(R.mipmap.ic_launcher)
        }
    }

    fun setImageNotHolderUrl(url: String?) {
        try {
            this.visibility = View.VISIBLE
            Glide.with(context)
                .load(url)
                .into(this)
        } catch (e: Exception) {
            this.visibility = View.GONE
        }
    }

    fun setImageUrlNoScaleType(url: String?) {
        try {
            Glide.with(context)
                .load(url)
                .into(this)
        } catch (e: Exception) {
            setImageResource(R.mipmap.ic_launcher)
        }
    }

    fun setImageUrlWithoutDefaultImg(url: String?) {
        try {
            Glide.with(context)
                .load(url)
                .into(this)
        } catch (e: Exception) {
            setImageResource(R.mipmap.ic_launcher)
        }
    }

    fun setImageUrl(url: String?, placeholder: Int) {
        try {
            val options = RequestOptions()
            options.placeholder(placeholder)
            options.error(placeholder)
            Glide.with(context)
                .load(url)
                .apply(options)
                .into(this)
        } catch (e: Exception) {
            setImageResource(placeholder)
        }
    }

    fun setImageBites(bytes: ByteArray?) {
        Glide.with(context)
            .load(bytes)
            .into(this)
    }

    fun setDefaultImageUrl(url: String?, errorResId: Int) {
        try {
            Glide.with(context)
                .load(url)
                .into(this)
        } catch (e: Exception) {
            setImageResource(errorResId)
        }
    }

    fun setUserHeadImageUrl(url: String?) {
        setRoundAngle(url)
    }

    fun setUserHeadImageUrl(url: String?, width: Int, height: Int) {
        setRoundAngle(url, width, height)
    }

    fun setImageUrlAndDefaultImg(
        url: String?,
        waitLoadImg: Int,
        errorImg: Int
    ) {
        try {
            Glide.with(context)
                .load(url)
                .into(this)
        } catch (e: Exception) {
            setImageResource(R.mipmap.ic_launcher)
        }
    }

    fun setImageUrl(
        url: String?,
        width: Int,
        height: Int,
        waitLoadImg: Int,
        errorImg: Int
    ) {
        try {
            Glide.with(context)
                .load(url)
                .into(this)
        } catch (e: Exception) {
            setImageResource(R.mipmap.ic_launcher)
        }
    }

    fun setImageUrl(url: String?, image: Drawable?) {
        try {
            Glide.with(context)
                .load(url) //                .transform(new PicassoBlurTransformation(getContext(), 25f))
                .into(this)
        } catch (e: Exception) {
            setImageResource(R.mipmap.ic_launcher)
        }
    }

    fun setLoadSrcDrawable(id: Int) {
        setImageResource(id)
    }

    // 加载本地图片
    fun loadLocalImage(path: String) {
        Glide.with(context)
            .load("file://$path")
            .into(this)
    }

    fun loadLocalImageWidthResize(path: String, width: Int, height: Int) {
        val options = RequestOptions()
        Glide.with(context)
            .load("file://$path")
            .apply(options)
            .into(this)
    }

    fun loadLocalFile(file: File?, width: Int, height: Int) {
        try {
            Glide.with(context)
                .load(file)
                .into(this)
        } catch (e: Exception) {
            setImageResource(R.mipmap.ic_launcher)
        }
    }

    fun glideLoadLocalFile(file: File?, width: Int, height: Int) {
        try {
            val options = RequestOptions()
            options.placeholder(R.mipmap.ic_launcher)
            options.error(R.mipmap.ic_launcher)
            Glide.with(context)
                .load(file).apply(options)
                .into(this)
            /*  Picasso.with(getContext())
                    .load(file)
                    .resize(width, height)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(this);*/
        } catch (e: Exception) {
            setImageResource(R.mipmap.ic_launcher)
        }
    }

    fun glideLoadLocalFile(file: File?) {
        try {
            Glide.with(context)
                .load(file)
                .into(this)
            /*  Picasso.with(getContext())
                    .load(file)
                    .resize(width, height)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(this);*/
        } catch (e: Exception) {
            setImageResource(R.mipmap.ic_launcher)
        }
    }

    /**
     * 设置默认头像
     *
     * @param url
     */
    fun setPortraitImageUrl(url: String?) {
        try {
            Glide.with(context)
                .load(url)
                .into(this)
        } catch (e: Exception) {
        }
    }

    /***
     * 圆型头像
     * @param url
     */
    private fun setRoundAngle(url: String?) {
        try {
            val options = RequestOptions()
            Glide.with(context)
                .load(url)
                .into(this)
        } catch (e: Exception) {
        }
    }

    private fun setRoundAngle(url: String?, width: Int, height: Int) {
        try {
            val options = RequestOptions()
            options.apply(RequestOptions.bitmapTransform(CircleCrop()))
            Glide.with(context)
                .load(url)
                .apply(options)
                .into(this)
        } catch (e: Exception) {
        }
    }

    fun setRoundRoundAngle(url: String?, width: Int, height: Int) {
        try {
            Glide.with(context)
                .load(url)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(this)
        } catch (e: Exception) {
        }
    }

    fun setRoundRoundAngle(url: String?, place: Int) {
        try {
            val options = RequestOptions()
            options.placeholder(R.drawable.ic_launcher_background)
            options.error(place)
            //禁用掉Glide的缓存功能
            //options.diskCacheStrategy(DiskCacheStrategy.NONE);
            Glide.with(context)
                .load(url)
                .apply(options)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(20)))
                .into(this)
        } catch (e: Exception) {
            setImageResource(place)
        }
    }
}