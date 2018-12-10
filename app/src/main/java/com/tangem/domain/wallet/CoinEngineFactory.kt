package com.tangem.domain.wallet

import android.util.Log

import com.tangem.domain.wallet.btc.BtcEngine
import com.tangem.domain.wallet.eth.EthEngine
import com.tangem.domain.wallet.token.TokenEngine
import com.tangem.domain.wallet.bch.BtcCashEngine
import com.tangem.data.Blockchain

/**
 * Factory for create specific engine
 *
 * @param Blockchain
 * @param TangemContext
 *
 */

object CoinEngineFactory {
    private val TAG = CoinEngineFactory::class.java.simpleName

    fun create(blockchain: Blockchain): CoinEngine? {
        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestNet -> BtcEngine()
            Blockchain.BitcoinCash -> BtcCashEngine()
            Blockchain.Ethereum, Blockchain.EthereumTestNet -> EthEngine()
            Blockchain.Token -> TokenEngine()
            else -> null
        }
    }

    fun create(context: TangemContext): CoinEngine? {
        var result: CoinEngine?
        try {
            result = if (Blockchain.BitcoinCash == context.blockchain)
                BtcCashEngine(context)
            else if (Blockchain.Bitcoin == context.blockchain || Blockchain.BitcoinTestNet == context.blockchain)
                BtcEngine(context)
            else if (Blockchain.Ethereum == context.blockchain || Blockchain.EthereumTestNet == context.blockchain)
                EthEngine(context)
            else if (Blockchain.Token == context.blockchain)
                TokenEngine(context)
            else
                return null
        } catch (e: Exception) {
            e.printStackTrace()
            result = null
            Log.e(TAG, "Can't create CoinEngine!")
        }
        return result
    }

}