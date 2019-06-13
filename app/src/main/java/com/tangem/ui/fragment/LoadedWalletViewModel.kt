package com.tangem.ui.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangem.data.Blockchain
import com.tangem.data.network.ServerApiCommon
import com.tangem.data.network.ServerApiCommon.RateInfoListener
import com.tangem.data.network.model.RateInfoResponse
import com.tangem.wallet.TangemContext

class LoadedWalletViewModel : ViewModel() {
    private var serverApiCommon: ServerApiCommon = ServerApiCommon()
    private lateinit var ctx: TangemContext
    private val rate: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>().also {
            loadRateInfo()
        }
    }

    fun getRateInfo(): LiveData<Float> {
        return rate
    }

    fun requestRateInfo(ctx: TangemContext) {
        this.ctx = ctx
// [REDACTED_TODO_COMMENT]
        serverApiCommon.requestRateInfo(ctx.blockchain.currency)
    }

    private fun loadRateInfo() {
        serverApiCommon.setRateInfoListener(object: RateInfoListener{
            override fun onSuccess(rateInfoResponse: RateInfoResponse?) {
                rate.value = rateInfoResponse!!.data!!.quote!!.usd!!.price
            }

            override fun onFail(message: String?) {
                ctx.error = message
            }
        })
    }
}
