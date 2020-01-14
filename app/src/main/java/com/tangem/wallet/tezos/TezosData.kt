package com.tangem.wallet.tezos

import android.os.Bundle
import android.util.Log
import com.tangem.wallet.CoinData
import java.lang.Exception

class TezosData : CoinData() {
    var balance: Long? = null
    var counter: Long? = null
    var publicKeyReavealed: Boolean? = null
    var tezosPublicKey: String? = null

    override fun clearInfo() {
        super.clearInfo()
        balance = null
        counter = null
        publicKeyReavealed = null
        tezosPublicKey = null
    }

    override fun loadFromBundle(B: Bundle) {
        super.loadFromBundle(B)

        if (B.containsKey("Balance")) balance = B.getLong("Balance")

        if (B.containsKey("Counter")) counter = B.getLong("Counter")

        if (B.containsKey("PublicKeyReavealed")) publicKeyReavealed = B.getBoolean("PublicKeyReavealed")

        if (B.containsKey("TezosPublicKey")) tezosPublicKey = B.getString("TezosPublicKey")
    }

    override fun saveToBundle(B: Bundle) {
        super.saveToBundle(B)
        try {
            if (balance != null) B.putLong("Balance", balance!!)

            if (counter != null) B.putLong("Counter", counter!!)

            if (publicKeyReavealed != null) B.putBoolean("PublicKeyReavealed", publicKeyReavealed!!)

            if (tezosPublicKey != null) B.putString("TezosPublicKey", tezosPublicKey!!)

        } catch (e: Exception) {
            Log.e("Can't save to bundle ", e.message)
        }
    }
}