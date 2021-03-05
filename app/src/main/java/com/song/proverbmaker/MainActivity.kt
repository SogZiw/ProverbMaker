package com.song.proverbmaker

import android.content.Intent
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.text.TextUtils
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import cn.iwgang.simplifyspan.SimplifySpanBuild
import cn.iwgang.simplifyspan.unit.SpecialTextUnit
import com.blankj.utilcode.util.LogUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.hjq.permissions.Permission
import com.hjq.toast.ToastUtils
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.song.proverbmaker.aop.Permissions
import com.song.proverbmaker.aop.SingleClick
import com.song.proverbmaker.extension.dp2px
import com.song.proverbmaker.helper.PinyinFormatter
import com.yalantis.ucrop.UCrop
import kotlinx.android.parcel.Parcelize
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

class MainActivity : AppCompatActivity(), View.OnClickListener, ColorPickerDialogListener {

    companion object {
        private const val REQUEST_CODE_ALBUM = 1001 //相册
    }

    private val mAdapter: HorizontalAdapter by lazy {
        HorizontalAdapter()
    }
    private lateinit var msc: MediaScannerConnection
    private var isUseCustomPinyin = false
    private var bitmapUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        btnMake.setOnClickListener(this)
        btnSave.setOnClickListener(this)
        selectBgColor.setOnClickListener(this)
        selectFontColor.setOnClickListener(this)
        selectExColor.setOnClickListener(this)
        btnSelectBg.setOnClickListener(this)
    }

    private fun init() {
        selectBgColor.color = Color.parseColor("#FFFADF4B")
        selectFontColor.color = Color.parseColor("#FF000000")
        layoutContent.visibility = View.GONE
        btnSave.visibility = View.GONE
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        proverbList.layoutManager = layoutManager
        proverbList.adapter = mAdapter
    }

    @SingleClick
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnMake -> {
                hideSoftKeyboard()
                make()
            }
            R.id.btnSave -> {
                save()
            }
            R.id.selectBgColor -> {
                ColorPickerDialog.newBuilder()
                    .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                    .setAllowPresets(false)
                    .setDialogId(0x10001)
                    .setColor(Color.BLACK)
                    .setShowAlphaSlider(true)
                    .show(this)
            }
            R.id.selectFontColor -> {
                ColorPickerDialog.newBuilder()
                    .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                    .setAllowPresets(false)
                    .setDialogId(0x10002)
                    .setColor(Color.BLACK)
                    .setShowAlphaSlider(true)
                    .show(this)
            }
            R.id.selectExColor -> {
                ColorPickerDialog.newBuilder()
                    .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                    .setAllowPresets(false)
                    .setDialogId(0x10003)
                    .setColor(Color.BLACK)
                    .setShowAlphaSlider(true)
                    .show(this)
            }
            R.id.btnSelectBg -> {
                openAlbum()
            }
        }
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        if (dialogId == 0x10001) {
            selectBgColor.color = color
        } else if (dialogId == 0x10002) {
            selectFontColor.color = color
            mAdapter.setTextColor(color)
        } else if (dialogId == 0x10003) {
            selectExColor.color = color
            mAdapter.setPinyinColor(color)
            tvExplanation.setTextColor(color)
        }
    }

    override fun onDialogDismissed(dialogId: Int) {
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
        isUseCustomPinyin = !TextUtils.isEmpty(etCustomPinyin.text.toString().trim())
        val proverbCollection = etProverb.text.toString().toCharArray()
        val proverbList = etProverb.text.toString().toCharArray().map {
            it.toString()
        }.toMutableList()

        val defaultFormat = HanyuPinyinOutputFormat()
        defaultFormat.caseType = HanyuPinyinCaseType.LOWERCASE
        defaultFormat.toneType = HanyuPinyinToneType.WITH_TONE_MARK
        defaultFormat.vCharType = HanyuPinyinVCharType.WITH_U_UNICODE

        val pinyinList: MutableList<String> = ArrayList()
        if (isUseCustomPinyin) {
            val strList = etCustomPinyin.text.toString().split(",").toMutableList()
            for (s in strList) {
                var pinyinStr = s.replace("u:".toRegex(), "v")
                pinyinStr = PinyinFormatter.convertToneNumber2ToneMark(pinyinStr)
                pinyinList.add(pinyinStr)
            }
        }

        val data: ArrayList<SingleText> = ArrayList()
        for (i in proverbList.indices) {
            var pinyin = ""
            if (isUseCustomPinyin) {
                if (i < pinyinList.size) {
                    pinyin = pinyinList[i]
                }
            } else {
                try {
                    val pinyinArray =
                        PinyinHelper.toHanyuPinyinStringArray(proverbCollection[i], defaultFormat)
                    if (!pinyinArray.isNullOrEmpty()) {
                        pinyin = pinyinArray[0].toString()
                    }
                } catch (e: Exception) {
                    pinyin = ""
                }
            }
            data.add(SingleText(proverbList[i], pinyin))
        }

        if (!TextUtils.isEmpty(etProverbFontSize.text.toString())) {
            val proverbFontSize = etProverbFontSize.text.toString().toFloat()
            val otherFontSize = proverbFontSize / 5f * 2f
            mAdapter.setProverbTextSize(proverbFontSize)
            mAdapter.setPinyinTextSize(otherFontSize)
            tvExplanation.textSize = proverbFontSize / 20f * 9f
        }

        var margin = 30
        if (!TextUtils.isEmpty(etMargin.text.toString().trim())) {
            margin = etMargin.text.toString().toInt()
            if (margin < 30) {
                margin = 30
            }
        }

        if (rbDefault.isChecked) {
            layoutMargin.setPadding(0, dp2px(margin.toFloat()), 0, 0)
            mAdapter.setList(data)
            if (!TextUtils.isEmpty(etExplanation.text.toString().trim())) {
                val simplifySpan =
                    SimplifySpanBuild().append(SpecialTextUnit("释义：").setTextStyle(Typeface.BOLD))
                        .append(etExplanation.text.toString())
                        .build()
                tvExplanation.text = simplifySpan
            }
            if (rbUsePurely.isChecked) {
                layoutContent.setBackgroundColor(selectBgColor.color)
                ivBg.visibility = View.GONE
            } else {
                layoutContent.setBackgroundColor(Color.TRANSPARENT)
                if (bitmapUri != null) {
                    val bitmap =
                        BitmapFactory.decodeStream(contentResolver.openInputStream(bitmapUri!!))
                    ivBg.visibility = View.VISIBLE
                    ivBg.setImageBitmap(bitmap)
                }
            }
            layoutContent.visibility = View.VISIBLE
            btnSave.visibility = View.VISIBLE
        } else {
            val textExplanation = etExplanation.text.toString()
            val intent = Intent(this, FullScreenActivity::class.java)
            intent.putParcelableArrayListExtra("dataList", data)
            intent.putExtra("explanation", textExplanation)
            intent.putExtra("margin", margin)
            intent.putExtra("rbUseCustomBg", rbUseCustomBg.isChecked)
            intent.putExtra("selectBgColor", selectBgColor.color)
            intent.putExtra("selectFontColor", selectFontColor.color)
            intent.putExtra("selectExColor", selectExColor.color)

            if (bitmapUri != null) {
                intent.putExtra("customBg", bitmapUri)
            }
            if (!TextUtils.isEmpty(etProverbFontSize.text.toString())) {
                val proverbFontSize = etProverbFontSize.text.toString().toFloat()
                intent.putExtra("fontSize", proverbFontSize)
            }
            startActivity(intent)
        }

    }

    @Permissions(Permission.MANAGE_EXTERNAL_STORAGE)
    private fun openAlbum() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = "android.intent.action.GET_CONTENT"
        intent.addCategory("android.intent.category.OPENABLE")
        startActivityForResult(intent, REQUEST_CODE_ALBUM)
    }

    private fun doCrop(sourceUri: Uri) {
        UCrop.of(sourceUri, getDestinationUri())//当前资源，保存目标位置
//            .withAspectRatio(1f, 1f)//宽高比
//            .withMaxResultSize(500, 500)//宽高
            .start(this)
    }

    private fun getDestinationUri(): Uri {
        val fileName = String.format("fr_crop_%s.jpg", System.currentTimeMillis())
        val cropFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)
        return Uri.fromFile(cropFile)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_ALBUM -> {
                    if (rbCrop.isChecked) {
                        doCrop(data?.data!!)
                    } else {
                        bitmapUri = data?.data!!
                    }
                }
                UCrop.REQUEST_CROP -> {
                    val resultUri: Uri = UCrop.getOutput(data!!)!!
                    bitmapUri = resultUri
                }
                UCrop.RESULT_ERROR -> {
                    val error: Throwable = UCrop.getError(data!!)!!
                    ToastUtils.show("图片剪裁失败" + error.message)
                }
            }
        }
    }

    /** 获取 View 的截图*/
    private fun getCacheBitmapFromView(view: View): Bitmap? {
        var bitmap: Bitmap? = null
        view.isDrawingCacheEnabled = true
        view.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
        val drawingCache = view.drawingCache
        if (drawingCache != null) {
            bitmap = Bitmap.createBitmap(drawingCache)
        }
        view.destroyDrawingCache()
        view.isDrawingCacheEnabled = false
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

    private var textColor = Color.BLACK
    private var pinyinColor = Color.BLACK
    private var proverbTextSize = 40f
    private var pinyinTextSize = 16f

    fun setTextColor(color: Int) {
        textColor = color
    }

    fun setPinyinColor(color: Int) {
        pinyinColor = color
    }

    fun setProverbTextSize(size: Float) {
        proverbTextSize = size
    }

    fun setPinyinTextSize(size: Float) {
        pinyinTextSize = size
    }

    override fun convert(holder: BaseViewHolder, item: SingleText) {
        holder.itemView.tvText.setTextColor(textColor)
        holder.itemView.tvPinyin.setTextColor(pinyinColor)
        holder.itemView.tvText.text = item.text ?: ""
        holder.itemView.tvPinyin.text = item.pinyin ?: ""

        holder.itemView.tvText.textSize = proverbTextSize
        holder.itemView.tvPinyin.textSize = pinyinTextSize

        val layoutParams = holder.itemView.tvText.layoutParams
        layoutParams.height = dp2px(proverbTextSize * 3f / 2f)
        layoutParams.width = dp2px(proverbTextSize * 3f / 2f)
        holder.itemView.tvText.layoutParams = layoutParams
    }
}

@Parcelize
class SingleText(
    var text: String?,
    var pinyin: String?
) : Parcelable