package com.song.proverbmaker

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.blankj.utilcode.util.Utils
import com.hjq.toast.ToastInterceptor
import com.hjq.toast.ToastUtils
import com.song.proverbmaker.helper.ActivityStackManager

/**
 * Title: com.song.proverbmaker
 * Description:
 * Copyright:Copyright(c) 2021
 * CreateTime:2021/03/02 11:13
 *
 * @author SogZiw
 * @version 1.0
 */
class App : Application() {

    companion object {
        lateinit var mApplication: Application

        fun getApplication(): Context {
            return mApplication
        }
    }

    override fun onCreate() {
        super.onCreate()
        mApplication = this
        Utils.init(this)
        // 吐司工具类
        ToastUtils.init(this)
        // 设置 Toast 拦截器
        ToastUtils.setToastInterceptor(object : ToastInterceptor() {
            override fun intercept(toast: Toast, text: CharSequence): Boolean {
                val intercept = super.intercept(toast, text)
                if (intercept) Log.e("Toast", "空 Toast") else Log.i("Toast", text.toString())
                return intercept
            }
        })
        // Activity 栈管理初始化
        ActivityStackManager.getInstance().init(this)
    }
}