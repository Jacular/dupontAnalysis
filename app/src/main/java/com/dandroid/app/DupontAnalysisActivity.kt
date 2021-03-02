package com.dandroid.app

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dandroid.app.photoview.PhotoView
import com.dandroid.app.screen.ScreenCapture
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
//杜邦分析，可用于组织架构图生成
class DupontAnalysisActivity : AppCompatActivity() {
    private val colors =
        intArrayOf(R.color.purple_200, R.color.purple_500, R.color.purple_700)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }
    private fun initView(){
        ivClose.setOnClickListener { finish() }
        tvTitleContent.text = "杜邦分析，可用于组织架构图生成"
        netAssetsIncomeRate.text = "50%"
        netTotalAssetsRate.text = "30%"
        equityMultiplier.text = "5"
        netSalesRate.text = "8%"
        netSalesRate.setTextColor(ContextCompat.getColor(this, colors[0]))
        totalAssetTurnover.text = "6%"
        totalAssetTurnover.setTextColor(ContextCompat.getColor(this, colors[1]))
        assetLiabilityRatio.text = "2%"
        assetLiabilityRatio.setTextColor(ContextCompat.getColor(this, colors[2]))
        llNetAssetsIncomeRate.setBackgroundColor(ContextCompat.getColor(this, colors[0]))
        Observable.timer(1000, TimeUnit.MILLISECONDS)
            .observeOn(Schedulers.io())
            .map{
                ScreenCapture.viewShot(hvScrollview)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{
                if(it!=null){
                    setImageBitmap(it)
                }
            }
    }
    private fun setImageBitmap(bitmap: Bitmap?) {
        hvScrollview.visibility = View.GONE
       val pvImage = PhotoView(false, this)
        pvImage.setImageBitmap(bitmap)
        clDuPontanAlysisPhoto.addView(pvImage)
    }
}