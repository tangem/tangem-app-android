package com.tangem.data.staking

import android.util.Base64
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.data.staking.converters.YieldConverter
import com.tangem.data.staking.converters.action.ActionStatusConverter
import com.tangem.data.staking.converters.action.EnterActionResponseConverter
import com.tangem.data.staking.converters.transaction.GasEstimateConverter
import com.tangem.data.staking.converters.transaction.StakingTransactionConverter
import com.tangem.data.staking.converters.transaction.StakingTransactionStatusConverter
import com.tangem.data.staking.converters.transaction.StakingTransactionTypeConverter
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.request.*
import com.tangem.datasource.api.stakekit.models.response.EnabledYieldsResponse
import com.tangem.datasource.api.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.action.StakingActionStatusDTO
import com.tangem.datasource.api.stakekit.models.response.model.transaction.tron.TronStakeKitTransaction
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.datasource.local.token.converter.StakingNetworkTypeConverter
import com.tangem.datasource.local.token.converter.YieldTokenConverter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
internal class DefaultStakeKitRepository(
    private val stakeKitApi: StakeKitApi,
    private val stakingYieldsStore: StakingYieldsStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val walletManagersFacade: WalletManagersFacade,
    moshi: Moshi,
) : StakeKitRepository {

    private val transactionStatusConverter = StakingTransactionStatusConverter()
    private val transactionTypeConverter = StakingTransactionTypeConverter()
    private val actionStatusConverter = ActionStatusConverter()

    private val transactionConverter = StakingTransactionConverter(
        transactionStatusConverter = transactionStatusConverter,
        transactionTypeConverter = transactionTypeConverter,
    )
    private val enterActionResponseConverter = EnterActionResponseConverter(
        actionStatusConverter = actionStatusConverter,
        transactionConverter = transactionConverter,
    )

    private val tronStakeKitTransactionAdapter by lazy { moshi.adapter(TronStakeKitTransaction::class.java) }
    private val networkTypeAdapter by lazy { moshi.adapter(NetworkTypeDTO::class.java) }
    private val stakingActionStatusAdapter by lazy { moshi.adapter(StakingActionStatusDTO::class.java) }

    override suspend fun fetchYields() {
        withContext(dispatchers.io) {
            val yieldsResponses = getAvailableStakeKitIntegrationsIds().map {
                async { it.getYieldRequest() }
            }.awaitAll()

            val yields = yieldsResponses.flatMap { response ->
                when (response) {
                    is ApiResponse.Success -> response.data.data.filter { yield -> yield.isAvailable == true }
                    is ApiResponse.Error -> {
                        Timber.e("Error fetching enabled yields: ${response.cause}")
                        emptyList()
                    }
                }
            }
            val shouldRewriteCache = yieldsResponses.all { it is ApiResponse.Success }
            stakingYieldsStore.store(yields, shouldRewriteCache)
        }
    }

    override suspend fun getYield(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Yield {
        return withContext(dispatchers.io) {
            val rawCurrencyId = cryptoCurrencyId.rawCurrencyId ?: error("Staking custom tokens is not available")

            val prefetchedYield = findPrefetchedYield(
                yields = getEnabledYieldsSync(),
                currencyId = rawCurrencyId,
                symbol = symbol,
            )

            prefetchedYield ?: error("Staking is unavailable")
        }
    }

    override suspend fun getYield(yieldId: String): Yield {
        return withContext(dispatchers.io) {
            getEnabledYieldsSync().find { it.id == yieldId } ?: error("Staking is unavailable")
        }
    }

    override suspend fun getActions(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        networkType: NetworkType,
        stakingActionStatus: StakingActionStatus,
    ): List<StakingAction> {
        return withContext(dispatchers.io) {
            val address = walletManagersFacade.getDefaultAddress(userWalletId, cryptoCurrency.network).orEmpty()

            val networkTypeDto = StakingNetworkTypeConverter.convertBack(networkType)
            val networkTypeString = networkTypeDto.extractJsonName()

            val actionStatusDTO = actionStatusConverter.convertBack(stakingActionStatus)
            val actionStatusString = actionStatusDTO.extractJsonName()

            enterActionResponseConverter.convertListIgnoreErrors(
                input = stakeKitApi.getActions(
                    walletAddress = address,
                    network = networkTypeString,
                    status = actionStatusString,
                ).getOrThrow().data,
                onError = { Timber.e("Error converting staking actions list: $it") },
            )
        }
    }

    private suspend fun StakingIntegrationID.StakeKit.getYieldRequest(): ApiResponse<EnabledYieldsResponse> {
        return when (this) {
            is StakingIntegrationID.StakeKit.Coin -> stakeKitApi.getEnabledYields(
                preferredValidatorsOnly = false,
                network = networkId,
            )
            is StakingIntegrationID.StakeKit.EthereumToken -> stakeKitApi.getEnabledYields(
                preferredValidatorsOnly = false,
                yieldId = value,
                network = networkId,
            )
        }
    }

    private fun getAvailableStakeKitIntegrationsIds(): List<StakingIntegrationID.StakeKit> {
        return StakingIntegrationID.StakeKit.entries
        // load all integrations for now and filter in use cases if needed
        // .filterNot {
        // it.blockchain == Blockchain.Cardano && !stakingFeatureToggles.isCardanoStakingEnabled
        // }
    }

    private fun NetworkTypeDTO.extractJsonName(): String {
        return networkTypeAdapter.toJson(this).replace("\"", "")
    }

    private fun StakingActionStatusDTO.extractJsonName(): String {
        return stakingActionStatusAdapter.toJson(this).replace("\"", "")
    }

    override suspend fun getEntryInfo(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): StakingEntryInfo {
        return withContext(dispatchers.io) {
            val yield = getYield(cryptoCurrencyId, symbol)

            StakingEntryInfo(
                tokenSymbol = yield.token.symbol,
            )
        }
    }

    override suspend fun createAction(
        userWalletId: UserWalletId,
        network: Network,
        params: ActionParams,
    ): StakingAction {
        return withContext(dispatchers.io) {
            val response = when (params.actionCommonType) {
                is StakingActionCommonType.Enter -> stakeKitApi.createEnterAction(
                    createActionRequestBody(
                        userWalletId,
                        network,
                        params,
                    ),
                )
                is StakingActionCommonType.Exit -> stakeKitApi.createExitAction(
                    createActionRequestBody(
                        userWalletId,
                        network,
                        params,
                    ),
                )
                is StakingActionCommonType.Pending -> stakeKitApi.createPendingAction(
                    createPendingActionRequestBody(params),
                )
            }

            enterActionResponseConverter.convert(response.getOrThrow())
        }
    }

    override suspend fun estimateGas(
        userWalletId: UserWalletId,
        network: Network,
        params: ActionParams,
    ): StakingGasEstimate {
        return withContext(dispatchers.io) {
            val gasEstimateDTO = when (params.actionCommonType) {
                is StakingActionCommonType.Enter -> stakeKitApi.estimateGasOnEnter(
                    createActionRequestBody(
                        userWalletId,
                        network,
                        params,
                    ),
                )
                is StakingActionCommonType.Exit -> stakeKitApi.estimateGasOnExit(
                    createActionRequestBody(
                        userWalletId,
                        network,
                        params,
                    ),
                )
                is StakingActionCommonType.Pending -> stakeKitApi.estimateGasOnPending(
                    createPendingActionRequestBody(params),
                )
            }

            GasEstimateConverter.convert(gasEstimateDTO.getOrThrow())
        }
    }

    override suspend fun constructTransaction(
        networkId: String,
        fee: Fee,
        amount: Amount,
        transactionId: String,
    ): Pair<StakingTransaction, TransactionData.Compiled> {
        return withContext(dispatchers.io) {
            val transactionResponse = stakeKitApi.constructTransaction(
                transactionId = transactionId,
                body = ConstructTransactionRequestBody(),
            )

            val transaction = transactionConverter.convert(transactionResponse.getOrThrow())
            val unsignedTransaction =
                transaction.unsignedTransaction ?: error("No unsigned transaction available")
            val transactionData = TransactionData.Compiled(
                value = getTransactionDataType(networkId, unsignedTransaction),
                fee = fee,
                amount = amount,
                status = TransactionStatus.Unconfirmed,
            )

            transaction to transactionData
        }
    }

    private suspend fun createActionRequestBody(
        userWalletId: UserWalletId,
        network: Network,
        params: ActionParams,
    ): ActionRequestBody {
        return ActionRequestBody(
            integrationId = params.integrationId,
            addresses = Address(
                address = params.address,
                additionalAddresses = createAdditionalAddresses(userWalletId, network, params),
            ),
            args = ActionRequestBodyArgs(
                amount = params.amount.toPlainString(),
                inputToken = YieldTokenConverter.convertBack(params.token),
                validatorAddress = params.validatorAddress,
                validatorAddresses = listOf(params.validatorAddress), // check on other networks
                tronResource = getTronResource(network),
            ),
        )
    }

    private fun createPendingActionRequestBody(params: ActionParams): PendingActionRequestBody {
        return PendingActionRequestBody(
            integrationId = params.integrationId,
            type = params.type ?: StakingActionType.UNKNOWN,
            passthrough = params.passthrough.orEmpty(),
            args = ActionRequestBodyArgs(
                amount = params.amount.toPlainString(),
                validatorAddress = params.validatorAddress,
                validatorAddresses = listOf(params.validatorAddress),
            ),
        )
    }

    private suspend fun createAdditionalAddresses(
        userWalletId: UserWalletId,
        network: Network,
        params: ActionParams,
    ): Address.AdditionalAddresses? {
        val selectedWallet = walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
        return when (params.token.network) {
            NetworkType.COSMOS -> Address.AdditionalAddresses(
                cosmosPubKey = Base64.encodeToString(
                    /* input = */ selectedWallet?.wallet?.publicKey?.blockchainKey?.toCompressedPublicKey(),
                    /* flags = */ Base64.NO_WRAP,
                ),
            )
            else -> null
        }
    }

    private fun getTransactionDataType(networkId: String, unsignedTransaction: String): TransactionData.Compiled.Data {
        return when (Blockchain.fromId(networkId)) {
            Blockchain.Solana,
            Blockchain.Cosmos,
            -> TransactionData.Compiled.Data.Bytes(unsignedTransaction.hexToBytes())
            Blockchain.BSC,
            Blockchain.Ethereum,
            Blockchain.TON,
            Blockchain.Cardano,
            -> TransactionData.Compiled.Data.RawString(unsignedTransaction)
            Blockchain.Tron -> {
                val tronStakeKitTransaction = tronStakeKitTransactionAdapter.fromJson(unsignedTransaction)
                    ?: error("Failed to parse Tron StakeKit transaction")
                TransactionData.Compiled.Data.RawString(tronStakeKitTransaction.rawDataHex)
            }
            else -> error("Unsupported blockchain")
        }
    }

    private fun findPrefetchedYield(yields: List<Yield>, currencyId: CryptoCurrency.RawID, symbol: String): Yield? {
        return yields.find { yield ->
            yield.tokens.any { it.coinGeckoId == currencyId.value && it.symbol == symbol }
        }
    }

    private suspend fun getEnabledYieldsSync(): List<Yield> {
        return YieldConverter.convertListIgnoreErrors(
            input = stakingYieldsStore.getSync(),
            onError = { Timber.e("Error converting one of the items in enabled yields: $it") },
        )
    }

    override fun getEnabledYields(): Flow<List<Yield>> {
        return stakingYieldsStore.get().map { yields ->
            YieldConverter.convertListIgnoreErrors(
                input = yields,
                onError = { Timber.e("Error converting one of the items in enabled yields: $it") },
            )
        }
    }

    override fun getStakingAvailability(
        integrationId: StakingIntegrationID.StakeKit,
        rawCurrencyId: CryptoCurrency.RawID,
        symbol: String,
    ): Flow<StakingAvailability> {
        return getEnabledYields()
            .distinctUntilChanged()
            .map { yields ->
                if (yields.isEmpty()) {
                    return@map StakingAvailability.TemporaryUnavailable
                }

                val prefetchedYield = findPrefetchedYield(
                    yields = yields,
                    currencyId = rawCurrencyId,
                    symbol = symbol,
                )

                if (prefetchedYield != null) {
                    StakingAvailability.Available(StakingOption.StakeKit(integrationId, prefetchedYield))
                } else {
                    StakingAvailability.TemporaryUnavailable
                }
            }
    }

    override suspend fun getStakingAvailabilitySync(
        integrationId: StakingIntegrationID.StakeKit,
        rawCurrencyId: CryptoCurrency.RawID,
        symbol: String,
    ): StakingAvailability {
        val yields = getEnabledYieldsSync()
        if (yields.isEmpty()) {
            return StakingAvailability.TemporaryUnavailable
        }

        val prefetchedYield = findPrefetchedYield(
            yields = yields,
            currencyId = rawCurrencyId,
            symbol = symbol,
        )

        return if (prefetchedYield != null) {
            StakingAvailability.Available(StakingOption.StakeKit(integrationId, prefetchedYield))
        } else {
            StakingAvailability.TemporaryUnavailable
        }
    }

    private fun getTronResource(network: Network): TronResource? {
        val blockchain = Blockchain.fromNetworkId(network.backendId)

        return if (blockchain == Blockchain.Tron || blockchain == Blockchain.TronTestnet) {
            TronResource.ENERGY
        } else {
            null
        }
    }
}