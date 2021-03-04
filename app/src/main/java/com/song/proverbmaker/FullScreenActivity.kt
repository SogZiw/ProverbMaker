package com.song.proverbmaker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cn.iwgang.simplifyspan.SimplifySpanBuild
import cn.iwgang.simplifyspan.unit.SpecialTextUnit
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import com.hjq.permissions.Permission
import com.hjq.toast.ToastUtils
import com.song.proverbmaker.aop.Permissions
import com.song.proverbmaker.extension.dp2px
import kotlinx.android.synthetic.main.activity_full_screen.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Title: com.song.proverbmaker
 * Description:
 * Copyright:Copyright(c) 2021
 * CreateTime:2021/03/03 15:28
 *
 * @author SogZiw
 * @version 1.0
 */
class FullScreenActivity : AppCompatActivity(), View.OnLongClickListener {

    private val mAdapter: HorizontalAdapter by lazy {
        HorizontalAdapter()
    }
    private lateinit var msc: MediaScannerConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initImmersionBar()
        setContentView(R.layout.activity_full_screen)
        init()
        initData()
        layoutContent.setOnLongClickListener(this)

    }

    private fun initImmersionBar() {
        ImmersionBar.with(this).hideBar(BarHide.FLAG_HIDE_STATUS_BAR).init()
    }

    private fun init() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        proverbList.layoutManager = layoutManager
        proverbList.adapter = mAdapter
    }

    private fun initData() {
        val margin = intent.getIntExtra("margin", 60)
        val layoutParams = marginView.layoutParams
        layoutParams.height = dp2px(margin.toFloat())
        marginView.layoutParams = layoutParams

        val selectBgColor = intent.getIntExtra("selectBgColor", Color.parseColor("#FFFADF4B"))
        val selectFontColor = intent.getIntExtra("selectFontColor", Color.parseColor("#FF000000"))
        layoutContent.setBackgroundColor(selectBgColor)
        mAdapter.setTextColor(selectFontColor)
        tvExplanation.setTextColor(selectFontColor)

        val data = intent.getParcelableArrayListExtra<SingleText>("dataList")
        val textExplanation = intent.getStringExtra("explanation")
        mAdapter.setList(data ?: ArrayList())
        if (!TextUtils.isEmpty(textExplanation?.trim())) {
            val simplifySpan =
                SimplifySpanBuild().append(SpecialTextUnit("释义：").setTextStyle(Typeface.BOLD))
                    .append(textExplanation ?: "")
                    .build()
            tvExplanation.text = simplifySpan
        }
    }

    override fun onLongClick(v: View?): Boolean {
        if (v?.id == R.id.layoutContent) {
            save()
        }
        return true
    }

    /** 获取 View 的截图*/
    private fun getCacheBitmapFromView(view: View): Bitmap? {
        view.isDrawingCacheEnabled = true
        view.buildDrawingCache(true)
        val drawingCache = view.drawingCache
        val bitmap: Bitmap?
        if (drawingCache != null) {
            bitmap = Bitmap.createBitmap(drawingCache)
            view.isDrawingCacheEnabled = false
        } else {
            bitmap = null
        }
        return bitmap
    }

    private fun saveImageToGallery(): Boolean {
        val bitmap = getCacheBitmapFromView(findViewById(R.id.layoutContent)) ?: return false
        var folder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Pictures"
        )
        if (!folder.exists() || !folder.isDirectory) {
            if (!folder.mkdirs()) {
                folder = Environment.getExternalStorageDirectory()
            }
        }
        val file = File(folder, System.currentTimeMillis().toString() + "_img.jpg")
        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        notifyGallery(file)
        return true
    }

    private fun notifyGallery(file: File) {
        try {
            //使用两种方式刷新图库，避免某一种刷新失败
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
            msc.connect()
        } catch (ignored: Exception) {
        }
    }

    @Permissions(Permission.MANAGE_EXTERNAL_STORAGE)
    private fun save() {
        if (saveImageToGallery()) {
            ToastUtils.show("保存成功")
        } else {
            ToastUtils.show("保存失败")
        }
    }

}