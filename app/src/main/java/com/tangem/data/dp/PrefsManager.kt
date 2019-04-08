package com.tangem.data.dp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

import com.orhanobut.hawk.Hawk
import com.tangem.Constant

class PrefsManager {
    companion object {
        const val PREF_NAME = "tangem_access"
        @SuppressLint("StaticFieldLeak")
        private var instance: PrefsManager? = null

        fun getInstance(): PrefsManager {
            if (instance == null) {
                instance = PrefsManager()
            }
            return instance as PrefsManager
        }
    }

    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        this.context = context
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        Hawk.init(context).build()
    }

    val lastWalletAddress: String
        get() = Hawk.get(Constant.PREF_LAST_WALLET_ADDRESS, "")

    fun saveLastWalletAddress(value: String) {
        Hawk.put(Constant.PREF_LAST_WALLET_ADDRESS, value)
    }

    fun clearLastWalletAddress() {
        Hawk.delete(Constant.PREF_LAST_WALLET_ADDRESS)
    }

}