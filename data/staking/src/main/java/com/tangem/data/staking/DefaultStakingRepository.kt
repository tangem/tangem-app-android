package com.tangem.data.staking

import android.util.Base64
import arrow.core.getOrElse
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.blockchainsdk.utils.toMigratedCoinId
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.staking.converters.YieldBalanceListConverter
import com.tangem.data.staking.converters.YieldConverter
import com.tangem.data.staking.converters.action.ActionStatusConverter
import com.tangem.data.staking.converters.action.EnterActionResponseConverter
import com.tangem.data.staking.converters.transaction.GasEstimateConverter
import com.tangem.data.staking.converters.transaction.StakingTransactionConverter
import com.tangem.data.staking.converters.transaction.StakingTransactionStatusConverter
import com.tangem.data.staking.converters.transaction.StakingTransactionTypeConverter
import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.data.staking.utils.StakingIdFactory
import com.tangem.data.staking.utils.StakingIdFactory.Companion.integrationIdMap
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.request.*
import com.tangem.datasource.api.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.api.stakekit.models.response.model.action.StakingActionStatusDTO
import com.tangem.datasource.api.stakekit.models.response.model.transaction.tron.TronStakeKitTransaction
import com.tangem.datasource.local.token.StakingBalanceStore
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.datasource.local.token.converter.StakingNetworkTypeConverter
import com.tangem.datasource.local.token.converter.TokenConverter
import com.tangem.domain.common.TapWorkarounds.isWallet2
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.stakekit.NetworkType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.lib.crypto.BlockchainUtils.isCardano
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
internal class DefaultStakingRepository(
    private val stakeKitApi: StakeKitApi,
    private val stakingYieldsStore: StakingYieldsStore,
    private val stakingBalanceStore: StakingBalanceStore,
    private val stakingBalanceStoreV2: YieldsBalancesStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
    private val walletManagersFacade: WalletManagersFacade,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val stakingFeatureToggles: StakingFeatureToggles,
    private val stakingIdFactory: StakingIdFactory,
    moshi: Moshi,
) : StakingRepository {

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

    override fun getIntegrationKey(cryptoCurrencyId: CryptoCurrency.ID): String = with(cryptoCurrencyId) {
        rawNetworkId.plus(rawCurrencyId)
    }

    override fun getSupportedIntegrationId(cryptoCurrencyId: CryptoCurrency.ID): String? {
        return integrationIdMap.getOrDefault(getIntegrationKey(cryptoCurrencyId), null)
    }

    override suspend fun fetchEnabledYields() {
        withContext(dispatchers.io) {
            when (val stakingTokensWithYields = stakeKitApi.getEnabledYields(preferredValidatorsOnly = false)) {
                is ApiResponse.Success -> stakingYieldsStore.store(
                    stakingTokensWithYields.data.data.filter {
                        it.isAvailable ?: false
                    },
                )
                else -> {
                    stakingYieldsStore.store(emptyList())
                    throw (stakingTokensWithYields as ApiResponse.Error).cause
                }
            }
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

    override fun getStakingAvailability(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Flow<StakingAvailability> {
        return channelFlow {
            if (!checkFeatureToggleEnabled(cryptoCurrency.network.id)) {
                send(StakingAvailability.Unavailable)
                return@channelFlow
            }

            if (checkForInvalidCardBatch(userWalletId, cryptoCurrency)) {
                send(StakingAvailability.Unavailable)
                return@channelFlow
            }

            val rawCurrencyId = cryptoCurrency.id.rawCurrencyId
            if (rawCurrencyId == null) {
                send(StakingAvailability.Unavailable)
                return@channelFlow
            }

            val isSupportedInMobileApp = getSupportedIntegrationId(cryptoCurrency.id).isNullOrEmpty().not()

            getEnabledYields()
                .distinctUntilChanged()
                .onEach { yields ->
                    if (yields.isEmpty()) {
                        send(StakingAvailability.TemporaryUnavailable)
                        return@onEach
                    }

                    val prefetchedYield = findPrefetchedYield(
                        yields = yields,
                        currencyId = rawCurrencyId,
                        symbol = cryptoCurrency.symbol,
                    )
                    when {
                        prefetchedYield != null && isSupportedInMobileApp -> {
                            send(StakingAvailability.Available(prefetchedYield.id))
                        }
                        prefetchedYield == null && isSupportedInMobileApp -> {
                            send(StakingAvailability.TemporaryUnavailable)
                        }
                        else -> send(StakingAvailability.Unavailable)
                    }
                }
                .launchIn(this)
        }
    }

    private fun checkFeatureToggleEnabled(networkId: Network.ID): Boolean {
        return when (networkId.toBlockchain()) {
            Blockchain.TON -> stakingFeatureToggles.isTonStakingEnabled
            Blockchain.Cardano -> stakingFeatureToggles.isCardanoStakingEnabled
            else -> true
        }
    }

    private fun checkForInvalidCardBatch(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        val userWallet = getUserWalletUseCase(userWalletId).getOrElse {
            error("Failed to get user wallet")
        }

        if (userWallet !is UserWallet.Cold) {
            return false
        }

        val blockchainId = cryptoCurrency.network.rawId
        return when {
            isSolana(blockchainId) -> INVALID_BATCHES_FOR_SOLANA.contains(userWallet.scanResponse.card.batchId)
            isCardano(blockchainId) -> !userWallet.scanResponse.card.isWallet2
            else -> false
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

                safeApiCall(
                    call = {
                        val result = stakeKitApi.getSingleYieldBalance(
                            integrationId = requestBody.integrationId,
                            body = requestBody,
                        ).bind()

                        stakingBalanceStore.store(
                            userWalletId = userWalletId,
                            stakingID = StakingBalanceStore.StakingID(
                                integrationId = requestBody.integrationId,
                                address = address,
                            ),
                            item = YieldBalanceWrapperDTO(
                                balances = result,
                                integrationId = requestBody.integrationId,
                                addresses = requestBody.addresses,
                            ),
                        )
                    },
                    onError = {
                        stakingBalanceStore.storeSingleYieldBalance(
                            userWalletId = userWalletId,
                            item = YieldBalance.Error(integrationId = requestBody.integrationId, address = address),
                        )
                    },
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

            stakingBalanceStore.get(
                userWalletId = userWalletId,
                stakingID = StakingBalanceStore.StakingID(integrationId = integrationId, address = address),
            )
                .distinctUntilChanged()
                .collectLatest {
                    if (it != null) {
                        send(it)
                    } else {
                        FirebaseCrashlytics.getInstance()
                            .log("No yield balance available for currency ${cryptoCurrency.id.value}")
                        send(YieldBalance.Error(integrationId, address))
                    }
                }
        }

        withContext(dispatchers.io) {
            fetchSingleYieldBalance(userWalletId = userWalletId, cryptoCurrency = cryptoCurrency)
        }
    }.cancellable()

    override suspend fun getSingleYieldBalanceSyncLegacy(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): YieldBalance = withContext(dispatchers.io) {
        fetchSingleYieldBalance(userWalletId, cryptoCurrency)

        val address = walletManagersFacade.getDefaultAddress(userWalletId, cryptoCurrency.network).orEmpty()

        val integrationId = integrationIdMap[getIntegrationKey(cryptoCurrency.id)]
            ?: error("Could not get integrationId")

        stakingBalanceStore.getSyncOrNull(
            userWalletId = userWalletId,
            stakingID = StakingBalanceStore.StakingID(integrationId = integrationId, address = address),
        )
            ?: YieldBalance.Error(integrationId, address)
    }

    override suspend fun getSingleYieldBalanceSync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): YieldBalance {
        val stakingId = stakingIdFactory.createForDefault(
            userWalletId = userWalletId,
            currencyId = cryptoCurrency.id,
            network = cryptoCurrency.network,
        ) ?: error("Could not create stakingId")

        return stakingBalanceStoreV2.getSyncOrNull(userWalletId = userWalletId, stakingId = stakingId)
            ?: YieldBalance.Error(integrationId = stakingId.integrationId, address = stakingId.address)
    }

    @Suppress("LongMethod")
    override suspend fun fetchMultiYieldBalance(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
        refresh: Boolean,
    ) = withContext(dispatchers.io) {
        if (refresh) {
            stakingBalanceStore.refresh(
                userWalletId = userWalletId,
                stakingIds = cryptoCurrencies.mapStakingId(userWalletId),
            )
        }

        val yieldDTOs = withTimeoutOrNull(YIELDS_WATITING_TIMEOUT) {
            runCatching { stakingYieldsStore.get().firstOrNull() }.getOrNull()
        }

        if (yieldDTOs == null) {
            Timber.i("No enabled yields for $userWalletId")
            stakingBalanceStore.store(userWalletId, emptySet())

            return@withContext
        }

        cacheRegistry.invokeOnExpire(
            key = getYieldBalancesKey(userWalletId),
            skipCache = refresh,
            block = {
                val yields = YieldConverter.convertListIgnoreErrors(
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

    private suspend fun List<CryptoCurrency>.mapStakingId(
        userWalletId: UserWalletId,
    ): List<StakingBalanceStore.StakingID> {
        return this
            .mapNotNull { currency ->
                val addresses = walletManagersFacade.getAddresses(userWalletId, currency.network)
                val integrationId = integrationIdMap[getIntegrationKey(currency.id)]

                if (integrationId != null) {
                    addresses to integrationId
                } else {
                    null
                }
            }
            .flatMap { (addresses, integrationId) ->
                addresses.map { address ->
                    StakingBalanceStore.StakingID(
                        integrationId = integrationId,
                        address = address.value,
                    )
                }
            }
    }

    override fun getMultiYieldBalanceUpdates(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): Flow<YieldBalanceList> {
        return flow {
            stakingBalanceStore.get(
                userWalletId = userWalletId,
                stakingIds = cryptoCurrencies.mapStakingId(userWalletId),
            )
                .map(YieldBalanceListConverter::convert)
                .collect { emit(it) }
        }
    }

    override suspend fun getMultiYieldBalanceSyncLegacy(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): YieldBalanceList = withContext(dispatchers.io) {
        fetchMultiYieldBalance(userWalletId, cryptoCurrencies)

        stakingBalanceStore.getSyncOrNull(userWalletId, cryptoCurrencies.mapStakingId(userWalletId))
            ?.let(YieldBalanceListConverter::convert)
            ?: YieldBalanceList.Error
    }

    override suspend fun getMultiYieldBalanceSync(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): YieldBalanceList {
        val stakingIds = cryptoCurrencies.flatMap {
            stakingIdFactory.create(userWalletId = userWalletId, currencyId = it.id, network = it.network)
        }

        return stakingBalanceStoreV2.getAllSyncOrNull(userWalletId)
            ?.filter { it.getStakingId() in stakingIds }
            ?.let { YieldBalanceListConverter.convert(value = it.toSet()) }
            ?: YieldBalanceList.Error
    }

    override suspend fun isAnyTokenStaked(userWalletId: UserWalletId): Boolean {
        return withContext(dispatchers.io) {
            stakingBalanceStore.getSyncOrNull(userWalletId)
                ?.let {
                    it.isNotEmpty() &&
                        it.any { yieldBalance ->
                            (yieldBalance as? YieldBalance.Data)?.balance?.items?.isNotEmpty() == true
                        }
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
                inputToken = TokenConverter.convertBack(params.token),
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

    private fun getEnabledYields(): Flow<List<Yield>> {
        return stakingYieldsStore.get().map {
            YieldConverter.convertListIgnoreErrors(
                input = it,
                onError = { Timber.e("Error converting one of the items in enabled yields: $it") },
            )
        }
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

    @Suppress("unused")
    companion object {
        private const val YIELDS_STORE_KEY = "yields"

        private const val ETHEREUM_POLYGON_APPROVE_SPENDER = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"

        internal val YIELDS_WATITING_TIMEOUT = 15.seconds

        private val INVALID_BATCHES_FOR_SOLANA = listOf("AC01", "CB79")
    }
}