package com.tangem.devkit

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.tangem.devkit._arch.structure.ILog
import com.tangem.devkit._arch.structure.ItemLogger
import com.tangem.devkit.commons.TangemLogger
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class AppTangemDemo : Application() {

    override fun onCreate() {
        super.onCreate()

        AppTangemDemo.appInstance = this
        setupLoggers()
    }

    private fun setupLoggers() {
        Log.setLogger(TangemLogger())
        ILog.setLogger(ItemLogger())
    }

    fun sharedPreferences(name: String = "DevKitApp", mode: Int = Context.MODE_PRIVATE): SharedPreferences {
        return getSharedPreferences(name, mode)
    }

    companion object {
        lateinit var appInstance: AppTangemDemo
    }
}
