package com.tangem

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tangem.di.DaggerNavigatorComponent
import com.tangem.di.DaggerNetworkComponent
import com.tangem.di.NavigatorComponent
import com.tangem.di.NetworkComponent
import com.tangem.card_android.android.data.Firmwares
import com.tangem.card_android.android.data.PINStorage
import com.tangem.card_common.data.Issuer
import com.tangem.server_android.data.LocalStorage
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

        var networkComponent: NetworkComponent? = null
            private set
        var navigatorComponent: NavigatorComponent? = null
            private set

        lateinit var firmwaresStorage: Firmwares
        lateinit var localStorage: LocalStorage
        lateinit var pinStorage: PINStorage
    }

    override fun onCreate() {
        super.onCreate()
        // initialize the singleton
        instance = this

        networkComponent = DaggerNetworkComponent.create()
        navigatorComponent = buildNavigatorComponent()

        // common init
        if (PINStorage.needInit())
            PINStorage.init(applicationContext)

        initIssuers()

        firmwaresStorage = Firmwares(applicationContext)
        localStorage = LocalStorage(applicationContext)
        pinStorage = PINStorage()
    }

    private fun buildNavigatorComponent(): NavigatorComponent {
        return DaggerNavigatorComponent.builder()
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