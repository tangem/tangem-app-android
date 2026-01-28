package com.tangem.data.transaction

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.transaction.convertes.GaslessSignedTransactionResultConverter
import com.tangem.data.transaction.convertes.GaslessTransactionRequestBuilder
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.gasless.GaslessTxServiceApi
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.models.Eip7702Authorization
import com.tangem.domain.transaction.models.GaslessSignedTransactionResult
import com.tangem.domain.transaction.models.GaslessTransactionData
import com.tangem.features.send.v2.api.SendFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigInteger

class DefaultGaslessTransactionRepository(
    private val gaslessTxServiceApi: GaslessTxServiceApi,
    private val coroutineDispatcherProvider: CoroutineDispatcherProvider,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    private val sendFeatureToggles: SendFeatureToggles,
) : GaslessTransactionRepository {

    private val supportedTokensState = MutableStateFlow<Map<Network.ID, Set<CryptoCurrency>>>(hashMapOf())
    private val allFeeRecipientAddress = mutableSetOf<String>()
    private val allAddressesMutex = Mutex()
    private val receiverAddressMutex = Mutex()
    private var feeReceiverAddress: String? = null

    private val gaslessTransactionRequestBuilder = GaslessTransactionRequestBuilder()
    private val signedTransactionResultConverter = GaslessSignedTransactionResultConverter()

    override suspend fun getSupportedTokens(network: Network): Set<CryptoCurrency> {
        return withContext(coroutineDispatcherProvider.io) {
            val storedTokens = supportedTokensState.value[network.id]
            if (storedTokens != null && storedTokens.isNotEmpty()) {
                return@withContext storedTokens
            }

            val supportedTokensData = gaslessTxServiceApi.getSupportedTokens().getOrThrow()
            if (supportedTokensData.isSuccess) {
                val networkBlockchain = Blockchain.fromId(network.rawId)
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
                error("Gasless service returned unsuccessful response")
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
                val response = gaslessTxServiceApi.getFeeRecipient().getOrThrow()
                if (response.isSuccess) {
                    feeReceiverAddress = response.result.address
                    response.result.address
                } else {
                    error("Gasless service returned unsuccessful response")
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
        val blockchain = Blockchain.fromId(network.rawId)
        val transactionRequest = gaslessTransactionRequestBuilder.build(
            gaslessTransaction = gaslessTransactionData,
            signature = signature,
            userAddress = userAddress,
            chainId = blockchain.getChainId() ?: error("ChainId is null for blockchain: $blockchain"),
            eip7702Auth = eip7702Auth,
        )

        val response = gaslessTxServiceApi.signGaslessTransaction(transactionRequest).getOrThrow()

        if (!response.isSuccess) {
            error("Gasless service returned unsuccessful response")
        }

        // Convert DTO to domain model
        signedTransactionResultConverter.convert(response.result)
    }

    override fun getBaseGasForTransaction(): BigInteger {
        return BASE_GAS_FOR_TRANSACTION
    }

    override fun getChainIdForNetwork(network: Network): Int {
        val networkBlockchain = Blockchain.fromId(network.rawId)
        return networkBlockchain.getChainId() ?: error("ChainId not found for blockchain ${networkBlockchain.name}")
    }

    override suspend fun getGaslessFeeAddresses(): Set<String> {
        if (!sendFeatureToggles.isGaslessTransactionsEnabled) {
            return EMPTY_ADDRESSES
        }
        return allAddressesMutex.withLock {
            allFeeRecipientAddress.ifEmpty {
                val allFeeAddresses = getAllFeeRecipientAddresses()
                allFeeRecipientAddress.addAll(allFeeAddresses)
                allFeeRecipientAddress
            }
        }
    }

    private suspend fun getAllFeeRecipientAddresses(): Set<String> {
        // TODO Replace with other backend call to get all fee recipient addresses when available
        return setOf(getTokenFeeReceiverAddress())
    }

    private companion object {
        val BASE_GAS_FOR_TRANSACTION: BigInteger = BigInteger("100000")
        val EMPTY_ADDRESSES = emptySet<String>()
    }
}