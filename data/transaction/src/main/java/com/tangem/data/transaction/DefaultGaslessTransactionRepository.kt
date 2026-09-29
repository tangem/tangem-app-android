package com.tangem.data.transaction

import com.tangem.blockchain.common.Token
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.transaction.convertes.GaslessBatchTransactionRequestBuilder
import com.tangem.data.transaction.convertes.GaslessSignedTransactionResultConverter
import com.tangem.data.transaction.convertes.GaslessTransactionRequestBuilder
import com.tangem.data.transaction.convertes.GaslessTxDataToGaslessRequestConverter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.GaslessServiceException
import com.tangem.domain.transaction.models.Eip7702Authorization
import com.tangem.domain.transaction.models.GaslessBatchTransactionData
import com.tangem.domain.transaction.models.GaslessSignedTransactionResult
import com.tangem.domain.transaction.models.GaslessTransactionData
import com.tangem.grow.datasource.gasless.GaslessTxServiceApi
import com.tangem.grow.datasource.gasless.GaslessTxServiceApiV2
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigInteger

class DefaultGaslessTransactionRepository(
    private val gaslessTxServiceApi: GaslessTxServiceApi,
    private val gaslessTxServiceApiV2: GaslessTxServiceApiV2,
    private val coroutineDispatcherProvider: CoroutineDispatcherProvider,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
) : GaslessTransactionRepository {

    private val supportedTokensState = MutableStateFlow<Map<Network.ID, Set<CryptoCurrency>>>(hashMapOf())
    private val receiverAddressMutex = Mutex()
    private var feeReceiverAddress: String? = null

    private val requestConverter = GaslessTxDataToGaslessRequestConverter(shouldIncludeGasLimit = true)
    private val gaslessTransactionRequestBuilder = GaslessTransactionRequestBuilder(requestConverter)
    private val gaslessBatchTransactionRequestBuilder = GaslessBatchTransactionRequestBuilder(requestConverter)
    private val signedTransactionResultConverter = GaslessSignedTransactionResultConverter()

    override suspend fun getSupportedTokens(network: Network): Set<CryptoCurrency> {
        return withContext(coroutineDispatcherProvider.io) {
            val storedTokens = supportedTokensState.value[network.id]
            if (storedTokens != null && storedTokens.isNotEmpty()) {
                return@withContext storedTokens
            }

            val supportedTokensData = gaslessTxServiceApi.getSupportedTokens().getOrThrowGaslessError()
            if (supportedTokensData.isSuccess) {
                val networkBlockchain = network.toBlockchain()
                val supportedTokens = supportedTokensData.result.tokens
                    .filter {
                        it.chainId == networkBlockchain.getChainId()
                    }
                    .map { token ->
                        responseCryptoCurrenciesFactory.createToken(
                            blockchain = networkBlockchain,
                            sdkToken = Token(
                                contractAddress = token.tokenAddress,
                                name = token.tokenName,
                                symbol = token.tokenSymbol,
                                decimals = token.decimals,
                            ),
                            network = network,
                        )
                    }.toSet()
                // update local cache
                supportedTokensState.update { current ->
                    val newMap = current.toMutableMap()
                    newMap[network.id] = supportedTokens
                    newMap
                }
                return@withContext supportedTokens
            } else {
                throw unsuccessfulResponseError()
            }
        }
    }

    override suspend fun getTokenFeeReceiverAddress(): String {
        return withContext(coroutineDispatcherProvider.io) {
            receiverAddressMutex.withLock {
                val feeReceiver = feeReceiverAddress
                if (feeReceiver != null) {
                    return@withContext feeReceiver
                }
                val response = gaslessTxServiceApi.getFeeRecipient().getOrThrowGaslessError()
                if (response.isSuccess) {
                    feeReceiverAddress = response.result.address
                    response.result.address
                } else {
                    throw unsuccessfulResponseError()
                }
            }
        }
    }

    override suspend fun signGaslessTransaction(
        gaslessTransactionData: GaslessTransactionData,
        signature: String,
        userAddress: String,
        network: Network,
        eip7702Auth: Eip7702Authorization?,
    ): GaslessSignedTransactionResult = withContext(coroutineDispatcherProvider.io) {
        val blockchain = network.toBlockchain()
        val transactionRequest = gaslessTransactionRequestBuilder.build(
            gaslessTransaction = gaslessTransactionData,
            signature = signature,
            userAddress = userAddress,
            chainId = blockchain.getChainId() ?: error("ChainId is null for blockchain: $blockchain"),
            eip7702Auth = eip7702Auth,
        )

        val response = gaslessTxServiceApiV2.signGaslessTransaction(transactionRequest).getOrThrowGaslessError()

        if (!response.isSuccess) {
            throw unsuccessfulResponseError()
        }

        // Convert DTO to domain model
        signedTransactionResultConverter.convert(response.result)
    }

    override suspend fun signGaslessBatchTransaction(
        gaslessBatchTransactionData: GaslessBatchTransactionData,
        signature: String,
        userAddress: String,
        network: Network,
        eip7702Auth: Eip7702Authorization?,
    ): GaslessSignedTransactionResult = withContext(coroutineDispatcherProvider.io) {
        val blockchain = network.toBlockchain()
        val transactionRequest = gaslessBatchTransactionRequestBuilder.build(
            gaslessBatchTransaction = gaslessBatchTransactionData,
            signature = signature,
            userAddress = userAddress,
            chainId = blockchain.getChainId() ?: error("ChainId is null for blockchain: $blockchain"),
            eip7702Auth = eip7702Auth,
        )

        val response = gaslessTxServiceApiV2.signGaslessBatchTransaction(transactionRequest).getOrThrowGaslessError()

        if (!response.isSuccess) {
            throw unsuccessfulResponseError()
        }

        signedTransactionResultConverter.convert(response.result)
    }

    private fun unsuccessfulResponseError() = GaslessServiceException(
        httpCode = null,
        message = "Gasless service returned unsuccessful response",
    )

    override fun getBaseGasForTransaction(): BigInteger {
        return BASE_GAS_FOR_TRANSACTION
    }

    override fun getChainIdForNetwork(network: Network): Int {
        val networkBlockchain = network.toBlockchain()
        return networkBlockchain.getChainId() ?: error("ChainId not found for blockchain ${networkBlockchain.name}")
    }

    override suspend fun getGaslessFeeAddresses(): Set<String> {
        // TODO Replace with other backend call to get all fee recipient addresses when available
        val backendAddress = runSuspendCatching { getTokenFeeReceiverAddress() }
            .onFailure { TangemLogger.e("Failed to load gasless fee recipient; serving hardcoded addresses", it) }
            .getOrNull()
        return KNOWN_FEE_COLLECTION_ADDRESSES + TRON_GASLESS_FEE_RECIPIENT + setOfNotNull(backendAddress)
    }

    private companion object {
        val BASE_GAS_FOR_TRANSACTION: BigInteger = BigInteger("60000")

        
        val KNOWN_FEE_COLLECTION_ADDRESSES = setOf(
            "0xFc719364BcCdc92D055d8C3164eF1ab4f5A9182c",
            "0xAf722F46145fbb106379d506ED3a5B96f110c8E5",
        )

        val TRON_GASLESS_FEE_RECIPIENT = setOf(
            "TSWsmaEDgWaE2VsP9tCBGh5GbBRbrdHSuZ",
        )
    }
}