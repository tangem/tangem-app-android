package com.tangem.tangemtest

import android.app.Application
import com.tangem.Log
import com.tangem.LoggerInterface

class TestApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.setLogger(
                object : LoggerInterface {
                    override fun i(logTag: String, message: String) {
                        android.util.Log.i(logTag, message)
                    }

                    override fun e(logTag: String, message: String) {
                        android.util.Log.e(logTag, message)
                    }

                    override fun v(logTag: String, message: String) {
                        android.util.Log.v(logTag, message)
                    }
                }
        )
    }
}