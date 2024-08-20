package com.tangem.data.staking

import android.util.Base64
import arrow.core.raise.catch
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toCompressedPublicKey
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
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.datasource.local.token.StakingBalanceStore
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.lce.lceFlow
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.UnsubmittedTransactionMetadata
import com.tangem.domain.staking.model.stakekit.NetworkType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
internal class DefaultStakingRepository(
    private val stakeKitApi: StakeKitApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val stakingYieldsStore: StakingYieldsStore,
    private val stakingBalanceStore: StakingBalanceStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
    private val stakingFeatureToggle: StakingFeatureToggles,
    private val walletManagersFacade: WalletManagersFacade,
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

    private val yieldBalanceListConverter = YieldBalanceListConverter()

    private val isYieldBalanceFetching = MutableStateFlow(
        value = emptyMap<UserWalletId, Boolean>(),
    )

    override fun isStakingSupported(integrationKey: String): Boolean {
        return integrationIdMap.containsKey(integrationKey)
    }

    override suspend fun fetchEnabledYields(refresh: Boolean) {
        withContext(dispatchers.io) {
            cacheRegistry.invokeOnExpire(
                key = YIELDS_STORE_KEY,
                skipCache = refresh,
                block = {
                    val stakingTokensWithYields = stakeKitApi.getMultipleYields().getOrThrow()
                    stakingYieldsStore.store(stakingTokensWithYields.data)
                },
            )
        }
    }

    override suspend fun getYield(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Yield {
        return withContext(dispatchers.io) {
            val yields = getEnabledYields() ?: error("No yields found")
            val rawCurrencyId = cryptoCurrencyId.rawCurrencyId ?: error("Staking custom tokens is not available")

            val prefetchedYield = findPrefetchedYield(
                yields = yields,
                currencyId = rawCurrencyId,
                symbol = symbol,
            )

            prefetchedYield ?: error("Staking is unavailable")
        }
    }

    override suspend fun getEntryInfo(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): StakingEntryInfo {
        return withContext(dispatchers.io) {
            val yield = getYield(cryptoCurrencyId, symbol)

            StakingEntryInfo(
                interestRate = requireNotNull(yield.validators.maxByOrNull { it.apr.orZero() }?.apr),
                periodInDays = yield.metadata.cooldownPeriod.days,
                tokenSymbol = yield.token.symbol,
            )
        }
    }

    override suspend fun getStakingAvailabilityForActions(
        cryptoCurrencyId: CryptoCurrency.ID,
        symbol: String,
    ): StakingAvailability {
        val rawCurrencyId = cryptoCurrencyId.rawCurrencyId ?: return StakingAvailability.Unavailable

        return withContext(dispatchers.io) {
            val yields = getEnabledYields() ?: return@withContext StakingAvailability.Unavailable

            val prefetchedYield = findPrefetchedYield(yields, rawCurrencyId, symbol)
            val isSupported = isStakingSupported(cryptoCurrencyId.getIntegrationKey())

            when {
                prefetchedYield != null && isSupported -> {
                    StakingAvailability.Available(prefetchedYield.id)
                }
                prefetchedYield == null && isSupported -> {
                    StakingAvailability.TemporaryDisabled
                }
                else -> StakingAvailability.Unavailable
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
                StakingActionCommonType.ENTER -> stakeKitApi.createEnterAction(
                    createActionRequestBody(
                        userWalletId,
                        network,
                        params,
                    ),
                )
                StakingActionCommonType.EXIT -> stakeKitApi.createExitAction(
                    createActionRequestBody(
                        userWalletId,
                        network,
                        params,
                    ),
                )
                StakingActionCommonType.PENDING_OTHER,
                StakingActionCommonType.PENDING_REWARDS,
                -> stakeKitApi.createPendingAction(
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
                StakingActionCommonType.ENTER -> stakeKitApi.estimateGasOnEnter(
                    createActionRequestBody(
                        userWalletId,
                        network,
                        params,
                    ),
                )
                StakingActionCommonType.EXIT -> stakeKitApi.estimateGasOnExit(
                    createActionRequestBody(
                        userWalletId,
                        network,
                        params,
                    ),
                )
                StakingActionCommonType.PENDING_REWARDS,
                StakingActionCommonType.PENDING_OTHER,
                -> stakeKitApi.estimateGasOnPending(
                    createPendingActionRequestBody(params),
                )
            }

            gasEstimateConverter.convert(gasEstimateDTO.getOrThrow())
        }
    }

    override suspend fun constructTransaction(
        networkId: String,
        fee: Fee,
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
        if (!stakingFeatureToggle.isStakingEnabled) return@withContext

        val integrationId = integrationIdMap[cryptoCurrency.id.getIntegrationKey()] ?: return@withContext

        val address = walletManagersFacade.getDefaultAddress(userWalletId, cryptoCurrency.network).orEmpty()

        cacheRegistry.invokeOnExpire(
            key = getYieldBalancesKey(userWalletId),
            skipCache = refresh,
            block = {
                val requestBody = getBalanceRequestData(address, integrationId)
                val result = stakeKitApi.getSingleYieldBalance(
                    integrationId = requestBody.integrationId,
                    body = requestBody,
                ).getOrThrow()

                stakingBalanceStore.store(
                    userWalletId,
                    requestBody.integrationId,
                    YieldBalanceWrapperDTO(
                        balances = result,
                        integrationId = requestBody.integrationId,
                    ),
                )
            },
        )
    }

    override fun getSingleYieldBalanceFlow(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Flow<YieldBalance> = channelFlow {
        if (!stakingFeatureToggle.isStakingEnabled) {
            send(YieldBalance.Empty)
        } else {
            launch(dispatchers.io) {
                val integrationId = integrationIdMap[cryptoCurrency.id.getIntegrationKey()]
                    ?: error("Could not get integrationId")
                stakingBalanceStore.get(userWalletId, integrationId)
                    .collectLatest {
                        send(
                            yieldBalanceConverter.convert(
                                YieldBalanceConverter.Data(
                                    balance = it,
                                    integrationId = integrationId,
                                ),
                            ),
                        )
                    }
            }

            withContext(dispatchers.io) {
                fetchSingleYieldBalance(
                    userWalletId,
                    cryptoCurrency,
                )
            }
        }
    }.cancellable()

    override suspend fun getSingleYieldBalanceSync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): YieldBalance = withContext(dispatchers.io) {
        if (!stakingFeatureToggle.isStakingEnabled) {
            YieldBalance.Empty
        } else {
            fetchSingleYieldBalance(userWalletId, cryptoCurrency)

            val integrationId = integrationIdMap[cryptoCurrency.id.getIntegrationKey()]
                ?: error("Could not get integrationId")
            val result = stakingBalanceStore.getSyncOrNull(userWalletId, integrationId)
                ?: return@withContext YieldBalance.Error

            yieldBalanceConverter.convert(
                YieldBalanceConverter.Data(
                    balance = result,
                    integrationId = integrationId,
                ),
            )
        }
    }

    override suspend fun fetchMultiYieldBalance(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
        refresh: Boolean,
    ) = withContext(dispatchers.io) {
        if (!stakingFeatureToggle.isStakingEnabled) return@withContext
        try {
            isYieldBalanceFetching.update {
                it + (userWalletId to true)
            }
            cacheRegistry.invokeOnExpire(
                key = getYieldBalancesKey(userWalletId),
                skipCache = refresh,
                block = {
                    val availableCurrencies = cryptoCurrencies
                        .mapNotNull { currency ->
                            val address = walletManagersFacade.getDefaultAddress(userWalletId, currency.network)
                            val integrationId = integrationIdMap[currency.id.getIntegrationKey()]

                            if (integrationId != null && address != null) {
                                address to integrationId
                            } else {
                                null
                            }
                        }
                        .distinct()
                        .map { getBalanceRequestData(it.first, it.second) }
                        .ifEmpty { return@invokeOnExpire }
                    val result = stakeKitApi.getMultipleYieldBalances(availableCurrencies).getOrThrow()

                    stakingBalanceStore.store(userWalletId, result)
                },
            )
        } finally {
            isYieldBalanceFetching.update {
                it - userWalletId
            }
        }
    }

    override fun getMultiYieldBalanceFlow(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): Flow<YieldBalanceList> = channelFlow {
        if (!stakingFeatureToggle.isStakingEnabled) {
            send(YieldBalanceList.Empty)
        } else {
            launch(dispatchers.io) {
                stakingBalanceStore.get(userWalletId)
                    .collectLatest { send(yieldBalanceListConverter.convert(it)) }
            }

            withContext(dispatchers.io) {
                fetchMultiYieldBalance(
                    userWalletId,
                    cryptoCurrencies,
                )
            }
        }
    }.cancellable()

    override fun getMultiYieldBalanceLce(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): LceFlow<Throwable, YieldBalanceList> = lceFlow {
        if (!stakingFeatureToggle.isStakingEnabled) {
            send(YieldBalanceList.Empty)
        } else {
            launch(dispatchers.io) {
                combine(
                    stakingBalanceStore.get(userWalletId),
                    isYieldBalanceFetching.map { it.getOrElse(userWalletId) { false } },
                ) { result, isFetching ->
                    val balances = yieldBalanceListConverter.convert(result)
                    send(balances, isStillLoading = isFetching)
                }.collect()
            }
            withContext(dispatchers.io) {
                catch(
                    block = { fetchMultiYieldBalance(userWalletId, cryptoCurrencies, refresh = false) },
                    catch = { raise(it) },
                )
            }
        }
    }

    override suspend fun getMultiYieldBalanceSync(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): YieldBalanceList = withContext(dispatchers.io) {
        if (!stakingFeatureToggle.isStakingEnabled) {
            YieldBalanceList.Empty
        } else {
            fetchMultiYieldBalance(userWalletId, cryptoCurrencies)
            val result = stakingBalanceStore.getSyncOrNull(userWalletId) ?: return@withContext YieldBalanceList.Error
            yieldBalanceListConverter.convert(result)
        }
    }

    override suspend fun submitHash(transactionId: String, transactionHash: String) {
        withContext(dispatchers.io) {
            stakeKitApi.submitTransactionHash(
                transactionId = transactionId,
                body = SubmitTransactionHashRequestBody(
                    hash = transactionHash,
                ),
            )
        }
    }

    override suspend fun storeUnsubmittedHash(unsubmittedTransactionMetadata: UnsubmittedTransactionMetadata) {
        withContext(dispatchers.io) {
            appPreferencesStore.editData { preferences ->
                val savedTransactions = preferences.getObjectListOrDefault<UnsubmittedTransactionMetadata>(
                    key = PreferencesKeys.UNSUBMITTED_TRANSACTIONS_KEY,
                    default = emptyList(),
                )

                preferences.setObjectList(
                    key = PreferencesKeys.UNSUBMITTED_TRANSACTIONS_KEY,
                    value = savedTransactions + unsubmittedTransactionMetadata,
                )
            }
        }
    }

    override suspend fun sendUnsubmittedHashes() {
        withContext(NonCancellable) {
            val savedTransactions = appPreferencesStore.getObjectListSync<UnsubmittedTransactionMetadata>(
                key = PreferencesKeys.UNSUBMITTED_TRANSACTIONS_KEY,
            )

            savedTransactions.forEach {
                stakeKitApi.submitTransactionHash(
                    transactionId = it.transactionId,
                    body = SubmitTransactionHashRequestBody(hash = it.transactionHash),
                )
            }

            appPreferencesStore.editData { mutablePreferences ->
                mutablePreferences.setObjectList<UnsubmittedTransactionMetadata>(
                    key = PreferencesKeys.UNSUBMITTED_TRANSACTIONS_KEY,
                    value = emptyList(),
                )
            }
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

    override fun isStakeMoreAvailable(networkId: Network.ID): Boolean {
        val blockchain = Blockchain.fromId(networkId.value)
        return when (blockchain) {
            Blockchain.Solana -> false
            else -> true
        }
    }

    override fun getStakingApproval(cryptoCurrency: CryptoCurrency): StakingApproval {
        return when (cryptoCurrency.id.getIntegrationKey()) {
            Blockchain.Ethereum.id + Blockchain.Polygon.toCoinId() -> {
                StakingApproval.Needed(ETHEREUM_POLYGON_APPROVE_SPENDER)
            }
            else -> StakingApproval.Empty
        }
    }

    private fun getTransactionDataType(networkId: String, unsignedTransaction: String): TransactionData.Compiled.Data {
        val blockchain = Blockchain.fromId(networkId)
        return when (blockchain) {
            Blockchain.Solana,
            Blockchain.Cosmos,
            -> TransactionData.Compiled.Data.Bytes(unsignedTransaction.hexToBytes())
            Blockchain.Ethereum,
            -> TransactionData.Compiled.Data.RawString(unsignedTransaction)
            else -> error("Unsupported blockchain")
        }
    }

    private fun findPrefetchedYield(yields: List<Yield>, currencyId: String, symbol: String): Yield? {
        return yields.find { it.token.coinGeckoId == currencyId && it.token.symbol == symbol }
    }

    private suspend fun getEnabledYields(): List<Yield>? {
        val yields = stakingYieldsStore.getSyncOrNull() ?: return null
        return yields.map { yieldConverter.convert(it) }
    }

    private fun getBalanceRequestData(address: String, integrationId: String): YieldBalanceRequestBody {
        return YieldBalanceRequestBody(
            addresses = Address(
                address = address,
                additionalAddresses = null, // todo fill additional addresses metadata if needed
                explorerUrl = "", // todo fill exporer url https://tangem.atlassian.net/browse/AND-7138
            ),
            args = YieldBalanceRequestBody.YieldBalanceRequestArgs(
                validatorAddresses = listOf(), // todo add validators https://tangem.atlassian.net/browse/AND-7138
            ),
            integrationId = integrationId,
        )
    }

    private fun getYieldBalancesKey(userWalletId: UserWalletId) = "yield_balance_${userWalletId.stringValue}"

    private fun CryptoCurrency.ID.getIntegrationKey(): String = rawNetworkId.plus(rawCurrencyId)

    private companion object {
        const val YIELDS_STORE_KEY = "yields"

        const val SOLANA_INTEGRATION_ID = "solana-sol-native-multivalidator-staking"
        const val COSMOS_INTEGRATION_ID = "cosmos-atom-native-staking"
        const val ETHEREUM_POLYGON_INTEGRATION_ID = "ethereum-matic-native-staking"
        const val POLKADOT_INTEGRATION_ID = "polkadot-dot-validator-staking"
        const val AVALANCHE_INTEGRATION_ID = "avalanche-avax-native-staking"
        const val TRON_INTEGRATION_ID = "tron-trx-native-staking"
        const val CRONOS_INTEGRATION_ID = "cronos-cro-native-staking"
        const val BINANCE_INTEGRATION_ID = "bsc-bnb-native-staking"
        const val KAVA_INTEGRATION_ID = "kava-kava-native-staking"
        const val NEAR_INTEGRATION_ID = "near-near-native-staking"
        const val TEZOS_INTEGRATION_ID = "tezos-xtz-native-staking"

        const val ETHEREUM_POLYGON_APPROVE_SPENDER = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"

        // uncomment items as implementation is ready
        val integrationIdMap = mapOf(
            Blockchain.Solana.run { id + toCoinId() } to SOLANA_INTEGRATION_ID,
            Blockchain.Cosmos.run { id + toCoinId() } to COSMOS_INTEGRATION_ID,
            Blockchain.Ethereum.id + Blockchain.Polygon.toCoinId() to ETHEREUM_POLYGON_INTEGRATION_ID,
            // Blockchain.Polkadot.run { id + toCoinId() } to POLKADOT_INTEGRATION_ID,
            // Blockchain.Avalanche.run { id + toCoinId() } to AVALANCHE_INTEGRATION_ID,
            // Blockchain.Tron.run { id + toCoinId() } to TRON_INTEGRATION_ID,
            // Blockchain.Cronos.run { id + toCoinId() } to CRONOS_INTEGRATION_ID,
            // Blockchain.BSC.run { id + toCoinId() } to BINANCE_INTEGRATION_ID,
            // Blockchain.Kava.run { id + toCoinId() } to KAVA_INTEGRATION_ID,
            // Blockchain.Near.run { id + toCoinId() } to NEAR_INTEGRATION_ID,
            // Blockchain.Tezos.run { id + toCoinId() } to TEZOS_INTEGRATION_ID,
        )
    }
}
