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
        // TODO - move loadRateInfo to CoinEngine
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
            Blockchain.Binance -> "binance-coin"
            Blockchain.BinanceTestNet -> "binance-coin"
            else -> {
                throw Exception("Can''t get rate for blockchain " + ctx.blockchainName)
            }
        }
        serverApiCommon.requestRateInfo(cryptoId)
    }

    private fun loadRateInfo() {
        serverApiCommon.setRateInfoListener {
            val rate = it.priceUsd.toFloat()
            this.rate.value = rate
        }
    }
}