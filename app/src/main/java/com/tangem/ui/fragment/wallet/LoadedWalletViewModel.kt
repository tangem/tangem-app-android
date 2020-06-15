package com.tangem.ui.fragment.wallet

import androidx.lifecycle.*
import com.tangem.data.network.ServerApiCommon
import com.tangem.data.network.ServerApiCommon.RateInfoListener
import com.tangem.data.network.model.RateInfoResponse
import com.tangem.server_android.PayIdResponse
import com.tangem.server_android.PayIdService
import com.tangem.server_android.Result
import com.tangem.server_android.SetPayIdResponse
import com.tangem.wallet.TangemContext

class LoadedWalletViewModel : ViewModel() {
    private val serverApiCommon: ServerApiCommon = ServerApiCommon()
    private val payIdService = PayIdService()
    private lateinit var ctx: TangemContext
    private val rate: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>().also {
            loadRateInfo()
        }
    }

    private val payId: LiveData<Result<PayIdResponse>> = MutableLiveData()

    fun getRateInfo(): LiveData<Float> {
        return rate
    }

    fun requestRateInfo(ctx: TangemContext) {
        this.ctx = ctx
// [REDACTED_TODO_COMMENT]
        serverApiCommon.requestRateInfo(ctx.blockchain.currency)
    }

    private fun loadRateInfo() {
        serverApiCommon.setRateInfoListener(object : RateInfoListener {
            override fun onSuccess(rateInfoResponse: RateInfoResponse?) {
                rate.value = rateInfoResponse!!.data!!.quote!!.usd!!.price
            }

            override fun onFail(message: String?) {
                ctx.error = message
            }
        })
    }

    fun getPayId(cardId: String, publicKey: String): LiveData<Result<PayIdResponse>> {
        return liveData(viewModelScope.coroutineContext) {
            emit(
                payIdService.getPayId(
                    cardId,
                    publicKey
                )
            )
        }
    }

    fun setPayId(
        cardId: String,
        publicKey: String,
        payId: String,
        address: String,
        network: String
    ): LiveData<Result<SetPayIdResponse>> {
        return liveData(viewModelScope.coroutineContext) {
            emit(
                payIdService.setPayId(
                    cardId, publicKey, payId, address, network
                )
            )
        }
    }
}
