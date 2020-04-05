package com.tangem

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tangem.data.dp.PrefsManager
import com.tangem.data.local.PendingTransactionsStorage
import com.tangem.di.DaggerNetworkComponent
import com.tangem.di.DaggerToastHelperComponent
import com.tangem.di.NetworkComponent
import com.tangem.di.ToastHelperComponent
import com.tangem.server_android.data.LocalStorage
import com.tangem.tangem_card.data.Issuer
import com.tangem.tangem_sdk.android.data.Firmwares
import com.tangem.tangem_sdk.android.data.PINStorage
import com.tangem.util.Analytics
import com.tangem.wallet.BuildConfig
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
            Runtime.getRuntime().exec("logcat -G 16M")
            com.tangem.tangem_card.util.Log.setLogger(
                    object : com.tangem.tangem_card.util.LoggerInterface {
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

        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(Analytics.isEnabled())
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(Analytics.isEnabled())
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