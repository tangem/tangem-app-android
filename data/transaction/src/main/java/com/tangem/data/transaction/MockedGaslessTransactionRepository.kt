package com.tangem.data.transaction

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.models.Eip7702Authorization
import com.tangem.domain.transaction.models.GaslessSignedTransactionResult
import com.tangem.domain.transaction.models.GaslessTransactionData
import java.math.BigInteger

class MockedGaslessTransactionRepository(
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
) : GaslessTransactionRepository {

    override fun isNetworkSupported(network: Network): Boolean {
        val blockchain = Blockchain.fromId(network.rawId)
        return SUPPORTED_BLOCKCHAINS.contains(blockchain)
    }

    override suspend fun getSupportedTokens(network: Network): Set<CryptoCurrency> {
        val usdcPolygon = responseCryptoCurrenciesFactory.createToken(
            blockchain = Blockchain.Polygon,
            sdkToken = Token(
                contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                name = "Tether",
                symbol = "USDT",
                decimals = 6,
            ),
            network = network,
        )
        return setOf(usdcPolygon)
    }

    override fun getChainIdForNetwork(network: Network): Int {
        val networkBlockchain = Blockchain.fromId(network.rawId)
        return networkBlockchain.getChainId() ?: error("ChainId not found for blockchain ${networkBlockchain.name}")
    }

    override fun getTokenFeeReceiverAddress(): String {
        return TOKEN_RECEIVER_ADDRESS
    }

    override suspend fun signGaslessTransaction(
        gaslessTransactionData: GaslessTransactionData,
        signature: String,
        userAddress: String,
        network: Network,
        eip7702Auth: Eip7702Authorization?,
    ): GaslessSignedTransactionResult = GaslessSignedTransactionResult(
        signedTransaction = "0x000",
        gasLimit = 100000.toBigInteger(),
        maxFeePerGas = 21000.toBigInteger(),
        maxPriorityFeePerGas = 21000.toBigInteger(),
    )

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