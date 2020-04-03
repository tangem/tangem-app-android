package com.tangem.data.dp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.orhanobut.hawk.Hawk
import com.tangem.Constant
import com.tangem.tangem_card.reader.CardCrypto

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

    fun getSettingsBoolean(key: Int, default: Boolean): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(key), default)
    }

    fun appendCid(newCid: String) {
        val prevCids: String = Hawk.get("CID_Key", "")
        if (!prevCids.contains(newCid)) Hawk.put("CID_Key", "$prevCids$newCid, ")
    }

    fun getAllCids(): String = Hawk.get("CID_Key", "")

    val terminalKeys: Map<String, ByteArray>
        get() {
            val privateKey = Hawk.get<ByteArray>(Constant.TERMINAL_PRIVATE_KEY) ?: byteArrayOf()
            val publicKey = Hawk.get<ByteArray>(Constant.TERMINAL_PUBLIC_KEY) ?: byteArrayOf()
            return if (privateKey.isNotEmpty() && publicKey.isNotEmpty()) {
                mapOf(Constant.TERMINAL_PRIVATE_KEY to privateKey, Constant.TERMINAL_PUBLIC_KEY to publicKey)
            } else {
                val keys = CardCrypto.generateTerminalKeys()
                Hawk.put(Constant.TERMINAL_PRIVATE_KEY, keys[Constant.TERMINAL_PRIVATE_KEY])
                Hawk.put(Constant.TERMINAL_PUBLIC_KEY, keys[Constant.TERMINAL_PUBLIC_KEY])
                keys
            }
        }
}