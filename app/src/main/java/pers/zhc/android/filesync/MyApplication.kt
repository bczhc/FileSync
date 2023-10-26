package pers.zhc.android.filesync

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.google.gson.Gson

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        appContext = this
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var appContext: Context

        val GSON = Gson()
    }
}
