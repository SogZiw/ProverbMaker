package com.song.proverbmaker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cn.iwgang.simplifyspan.SimplifySpanBuild
import cn.iwgang.simplifyspan.unit.SpecialTextUnit
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.hjq.permissions.Permission
import com.hjq.toast.ToastUtils
import com.song.proverbmaker.aop.Permissions
import com.song.proverbmaker.aop.SingleClick
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_single_text.view.*
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val mAdapter: HorizontalAdapter by lazy {
        HorizontalAdapter()
    }
    private lateinit var msc: MediaScannerConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        btnMake.setOnClickListener(this)
        btnSave.setOnClickListener(this)
    }

    private fun init() {
        layoutContent.visibility = View.GONE
        btnSave.visibility = View.GONE
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        proverbList.layoutManager = layoutManager
        proverbList.adapter = mAdapter
    }

    @SingleClick
    override fun onClick(v: View?) {
        if (v?.id == R.id.btnMake) {
            hideSoftKeyboard()
            make()
        } else if (v?.id == R.id.btnSave) {
            save()
        }
    }

    private fun checkIsNotEmpty(): Boolean {
        if (TextUtils.isEmpty(etProverb.text.toString().trim())) {
            ToastUtils.show("输入框不能为空")
            return false
        }
        return true
    }

    private fun make() {
        if (!checkIsNotEmpty()) {
            return
        }
        val proverbCollection = etProverb.text.toString().toCharArray()
        val proverbList = etProverb.text.toString().toCharArray().map {
            it.toString()
        }.toMutableList()
        val defaultFormat = HanyuPinyinOutputFormat()
        defaultFormat.caseType = HanyuPinyinCaseType.LOWERCASE
        defaultFormat.toneType = HanyuPinyinToneType.WITH_TONE_MARK
        defaultFormat.vCharType = HanyuPinyinVCharType.WITH_U_UNICODE

        val data: MutableList<SingleText> = ArrayList()
        for (i in proverbList.indices) {
            var pinyin = ""
            try {
                val pinyinArray =
                    PinyinHelper.toHanyuPinyinStringArray(proverbCollection[i], defaultFormat)
                if (!pinyinArray.isNullOrEmpty()) {
                    pinyin = pinyinArray[0].toString()
                }
            } catch (e: Exception) {
                pinyin = ""
            }
            data.add(SingleText(proverbList[i], pinyin))
        }
        mAdapter.setList(data)
        if (!TextUtils.isEmpty(etExplanation.text.toString().trim())) {
            val simplifySpan =
                SimplifySpanBuild().append(SpecialTextUnit("释义：").setTextStyle(Typeface.BOLD))
                    .append(etExplanation.text.toString())
                    .build()
            tvExplanation.text = simplifySpan
        }
        layoutContent.visibility = View.VISIBLE
        btnSave.visibility = View.VISIBLE
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

    /**
     * 隐藏软键盘
     */
    private fun hideSoftKeyboard() {
        // 隐藏软键盘，避免软键盘引发的内存泄露
        val view = currentFocus
        if (view != null) {
            val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            if (manager != null && manager.isActive(view)) {
                manager.hideSoftInputFromWindow(
                    view.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }
        }
    }

}

class HorizontalAdapter :
    BaseQuickAdapter<SingleText, BaseViewHolder>(R.layout.item_single_text, null) {
    override fun convert(holder: BaseViewHolder, item: SingleText) {
        holder.itemView.tvText.text = item.text ?: ""
        holder.itemView.tvPinyin.text = item.pinyin ?: ""
    }
}

class SingleText(
    var text: String?,
    var pinyin: String?
)