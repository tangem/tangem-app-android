package com.tangem.data.transaction

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.transaction.converters.GaslessTokenDtoToCryptoCurrencyConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.gasless.GaslessTxServiceApi
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.math.BigInteger

class DefaultGaslessTransactionRepository(
    private val gaslessTxServiceApi: GaslessTxServiceApi,
    private val coroutineDispatcherProvider: CoroutineDispatcherProvider,
) : GaslessTransactionRepository {

    private val gaslessTokenDtoToCryptoCurrency = GaslessTokenDtoToCryptoCurrencyConverter()
    private val supportedTokensState = MutableStateFlow<Set<CryptoCurrency>?>(null)

    override fun isNetworkSupported(network: Network): Boolean {
        val blockchain = Blockchain.fromNetworkId(network.backendId) ?: return false
        return SUPPORTED_BLOCKCHAINS.contains(blockchain)
    }

    override suspend fun getSupportedTokens(): Set<CryptoCurrency> {
        return withContext(coroutineDispatcherProvider.io) {
            val storedTokens = supportedTokensState.value
            if (storedTokens != null) {
                return@withContext storedTokens
            }
            val supportedTokensData = gaslessTxServiceApi.getSupportedTokens().getOrThrow()
            if (supportedTokensData.isSuccess) {
                val supportedTokens = supportedTokensData.result.tokens.map {
                    gaslessTokenDtoToCryptoCurrency.convert(it)
                }.toSet()
                // update local cache
                supportedTokensState.update { supportedTokens }
                return@withContext supportedTokens
            } else {
                error("Gasless service returned unsuccessful response")
            }
        }
    }

    override fun getTokenFeeReceiverAddress(): String {
        return TOKEN_RECEIVER_ADDRESS
    }

    override fun getBaseGasForTransaction(): BigInteger {
        return BASE_GAS_FOR_TRANSACTION
    }

    private companion object {

        const val TOKEN_RECEIVER_ADDRESS = "0x"

        val BASE_GAS_FOR_TRANSACTION: BigInteger = BigInteger("100000")
        val SUPPORTED_BLOCKCHAINS = arrayOf(
            Blockchain.Ethereum,
            Blockchain.BSC,
            Blockchain.Base,
            Blockchain.Polygon,
            Blockchain.Arbitrum,
            Blockchain.XDC,
            Blockchain.Optimism,
        )
    }
}