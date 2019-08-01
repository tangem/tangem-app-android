package com.tangem

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.crashlytics.android.Crashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tangem.card_android.android.data.Firmwares
import com.tangem.card_android.android.data.PINStorage
import com.tangem.card_common.data.Issuer
import com.tangem.data.dp.PrefsManager
import com.tangem.data.local.PendingTransactionsStorage
import com.tangem.di.*
import com.tangem.server_android.data.LocalStorage
import com.tangem.wallet.BuildConfig
import io.fabric.sdk.android.Fabric
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class App : Application() {
    companion object {
        @get:Synchronized
        var instance: App? = null
            private set

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }

        lateinit var networkComponent: NetworkComponent
        lateinit var navigatorComponent: NavigatorComponent
        lateinit var toastHelperComponent: ToastHelperComponent

        lateinit var firmwaresStorage: Firmwares
        lateinit var localStorage: LocalStorage
        lateinit var pinStorage: PINStorage
        lateinit var pendingTransactionsStorage: PendingTransactionsStorage
    }

    override fun onCreate() {
        super.onCreate()
        // initialize the singleton
        instance = this

        networkComponent = DaggerNetworkComponent.create()
        navigatorComponent = buildNavigatorComponent()
        toastHelperComponent = buildToastHelperComponent()

        PrefsManager.getInstance().init(this)

        // common init
        if (PINStorage.needInit())
            PINStorage.init(applicationContext)

        initIssuers()

        firmwaresStorage = Firmwares(applicationContext)
        localStorage = LocalStorage(applicationContext)
        pinStorage = PINStorage()
        pendingTransactionsStorage = PendingTransactionsStorage(applicationContext)

        if (BuildConfig.DEBUG) {
            Fabric.with(this, Crashlytics())
            com.tangem.card_common.util.Log.setLogger(
                    object : com.tangem.card_common.util.LoggerInterface {
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

    private fun buildNavigatorComponent(): NavigatorComponent {
        return DaggerNavigatorComponent.builder()
                .build()
    }

    private fun buildToastHelperComponent(): ToastHelperComponent {
        return DaggerToastHelperComponent.builder()
                .build()
    }

    private fun initIssuers() {
        try {
            applicationContext.assets.open("issuers.json").use { `is` ->
                InputStreamReader(`is`, StandardCharsets.UTF_8).use { reader ->
                    val listType = object : TypeToken<List<Issuer>>() {

                    }.type

                    Issuer.fillIssuers(Gson().fromJson(reader, listType))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}