package com.tangem.wallet.flowDemo

import android.os.Bundle
import android.util.Log
import com.tangem.wallet.CoinData
import java.lang.Exception

class FlowDemoData : CoinData() {
    var description: String? = null

    override fun clearInfo() {
        super.clearInfo()
        description = null
    }

    override fun loadFromBundle(B: Bundle) {
        super.loadFromBundle(B)
        if (B.containsKey("Description")) description = B.getString("Description")
    }

    override fun saveToBundle(B: Bundle) {
        super.saveToBundle(B)
        try {
            if (description != null) B.putString("Description", description!!)
        } catch (e: Exception) {
            Log.e("Can't save to bundle ", e.message)
        }
    }
}