package com.tangem.data.staking

import android.util.Base64
import arrow.core.getOrElse
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.blockchainsdk.utils.toMigratedCoinId
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.staking.converters.*
import com.tangem.data.staking.converters.action.ActionStatusConverter
import com.tangem.data.staking.converters.action.EnterActionResponseConverter
import com.tangem.data.staking.converters.action.StakingActionTypeConverter
import com.tangem.data.staking.converters.transaction.GasEstimateConverter
import com.tangem.data.staking.converters.transaction.StakingTransactionConverter
import com.tangem.data.staking.converters.transaction.StakingTransactionStatusConverter
import com.tangem.data.staking.converters.transaction.StakingTransactionTypeConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.request.*
import com.tangem.datasource.api.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.api.stakekit.models.response.model.action.StakingActionStatusDTO
import com.tangem.datasource.api.stakekit.models.response.model.transaction.tron.TronStakeKitTransaction
import com.tangem.datasource.local.token.StakingBalanceStore
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.stakekit.*
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
internal class DefaultStakingRepository(
    private val stakeKitApi: StakeKitApi,
    private val stakingYieldsStore: StakingYieldsStore,
    private val stakingBalanceStore: StakingBalanceStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
    private val walletManagersFacade: WalletManagersFacade,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    moshi: Moshi,
) : StakingRepository {

    private val stakingNetworkTypeConverter = StakingNetworkTypeConverter()
    private val networkTypeConverter = StakingNetworkTypeConverter()
    private val transactionStatusConverter = StakingTransactionStatusConverter()
    private val transactionTypeConverter = StakingTransactionTypeConverter()
    private val actionStatusConverter = ActionStatusConverter()
    private val stakingActionTypeConverter = StakingActionTypeConverter()
    private val tokenConverter = TokenConverter(
        stakingNetworkTypeConverter = stakingNetworkTypeConverter,
    )
    private val yieldConverter = YieldConverter(
        tokenConverter = tokenConverter,
    )
    private val gasEstimateConverter = GasEstimateConverter(
        tokenConverter = tokenConverter,
    )
    private val transactionConverter = StakingTransactionConverter(
        networkTypeConverter = networkTypeConverter,
        transactionStatusConverter = transactionStatusConverter,
        transactionTypeConverter = transactionTypeConverter,
        gasEstimateConverter = gasEstimateConverter,
    )
    private val enterActionResponseConverter = EnterActionResponseConverter(
        actionStatusConverter = actionStatusConverter,
        stakingActionTypeConverter = stakingActionTypeConverter,
        transactionConverter = transactionConverter,
    )

    private val yieldBalanceConverter = YieldBalanceConverter()
    private val yieldBalanceListConverter = YieldBalanceListConverter(yieldBalanceConverter)

    private val tronStakeKitTransactionAdapter by lazy { moshi.adapter(TronStakeKitTransaction::class.java) }
    private val networkTypeAdapter by lazy { moshi.adapter(NetworkTypeDTO::class.java) }
    private val stakingActionStatusAdapter by lazy { moshi.adapter(StakingActionStatusDTO::class.java) }

    override fun getIntegrationKey(cryptoCurrencyId: CryptoCurrency.ID): String = with(cryptoCurrencyId) {
        rawNetworkId.plus(rawCurrencyId)
    }

    override fun getSupportedIntegrationId(cryptoCurrencyId: CryptoCurrency.ID): String? {
        return integrationIdMap.getOrDefault(getIntegrationKey(cryptoCurrencyId), null)
    }

    override suspend fun fetchEnabledYields(refresh: Boolean) {
        withContext(dispatchers.io) {
            cacheRegistry.invokeOnExpire(
                key = YIELDS_STORE_KEY,
                skipCache = refresh,
                block = {
                    val stakingTokensWithYields = stakeKitApi.getEnabledYields(preferredValidatorsOnly = false)
                        .getOrThrow()

                    stakingYieldsStore.store(stakingTokensWithYields.data.filter { it.isAvailable ?: false })
                },
            )
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

    override suspend fun getActions(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        networkType: NetworkType,
        stakingActionStatus: StakingActionStatus,
    ): List<StakingAction> {
        return withContext(dispatchers.io) {
            val address = walletManagersFacade.getDefaultAddress(userWalletId, cryptoCurrency.network).orEmpty()

            val networkTypeDto = networkTypeConverter.convertBack(networkType)
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
                apr = requireNotNull(yield.preferredValidators.maxByOrNull { it.apr.orZero() }?.apr),
                rewardSchedule = yield.metadata.rewardSchedule,
                tokenSymbol = yield.token.symbol,
            )
        }
    }

    override suspend fun getStakingAvailability(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): StakingAvailability {
        if (checkForInvalidCardBatch(userWalletId, cryptoCurrency)) return StakingAvailability.Unavailable

        val rawCurrencyId = cryptoCurrency.id.rawCurrencyId ?: return StakingAvailability.Unavailable

        val isSupportedInMobileApp = getSupportedIntegrationId(cryptoCurrency.id).isNullOrEmpty().not()

        val prefetchedYield = findPrefetchedYield(
            yields = getEnabledYieldsSync(),
            currencyId = rawCurrencyId,
            symbol = cryptoCurrency.symbol,
        )

        return when {
            prefetchedYield != null && isSupportedInMobileApp -> {
                StakingAvailability.Available(prefetchedYield.id)
            }
            prefetchedYield == null && isSupportedInMobileApp -> {
                StakingAvailability.TemporaryUnavailable
            }
            else -> StakingAvailability.Unavailable
        }
    }

    private fun checkForInvalidCardBatch(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        val userWallet = getUserWalletUseCase(userWalletId).getOrElse {
            error("Failed to get user wallet")
        }

        return when {
            isSolana(cryptoCurrency.network.id.value) -> {
                INVALID_BATCHES_FOR_SOLANA.contains(userWallet.scanResponse.card.batchId)
            }
            else -> {
                false
            }
        }
    }

    override suspend fun createAction(
        userWalletId: UserWalletId,
        network: Network,
        params: ActionParams,
    ): StakingAction {
        return withContext(dispatchers.io) {
            val response = when (params.actionCommonType) {
                StakingActionCommonType.Enter -> stakeKitApi.createEnterAction(
                    createActionRequestBody(
                        userWalletId,
                        network,
                        params,
                    ),
                )
                StakingActionCommonType.Exit -> stakeKitApi.createExitAction(
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
                StakingActionCommonType.Enter -> stakeKitApi.estimateGasOnEnter(
                    createActionRequestBody(
                        userWalletId,
                        network,
                        params,
                    ),
                )
                StakingActionCommonType.Exit -> stakeKitApi.estimateGasOnExit(
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

            gasEstimateConverter.convert(gasEstimateDTO.getOrThrow())
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
            val unsignedTransaction = transaction.unsignedTransaction ?: error("No unsigned transaction available")
            val transactionData = TransactionData.Compiled(
                value = getTransactionDataType(networkId, unsignedTransaction),
                fee = fee,
                amount = amount,
                status = TransactionStatus.Unconfirmed,
            )

            transaction to transactionData
        }
    }

    override suspend fun fetchSingleYieldBalance(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        refresh: Boolean,
    ) = withContext(dispatchers.io) {
        cacheRegistry.invokeOnExpire(
            key = getYieldBalancesKey(userWalletId),
            skipCache = refresh,
            block = {
                val integrationId = integrationIdMap[getIntegrationKey(cryptoCurrency.id)]
                val address = walletManagersFacade.getDefaultAddress(userWalletId, cryptoCurrency.network)

                if (integrationId == null || address.isNullOrBlank()) {
                    cacheRegistry.invalidate(getYieldBalancesKey(userWalletId))
                    Timber.w(
                        "IntegrationId or address is null fetching ${cryptoCurrency.name} staking balance",
                    )
                    return@invokeOnExpire
                }

                val requestBody = getBalanceRequestData(address, integrationId)
                val result = stakeKitApi.getSingleYieldBalance(
                    integrationId = requestBody.integrationId,
                    body = requestBody,
                ).getOrThrow()

                stakingBalanceStore.store(
                    userWalletId = userWalletId,
                    integrationId = requestBody.integrationId,
                    address = address,
                    item = YieldBalanceWrapperDTO(
                        balances = result,
                        integrationId = requestBody.integrationId,
                        addresses = requestBody.addresses,
                    ),
                )
            },
        )
    }

    override fun getSingleYieldBalanceFlow(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Flow<YieldBalance> = channelFlow {
        launch(dispatchers.io) {
            val address = walletManagersFacade.getDefaultAddress(userWalletId, cryptoCurrency.network).orEmpty()
            val integrationId = integrationIdMap[getIntegrationKey(cryptoCurrency.id)]
                ?: error("Could not get integrationId")
            stakingBalanceStore.get(userWalletId, address, integrationId)
                .distinctUntilChanged()
                .collectLatest {
                    if (it != null) {
                        send(yieldBalanceConverter.convert(it))
                    } else {
                        error("No yield balance available for currency ${cryptoCurrency.id.value}")
                    }
                }
        }

        withContext(dispatchers.io) {
            fetchSingleYieldBalance(
                userWalletId,
                cryptoCurrency,
            )
        }
    }.cancellable()

    override suspend fun getSingleYieldBalanceSync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): YieldBalance = withContext(dispatchers.io) {
        fetchSingleYieldBalance(userWalletId, cryptoCurrency)

        val address = walletManagersFacade.getDefaultAddress(userWalletId, cryptoCurrency.network).orEmpty()

        val integrationId = integrationIdMap[getIntegrationKey(cryptoCurrency.id)]
            ?: error("Could not get integrationId")

        val result = stakingBalanceStore.getSyncOrNull(userWalletId, address, integrationId)
            ?: return@withContext YieldBalance.Error

        yieldBalanceConverter.convert(result)
    }

    override suspend fun fetchMultiYieldBalance(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
        refresh: Boolean,
    ) = withContext(dispatchers.io) {
        cacheRegistry.invokeOnExpire(
            key = getYieldBalancesKey(userWalletId),
            skipCache = refresh,
            block = {
                val yieldDTOs = withTimeoutOrNull(YIELDS_WATITING_TIMEOUT) {
                    runCatching { stakingYieldsStore.get().firstOrNull() }.getOrNull()
                }

                if (yieldDTOs == null) {
                    Timber.i("No enabled yields for $userWalletId")
                    stakingBalanceStore.store(userWalletId, emptySet())

                    return@invokeOnExpire
                }

                val yields = yieldConverter.convertListIgnoreErrors(
                    input = yieldDTOs,
                    onError = { Timber.e("Error converting one of the items in enabled yields: $it") },
                )

                val availableCurrencies = cryptoCurrencies
                    .mapNotNull { currency ->
                        val addresses = walletManagersFacade.getAddresses(userWalletId, currency.network)
                        val integrationId = integrationIdMap[getIntegrationKey(currency.id)]

                        if (integrationId != null && yields.any { it.id == integrationId }) {
                            addresses to integrationId
                        } else {
                            null
                        }
                    }
                    .flatMap { (addresses, integrationId) ->
                        addresses.map { address -> address to integrationId }
                    }
                    .map { getBalanceRequestData(it.first.value, it.second) }
                    .ifEmpty {
                        stakingBalanceStore.store(userWalletId, emptySet())

                        cacheRegistry.invalidate(getYieldBalancesKey(userWalletId))

                        return@invokeOnExpire
                    }

                val yieldBalances = safeApiCall(
                    call = {
                        stakeKitApi
                            .getMultipleYieldBalances(availableCurrencies)
                            .bind()
                    },
                    onError = {
                        Timber.e(it, "Unable to fetch yield balances")
                        cacheRegistry.invalidate(getYieldBalancesKey(userWalletId))
                        emptySet()
                    },
                )

                stakingBalanceStore.store(userWalletId, yieldBalances)
            },
        )
    }

    override fun getMultiYieldBalanceUpdates(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): Flow<YieldBalanceList> = channelFlow {
        stakingBalanceStore.get(userWalletId)
            .onEach {
                val balances = yieldBalanceListConverter.convert(it)
                send(balances)
            }
            .launchIn(scope = this + dispatchers.io)

        withContext(dispatchers.io) {
            fetchMultiYieldBalance(userWalletId, cryptoCurrencies, refresh = false)
        }
    }

    override suspend fun getMultiYieldBalanceSync(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): YieldBalanceList = withContext(dispatchers.io) {
        fetchMultiYieldBalance(userWalletId, cryptoCurrencies)
        val result = stakingBalanceStore.getSyncOrNull(userWalletId) ?: return@withContext YieldBalanceList.Error
        yieldBalanceListConverter.convert(result)
    }

    override suspend fun isAnyTokenStaked(userWalletId: UserWalletId): Boolean {
        return withContext(dispatchers.io) {
            stakingBalanceStore.getSyncOrNull(userWalletId)
                ?.let {
                    it.isNotEmpty() && it.any { yieldBalance -> yieldBalance.balances.isNotEmpty() }
                }
                ?: false
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
                inputToken = tokenConverter.convertBack(params.token),
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

    override fun getStakingApproval(cryptoCurrency: CryptoCurrency): StakingApproval {
        return when (getIntegrationKey(cryptoCurrency.id)) {
            Blockchain.Ethereum.id + Blockchain.Polygon.toCoinId(),
            Blockchain.Ethereum.id + Blockchain.Polygon.toMigratedCoinId(),
            -> StakingApproval.Needed(ETHEREUM_POLYGON_APPROVE_SPENDER)
            else -> StakingApproval.Empty
        }
    }

    private fun getTransactionDataType(networkId: String, unsignedTransaction: String): TransactionData.Compiled.Data {
        val blockchain = Blockchain.fromId(networkId)
        return when (blockchain) {
            Blockchain.Solana,
            Blockchain.Cosmos,
            -> TransactionData.Compiled.Data.Bytes(unsignedTransaction.hexToBytes())
            Blockchain.BSC,
            Blockchain.Ethereum,
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
        return yieldConverter.convertListIgnoreErrors(
            input = stakingYieldsStore.getSync(),
            onError = { Timber.e("Error converting one of the items in enabled yields: $it") },
        )
    }

    private fun getBalanceRequestData(address: String, integrationId: String): YieldBalanceRequestBody {
        return YieldBalanceRequestBody(
            addresses = Address(
                address = address,
                additionalAddresses = null, // todo fill additional addresses metadata if needed
                explorerUrl = "", // todo fill exporer url [REDACTED_JIRA]
            ),
            args = YieldBalanceRequestBody.YieldBalanceRequestArgs(
                validatorAddresses = listOf(), // todo add validators [REDACTED_JIRA]
            ),
            integrationId = integrationId,
        )
    }

    private fun getYieldBalancesKey(userWalletId: UserWalletId) = "yield_balance_${userWalletId.stringValue}"

    private fun getTronResource(network: Network): TronResource? {
        val blockchain = Blockchain.fromNetworkId(network.backendId)

        return if (blockchain == Blockchain.Tron || blockchain == Blockchain.TronTestnet) {
            TronResource.ENERGY
        } else {
            null
        }
    }

    private companion object {
        const val YIELDS_STORE_KEY = "yields"

        const val SOLANA_INTEGRATION_ID = "solana-sol-native-multivalidator-staking"
        const val COSMOS_INTEGRATION_ID = "cosmos-atom-native-staking"
        const val ETHEREUM_POLYGON_INTEGRATION_ID = "ethereum-matic-native-staking"
        const val BINANCE_INTEGRATION_ID = "bsc-bnb-native-staking"
        const val POLKADOT_INTEGRATION_ID = "polkadot-dot-validator-staking"
        const val AVALANCHE_INTEGRATION_ID = "avalanche-avax-native-staking"
        const val TRON_INTEGRATION_ID = "tron-trx-native-staking"
        const val CRONOS_INTEGRATION_ID = "cronos-cro-native-staking"
        const val KAVA_INTEGRATION_ID = "kava-kava-native-staking"
        const val NEAR_INTEGRATION_ID = "near-near-native-staking"
        const val TEZOS_INTEGRATION_ID = "tezos-xtz-native-staking"

        const val ETHEREUM_POLYGON_APPROVE_SPENDER = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"

        val YIELDS_WATITING_TIMEOUT = 15.seconds

        val INVALID_BATCHES_FOR_SOLANA = listOf("AC01", "CB79")

        // uncomment items as implementation is ready
        val integrationIdMap = mapOf(
            Blockchain.Solana.run { id + toCoinId() } to SOLANA_INTEGRATION_ID,
            Blockchain.Cosmos.run { id + toCoinId() } to COSMOS_INTEGRATION_ID,
            Blockchain.Tron.run { id + toCoinId() } to TRON_INTEGRATION_ID,
            Blockchain.Ethereum.id + Blockchain.Polygon.toMigratedCoinId() to ETHEREUM_POLYGON_INTEGRATION_ID,
            // Blockchain.Ethereum.id + Blockchain.Polygon.toCoinId() to ETHEREUM_POLYGON_INTEGRATION_ID,
            Blockchain.BSC.run { id + toCoinId() } to BINANCE_INTEGRATION_ID,
            // Blockchain.Polkadot.run { id + toCoinId() } to POLKADOT_INTEGRATION_ID,
            // Blockchain.Avalanche.run { id + toCoinId() } to AVALANCHE_INTEGRATION_ID,
            // Blockchain.Cronos.run { id + toCoinId() } to CRONOS_INTEGRATION_ID,
            // Blockchain.Kava.run { id + toCoinId() } to KAVA_INTEGRATION_ID,
            // Blockchain.Near.run { id + toCoinId() } to NEAR_INTEGRATION_ID,
            // Blockchain.Tezos.run { id + toCoinId() } to TEZOS_INTEGRATION_ID,
        )
    }
}