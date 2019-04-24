package com.tangem.ui.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangem.data.Blockchain
import com.tangem.data.network.ServerApiCommon
import com.tangem.wallet.TangemContext

class LoadedWalletViewModel : ViewModel() {
    private var serverApiCommon: ServerApiCommon = ServerApiCommon()

    private lateinit var ctx: TangemContext

//    private var rate = MutableLiveData<Float>()
//
//    fun getRateInfo(ctx: TangemContext): LiveData<Float> {
//        this.ctx = ctx
//        rate = MutableLiveData()
//        requestRateInfo()
//        return rate
//    }

    private val rate: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>().also {
            requestRateInfo()
        }
    }

    fun getRateInfo(ctx: TangemContext): LiveData<Float> {
        this.ctx = ctx
        return rate
    }

    private fun requestRateInfo() {
        serverApiCommon.setRateInfoListener {
            val rate = it.priceUsd.toFloat()
            this.rate.value = rate
        }
        // TODO - move requestRateInfo to CoinEngine
        val cryptoId: String = when (ctx.blockchain) {
            Blockchain.Bitcoin -> "bitcoin"
            Blockchain.BitcoinTestNet -> "bitcoin"
            Blockchain.Ethereum -> "ethereum"
            Blockchain.EthereumTestNet -> "ethereum"
            Blockchain.Token -> "ethereum"
            Blockchain.NftToken -> "ethereum"
            Blockchain.BitcoinCash -> "bitcoin-cash"
            Blockchain.Litecoin -> "litecoin"
            Blockchain.Rootstock -> "bitcoin"
            Blockchain.RootstockToken -> "bitcoin"
            Blockchain.Cardano -> "cardano"
            Blockchain.Ripple -> "ripple"
            else -> {
                throw Exception("Can''t get rate for blockchain " + ctx.blockchainName)
            }
        }
        serverApiCommon.requestRateInfo(cryptoId)
    }

    override fun onCleared() {
        super.onCleared()
//        getRateInfo(ctx).removeObservers()
    }
}