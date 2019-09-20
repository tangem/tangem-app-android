package com.tangem.ui.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangem.Constant
import com.tangem.data.dp.PrefsManager
import com.tangem.data.network.ServerApiCommon
import com.tangem.wallet.BuildConfig

class MainViewModel : ViewModel() {
    private var serverApiCommon: ServerApiCommon = ServerApiCommon()

    private var versionName = MutableLiveData<String>()

    fun getVersionName(): LiveData<String> {
        versionName = MutableLiveData()
        requestVersionName()
        return versionName
    }

    private fun requestVersionName() {
        serverApiCommon.setLastVersionListener { response ->
            try {
                if (response.isNullOrEmpty())
                    return@setLastVersionListener
                val responseVersionName = response.trim(' ', '\n', '\r', '\t')
                val responseBuildVersion = responseVersionName.split('.').last()
                val appBuildVersion = BuildConfig.VERSION_NAME.split('.').last()
                if (responseBuildVersion.toInt() > appBuildVersion.toInt())
                    if (BuildConfig.FLAVOR.equals(Constant.FLAVOR_TANGEM_ACCESS))
                        versionName.value = responseVersionName
            } catch (E: Exception) {
                E.printStackTrace()
            }
        }
        serverApiCommon.requestLastVersion()
    }

    fun getTerminalKeys(): Map<String, ByteArray> {
        return PrefsManager.getInstance().terminalKeys
    }

}
