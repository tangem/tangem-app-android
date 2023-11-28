package com.tangem.feature.swap.domain

import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyStatusesSyncUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.utils.convertToAmount
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.swap.domain.cache.SwapDataCache
import com.tangem.feature.swap.domain.converters.SwapCurrencyConverter
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.data.AggregatedSwapDataModel
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.toStringWithRightOffset
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.*
import com.tangem.lib.crypto.models.transactions.SendTxResult
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.toFiatString
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@Suppress("LargeClass", "LongParameterList")
internal class SwapInteractorImpl @Inject constructor(
    private val transactionManager: TransactionManager,
    private val userWalletManager: UserWalletManager,
    private val repository: SwapRepository,
    private val cache: SwapDataCache,
    private val allowPermissionsHandler: AllowPermissionsHandler,
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val walletFeatureToggles: WalletFeatureToggles,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getMultiCryptoCurrencyStatusUseCase: GetCryptoCurrencyStatusesSyncUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val quotesRepository: QuotesRepository,
    private val dispatcher: CoroutineDispatcherProvider,
) : SwapInteractor {

    // TODO: Move to DI
    private val addCryptoCurrenciesUseCase by lazy(LazyThreadSafetyMode.NONE) {
        AddCryptoCurrenciesUseCase(currenciesRepository, networksRepository)
    }

    private val getFeeUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetFeeUseCase(walletManagersFacade, dispatcher)
    }

    private val swapCurrencyConverter = SwapCurrencyConverter()
    private val amountFormatter = AmountFormatter()
    private var derivationPath: String? = null
    private var network: Network? = null

    override suspend fun getTokensDataState(currency: CryptoCurrency): TokensDataStateExpress {
        val selectedWallet = getSelectedWalletSyncUseCase().fold(
            ifLeft = { null },
            ifRight = { it },
        )

        requireNotNull(selectedWallet) { "No selected wallet" }

        val walletCurrencyStatuses = getMultiCryptoCurrencyStatusUseCase(selectedWallet.walletId)
            .getOrElse { emptyList() }

        val walletCurrencyStatusesExceptInitial = walletCurrencyStatuses.filter {
            it.currency.network.backendId != currency.network.backendId ||
                it.currency.getContractAddress() != currency.getContractAddress()
        }

        val pairsLeast = getPairs(
            initialCurrency = LeastTokenInfo(
                contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress ?: "0",
                network = currency.network.backendId,
            ),
            currenciesList = walletCurrencyStatusesExceptInitial.map { it.currency },
        )

        return TokensDataStateExpress(
            fromGroup = getToCurrenciesGroup(
                currency = currency,
                leastPairs = pairsLeast,
                cryptoCurrenciesList = walletCurrencyStatusesExceptInitial,
                tokenInfoForFilter = { it.from },
                tokenInfoForAvailable = { it.to },
            ),
            toGroup = getToCurrenciesGroup(
                currency = currency,
                leastPairs = pairsLeast,
                cryptoCurrenciesList = walletCurrencyStatusesExceptInitial,
                tokenInfoForFilter = { it.to },
                tokenInfoForAvailable = { it.from },
            ),
        )
    }

    override fun getSelectedWallet(): UserWallet? {
        return getSelectedWalletSyncUseCase().getOrNull()
    }

    private fun getToCurrenciesGroup(
        currency: CryptoCurrency,
        leastPairs: List<SwapPairLeast>,
        cryptoCurrenciesList: List<CryptoCurrencyStatus>,
        tokenInfoForFilter: (SwapPairLeast) -> LeastTokenInfo,
        tokenInfoForAvailable: (SwapPairLeast) -> LeastTokenInfo,
    ): CurrenciesGroup {
        val filteredPairs = leastPairs.filter {
            tokenInfoForFilter(it).contractAddress == currency.getContractAddress() &&
                tokenInfoForFilter(it).network == currency.network.backendId
        }

        val availableCryptoCurrencies = filteredPairs.mapNotNull { pair ->
            val status = findCryptoCurrencyStatusByLeastInfo(tokenInfoForAvailable(pair), cryptoCurrenciesList)
            status?.let { CryptoCurrencySwapInfo(it, pair.providers) }
        }

        val unavailableCryptoCurrencies = cryptoCurrenciesList - availableCryptoCurrencies
            .map { it.currencyStatus }
            .toSet()

        return CurrenciesGroup(
            available = availableCryptoCurrencies,
            unavailable = unavailableCryptoCurrencies.map { CryptoCurrencySwapInfo(it, emptyList()) },
        )
    }

    private fun findCryptoCurrencyStatusByLeastInfo(
        leastTokenInfo: LeastTokenInfo,
        cryptoCurrencyStatusesList: List<CryptoCurrencyStatus>,
    ): CryptoCurrencyStatus? {
        return cryptoCurrencyStatusesList.find {
            it.currency.network.backendId == leastTokenInfo.network &&
                it.currency.getContractAddress() == leastTokenInfo.contractAddress
        }
    }

    private fun CryptoCurrency.getContractAddress(): String {
        return when (this) {
            is CryptoCurrency.Token -> this.contractAddress
            is CryptoCurrency.Coin -> "0"
        }
    }

    private suspend fun getPairs(
        initialCurrency: LeastTokenInfo,
        currenciesList: List<CryptoCurrency>,
    ): List<SwapPairLeast> {
        return repository.getPairs(initialCurrency, currenciesList)
    }

    @Deprecated("used in old swap mechanism")
    override fun initDerivationPathAndNetwork(derivationPath: String?, network: Network) {
        this.derivationPath = derivationPath
        this.network = network
    }

    @Deprecated("used in old swap mechanism")
    override suspend fun searchTokens(networkId: String, searchQuery: String): FoundTokensStateExpress {
        val searchQueryLowerCase = searchQuery.lowercase()
        val tokensInWallet = cache.getInWalletTokens()
            .filter {
                it.token.name.lowercase().contains(searchQueryLowerCase) ||
                    it.token.symbol.lowercase().contains(searchQueryLowerCase)
            }
        val loadedTokens = cache.getLoadedTokens()
            .filter {
                it.token.name.lowercase().contains(searchQueryLowerCase) ||
                    it.token.symbol.lowercase().contains(searchQueryLowerCase)
            }
        return FoundTokensStateExpress(
            tokensInWallet = tokensInWallet,
            loadedTokens = loadedTokens,
        )
    }

    @Deprecated("used in old swap mechanism")
    override suspend fun givePermissionToSwap(networkId: String, permissionOptions: PermissionOptions): TxState {
        val dataToSign = if (permissionOptions.approveType == SwapApproveType.UNLIMITED) {
            getApproveData(
                networkId = networkId,
                derivationPath = derivationPath,
                fromToken = permissionOptions.fromToken,
                spenderAddress = permissionOptions.spenderAddress,
            )
        } else {
            permissionOptions.approveData.approveData
        }
        val result = transactionManager.sendApproveTransaction(
            txData = ApproveTxData(
                networkId = networkId,
                feeAmount = permissionOptions.txFee.feeValue,
                gasLimit = permissionOptions.txFee.gasLimit,
                destinationAddress = getTokenAddress(permissionOptions.fromToken),
                dataToSign = dataToSign,
            ),
            derivationPath = derivationPath,
            analyticsData = AnalyticsData(
                feeType = permissionOptions.txFee.feeType.getNameForAnalytics(),
                tokenSymbol = permissionOptions.fromToken.symbol,
                permissionType = permissionOptions.approveType.getNameForAnalytics(),
            ),
        )
        return when (result) {
            is SendTxResult.Success -> {
                allowPermissionsHandler.addAddressToInProgress(permissionOptions.forTokenContractAddress)
                TxState.TxSent(txAddress = userWalletManager.getLastTransactionHash(networkId, derivationPath) ?: "")
            }
            SendTxResult.UserCancelledError -> TxState.UserCancelled
            is SendTxResult.BlockchainSdkError -> TxState.BlockchainError
            is SendTxResult.TangemSdkError -> TxState.TangemSdkError
            is SendTxResult.NetworkError -> TxState.NetworkError
            is SendTxResult.UnknownError -> TxState.UnknownError
        }
    }

    @Deprecated("used in old swap mechanism")
    override suspend fun findBestQuote(
        networkId: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        providers: List<SwapProvider>,
        amountToSwap: String,
        selectedFee: FeeType,
    ): Map<SwapProvider, SwapState> {
        return providers.map { provider ->
            syncWalletBalanceForTokens(networkId, listOf(fromToken.currency, toToken.currency))
            val amountDecimal = toBigDecimalOrNull(amountToSwap)
            if (amountDecimal == null || amountDecimal.signum() == 0) {
                return providers.associateWith {
                    createEmptyAmountState(
                        networkId,
                        fromToken.currency,
                        toToken.currency,
                    )
                }
            }
            val amount = SwapAmount(amountDecimal, getTokenDecimals(fromToken.currency))
            val isBalanceWithoutFeeEnough = isBalanceEnough(networkId, fromToken.currency, amount, null)

            when (provider.type) {
                ExchangeProviderType.DEX -> {
                    manageDex(
                        networkId = networkId,
                        fromToken = fromToken,
                        toToken = toToken,
                        provider = provider,
                        selectedFee = selectedFee,
                        amount = amount,
                        isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                    )
                }
                ExchangeProviderType.CEX -> {
                    manageCex(
                        networkId = networkId,
                        fromToken = fromToken,
                        toToken = toToken,
                        provider = provider,
                        amount = amount,
                        isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                        selectedFee = selectedFee,
                    )
                }
            }
        }.toMap()
    }

    private suspend fun manageDex(
        networkId: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        provider: SwapProvider,
        selectedFee: FeeType,
        amount: SwapAmount,
        isBalanceWithoutFeeEnough: Boolean,
    ): Pair<SwapProvider, SwapState> {
        val quotes = repository.findBestQuote(
            fromContractAddress = fromToken.currency.getContractAddress(),
            fromNetwork = fromToken.currency.network.backendId,
            toContractAddress = toToken.currency.getContractAddress(),
            toNetwork = toToken.currency.network.backendId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
        )

        val fromTokenAddress = getTokenAddress(fromToken.currency)
        val isAllowedToSpend = quotes.dataModel?.allowanceContract?.let {
            isAllowedToSpend(networkId, fromToken.currency, amount, it)
        } ?: false

        if (isAllowedToSpend && allowPermissionsHandler.isAddressAllowanceInProgress(fromTokenAddress)) {
            allowPermissionsHandler.removeAddressFromProgress(fromTokenAddress)
            transactionManager.updateWalletManager(networkId, derivationPath)
        }
        return if (isAllowedToSpend && isBalanceWithoutFeeEnough) {
            provider to loadSwapData(
                provider = provider,
                networkId = networkId,
                fromToken = fromToken,
                toToken = toToken,
                amount = amount,
                selectedFee = selectedFee,
            )
        } else {
            provider to getQuotesState(
                exchangeProviderType = ExchangeProviderType.DEX,
                quoteDataModel = quotes,
                amount = amount,
                fromToken = fromToken,
                toToken = toToken,
                networkId = networkId,
                isAllowedToSpend = isAllowedToSpend,
                isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                providerType = provider.type,
                selectedFee = selectedFee,
            )
        }
    }

    private suspend fun manageCex(
        networkId: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        provider: SwapProvider,
        amount: SwapAmount,
        isBalanceWithoutFeeEnough: Boolean,
        selectedFee: FeeType,
    ): Pair<SwapProvider, SwapState> {
        return provider to loadQuoteData(
            exchangeProviderType = ExchangeProviderType.CEX,
            networkId = networkId,
            amount = amount,
            fromTokenStatus = fromToken,
            toTokenStatus = toToken,
            isAllowedToSpend = true,
            isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
            provider = provider,
            selectedFee = selectedFee,
        )
    }

    @Deprecated("used in old swap mechanism")
    override suspend fun onSwap(
        swapProvider: SwapProvider,
        networkId: String,
        swapData: SwapDataModel?,
        currencyToSend: CryptoCurrencyStatus,
        currencyToGet: CryptoCurrencyStatus,
        amountToSwap: String,
        fee: TxFee,
    ): TxState {
        return when (swapProvider.type) {
            ExchangeProviderType.CEX -> {
                val amountDecimal = toBigDecimalOrNull(amountToSwap)
                val amount = SwapAmount(requireNotNull(amountDecimal), getTokenDecimals(currencyToSend.currency))

                onSwapCex(
                    currencyToSend = currencyToSend,
                    currencyToGet = currencyToGet,
                    amount = amount,
                    fee = fee,
                    providerId = swapProvider.providerId,
                    userWalletId = requireNotNull(getSelectedWallet()).walletId
                )
            }
            ExchangeProviderType.DEX -> {
                onSwapDex(
                    networkId = networkId,
                    swapData = requireNotNull(swapData),
                    currencyToSend = currencyToSend.currency,
                    currencyToGet = currencyToGet.currency,
                    amountToSwap = amountToSwap,
                    fee = fee,
                )
            }
        }
    }

    override suspend fun updateQuotesStateWithSelectedFee(
        state: SwapState.QuotesLoadedState,
        selectedFee: FeeType,
        fromToken: CryptoCurrencyStatus,
        amountToSwap: String,
        networkId: String,
    ): SwapState.QuotesLoadedState {
        val amountDecimal = toBigDecimalOrNull(amountToSwap)
        if (amountDecimal == null || amountDecimal.signum() == 0) {
            return state
        }
        val amount = SwapAmount(amountDecimal, getTokenDecimals(fromToken.currency))
        val feeByPriority = selectFeeByType(feeType = selectedFee, txFeeState = state.txFee)
        val isBalanceIncludeFeeEnough =
            isBalanceEnough(networkId, fromToken.currency, amount, feeByPriority)
        val isFeeEnough = checkFeeIsEnough(
            fee = feeByPriority,
            spendAmount = amount,
            networkId = networkId,
            fromToken = fromToken.currency,
        )
        return state.copy(
            permissionState = PermissionDataState.Empty,
            preparedSwapConfigState = state.preparedSwapConfigState.copy(
                isBalanceEnough = isBalanceIncludeFeeEnough,
                isFeeEnough = isFeeEnough,
            ),
        )
    }

    private suspend fun onSwapDex(
        networkId: String,
        swapData: SwapDataModel,
        currencyToSend: CryptoCurrency,
        currencyToGet: CryptoCurrency,
        amountToSwap: String,
        fee: TxFee,
    ): TxState {
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amountToSwap)) { "wrong amount format" }
        val amount = SwapAmount(amountDecimal, getTokenDecimals(currencyToSend))
        val result = transactionManager.sendTransaction(
            txData = SwapTxData(
                networkId = networkId,
                amountToSend = amountDecimal,
                currencyToSend = swapCurrencyConverter.convert(currencyToSend),
                feeAmount = fee.feeValue,
                gasLimit = fee.gasLimit,
                destinationAddress = swapData.transaction.txTo,
                dataToSign = (swapData.transaction as ExpressTransactionModel.DEX).txData,
            ),
            isSwap = true,
            derivationPath = derivationPath,
            analyticsData = AnalyticsData(
                feeType = fee.feeType.getNameForAnalytics(),
                tokenSymbol = currencyToSend.symbol,
            ),
        )
        return when (result) {
            is SendTxResult.Success -> {
                TxState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(
                        amount,
                        currencyToSend.symbol,
                    ),
                    toAmount = amountFormatter.formatSwapAmountToUI(
                        swapData.toTokenAmount,
                        currencyToGet.symbol,
                    ),
                    txAddress = userWalletManager.getLastTransactionHash(networkId, derivationPath) ?: "",
                )
            }
            SendTxResult.UserCancelledError -> TxState.UserCancelled
            is SendTxResult.BlockchainSdkError -> TxState.BlockchainError
            is SendTxResult.TangemSdkError -> TxState.TangemSdkError
            is SendTxResult.NetworkError -> TxState.NetworkError
            is SendTxResult.UnknownError -> TxState.UnknownError
        }
    }

    private suspend fun onSwapCex(
        currencyToSend: CryptoCurrencyStatus,
        currencyToGet: CryptoCurrencyStatus,
        amount: SwapAmount,
        fee: TxFee,
        providerId: String,
        userWalletId: UserWalletId
    ): TxState {
        val exchangeData = repository.getExchangeData(
            fromContractAddress = currencyToSend.currency.getContractAddress(),
            fromNetwork = currencyToSend.currency.network.backendId,
            toContractAddress = currencyToGet.currency.getContractAddress(),
            toNetwork = currencyToGet.currency.network.backendId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            providerId = providerId,
            rateType = RateType.FLOAT,
            toAddress = currencyToGet.value.networkAddress?.defaultAddress ?: "",
        )

        val txData = walletManagersFacade.createTransaction(
            amount = amount.value.convertToAmount(currencyToSend.currency),
            fee = Fee.Common(fee.feeValue.convertToAmount(currencyToSend.currency)),
            memo = null,
            destination = (exchangeData.dataModel?.transaction as ExpressTransactionModel.CEX).txTo,
            userWalletId = userWalletId,
            network = currencyToSend.currency.network,
        )

        val result = sendTransactionUseCase(
            requireNotNull(txData),
            userWallet = requireNotNull(getSelectedWallet()),
            network = currencyToSend.currency.network
        )


        return result.fold(ifLeft = {
            when(it){
                is SendTransactionError.NetworkError -> TxState.NetworkError
                is SendTransactionError.DataError -> TxState.BlockchainError
                SendTransactionError.DemoCardError -> TxState.UnknownError
                else -> TxState.UnknownError
            }

        }, ifRight = {
            TxState.TxSent(
                fromAmount = amountFormatter.formatSwapAmountToUI(
                    amount,
                    currencyToSend.currency.symbol,
                ),
                toAmount = amountFormatter.formatSwapAmountToUI(
                    exchangeData.dataModel.toTokenAmount,
                    currencyToGet.currency.symbol,
                ),
                txAddress = userWalletManager.getLastTransactionHash(currencyToSend.currency.network.backendId, derivationPath) ?: "",
            )
            TxState.TxSent(
                txAddress = userWalletManager.getLastTransactionHash(
                    networkId = currencyToSend.currency.network.backendId,
                    derivationPath = derivationPath
                ) ?: ""
            )
        })
    }

    @Deprecated("used in old swap mechanism")
    override fun getTokenBalance(networkId: String, token: CryptoCurrency): SwapAmount {
        return cache.getBalanceForToken(
            networkId = networkId,
            derivationPath = derivationPath,
            symbol = token.symbol,
        ) ?: SwapAmount(BigDecimal.ZERO, getTokenDecimals(token))
    }

    @Deprecated("used in old swap mechanism")
    override fun isAvailableToSwap(networkId: String): Boolean {
        return ONE_INCH_SUPPORTED_NETWORKS.contains(networkId)
    }

    @Deprecated("used in old swap mechanism")
    private fun getTangemFee(): Double {
        return repository.getTangemFee()
    }

    private fun getTokenDecimals(token: CryptoCurrency): Int {
        return if (token is CryptoCurrency.Token) {
            token.decimals
        } else {
            transactionManager.getNativeTokenDecimals(token.network.backendId)
        }
    }

    private suspend fun isAllowedToSpend(
        networkId: String,
        fromToken: CryptoCurrency,
        amount: SwapAmount,
        spenderAddress: String,
    ): Boolean {
        if (fromToken is CryptoCurrency.Coin) return true
        return getSelectedWalletSyncUseCase().fold(
            ifRight = { userWallet ->
                val allowance = repository.getAllowance(
                    userWalletId = userWallet.walletId,
                    networkId = networkId,
                    derivationPath = derivationPath,
                    tokenDecimalCount = getTokenDecimals(fromToken),
                    tokenAddress = getTokenAddress(fromToken),
                    spenderAddress = spenderAddress,
                )
                allowance >= amount.value
            },
            ifLeft = {
                Timber.e("Swap Error on isAllowedToSpend")
                false
            },
        )
    }

    private fun createEmptyAmountState(
        networkId: String,
        fromToken: CryptoCurrency,
        toToken: CryptoCurrency,
    ): SwapState {
        val appCurrency = userWalletManager.getUserAppCurrency()
        val fromTokenBalance = cache.getBalanceForToken(networkId, derivationPath, fromToken.symbol)
        val toTokenBalance = cache.getBalanceForToken(networkId, derivationPath, toToken.symbol)
        return SwapState.EmptyAmountState(
            fromTokenWalletBalance = fromTokenBalance?.let { amountFormatter.formatSwapAmountToUI(it, "") }.orEmpty(),
            toTokenWalletBalance = toTokenBalance?.let { amountFormatter.formatSwapAmountToUI(it, "") }.orEmpty(),
            zeroAmountEquivalent = BigDecimal.ZERO.toFiatString(
                rateValue = BigDecimal.ONE,
                fiatCurrencyName = appCurrency.symbol,
                formatWithSpaces = true,
            ),
        )
    }

    /**
     * Load quote data calls only if spend is not allowed for token contract address
     */
    @Suppress("LongParameterList")
    private suspend fun loadQuoteData(
        exchangeProviderType: ExchangeProviderType,
        networkId: String,
        amount: SwapAmount,
        fromTokenStatus: CryptoCurrencyStatus,
        toTokenStatus: CryptoCurrencyStatus,
        provider: SwapProvider,
        isAllowedToSpend: Boolean,
        isBalanceWithoutFeeEnough: Boolean,
        selectedFee: FeeType,
    ): SwapState {
        val fromToken = fromTokenStatus.currency
        val toToken = toTokenStatus.currency
        return coroutineScope {
            val quotes = repository.findBestQuote(
                fromContractAddress = fromToken.getContractAddress(),
                fromNetwork = fromToken.network.backendId,
                toContractAddress = toToken.getContractAddress(),
                toNetwork = toToken.network.backendId,
                fromAmount = amount.toStringWithRightOffset(),
                fromDecimals = amount.decimals,
                providerId = provider.providerId,
                rateType = RateType.FLOAT,
            )

            getQuotesState(
                exchangeProviderType = exchangeProviderType,
                quoteDataModel = quotes,
                amount = amount,
                fromToken = fromTokenStatus,
                toToken = toTokenStatus,
                networkId = networkId,
                isAllowedToSpend = isAllowedToSpend,
                isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
                providerType = provider.type,
                selectedFee = selectedFee,
            )
        }
    }

    private suspend fun getQuotesState(
        exchangeProviderType: ExchangeProviderType,
        quoteDataModel: AggregatedSwapDataModel<QuoteModel>,
        amount: SwapAmount,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        networkId: String,
        isAllowedToSpend: Boolean,
        isBalanceWithoutFeeEnough: Boolean,
        providerType: ExchangeProviderType,
        selectedFee: FeeType,
    ): SwapState {
        val quoteModel = quoteDataModel.dataModel
        if (quoteModel != null) {
            val txFee = if (providerType == ExchangeProviderType.CEX) {
                getFeeForCex(amount, fromToken, networkId)
            } else {
                TxFeeState.Empty
            }
            val swapState = updateBalances(
                networkId = networkId,
                fromTokenStatus = fromToken,
                toTokenStatus = toToken,
                fromTokenAmount = amount,
                toTokenAmount = quoteModel.toTokenAmount,
                swapData = null,
                txFeeState = txFee,
            )

            return when (exchangeProviderType) {
                ExchangeProviderType.DEX -> {
                    val state = updatePermissionState(
                        networkId = networkId,
                        fromToken = fromToken.currency,
                        swapAmount = amount,
                        quotesLoadedState = swapState,
                        isAllowedToSpend = isAllowedToSpend,
                        spenderAddress = requireNotNull(quoteModel.allowanceContract) { "Allowance contract is null" },
                    )
                    state.copy(
                        preparedSwapConfigState = state.preparedSwapConfigState.copy(
                            isAllowedToSpend = isAllowedToSpend,
                            isBalanceEnough = isBalanceWithoutFeeEnough,
                        ),
                    )
                }
                ExchangeProviderType.CEX -> {
                    val feeByPriority = selectFeeByType(feeType = selectedFee, txFeeState = txFee)
                    val isFeeEnough = checkFeeIsEnough(
                        fee = feeByPriority,
                        spendAmount = amount,
                        networkId = networkId,
                        fromToken = fromToken.currency,
                    )
                    swapState.copy(
                        permissionState = PermissionDataState.Empty,
                        preparedSwapConfigState = PreparedSwapConfigState(
                            isFeeEnough = isFeeEnough,
                            isAllowedToSpend = isAllowedToSpend,
                            isBalanceEnough = isBalanceWithoutFeeEnough,
                        ),
                    )
                }
            }
        } else {
            return SwapState.SwapError(quoteDataModel.error)
        }
    }

    private suspend fun getFormattedFiatFees(networkId: String, vararg fees: BigDecimal): List<String> {
        val appCurrency = userWalletManager.getUserAppCurrency()
        val nativeToken = repository.getNativeTokenForNetwork(networkId)
        val rates = getQuotes(nativeToken.id)
        return rates[nativeToken.id]?.fiatRate?.let { rate ->
            fees.map { fee ->
                fee.toFiatString(rate, appCurrency.symbol, true)
            }
        }.orEmpty()
    }

    /**
     * Load swap data calls only if spend is allowed for token contract address
     */
    @Suppress("LongParameterList")
    private suspend fun loadSwapData(
        provider: SwapProvider,
        networkId: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        amount: SwapAmount,
        selectedFee: FeeType,
    ): SwapState {
        repository.getExchangeData(
            fromContractAddress = fromToken.currency.getContractAddress(),
            fromNetwork = fromToken.currency.network.backendId,
            toContractAddress = toToken.currency.getContractAddress(),
            toNetwork = toToken.currency.network.backendId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
            toAddress = toToken.value.networkAddress?.defaultAddress ?: "",
        ).let {
            val swapData = it.dataModel
            if (swapData != null) {
                val feeData = transactionManager.getFee(
                    networkId = networkId,
                    amountToSend = amount.value,
                    currencyToSend = swapCurrencyConverter.convert(fromToken.currency),
                    destinationAddress = swapData.transaction.txTo,
                    increaseBy = INCREASE_GAS_LIMIT_BY,
                    data = (swapData.transaction as ExpressTransactionModel.DEX).txData,
                    derivationPath = derivationPath,
                )
                val txFeeState = when (feeData) {
                    is ProxyFees.MultipleFees -> feeData.proxyFeesToFeeState(networkId)
                    is ProxyFees.SingleFee -> feeData.proxyFeesToFeeState(networkId)
                }
                val feeByPriority = selectFeeByType(feeType = selectedFee, txFeeState = txFeeState)
                val isBalanceIncludeFeeEnough =
                    isBalanceEnough(networkId, fromToken.currency, amount, feeByPriority)
                val isFeeEnough = checkFeeIsEnough(
                    fee = feeByPriority,
                    spendAmount = amount,
                    networkId = networkId,
                    fromToken = fromToken.currency,
                )
                val swapState = updateBalances(
                    networkId = networkId,
                    fromTokenStatus = fromToken,
                    toTokenStatus = toToken,
                    fromTokenAmount = amount,
                    toTokenAmount = swapData.toTokenAmount,
                    swapData = swapData,
                    txFeeState = txFeeState,
                )
                return swapState.copy(
                    permissionState = PermissionDataState.Empty,
                    preparedSwapConfigState = PreparedSwapConfigState(
                        isAllowedToSpend = true,
                        isBalanceEnough = isBalanceIncludeFeeEnough,
                        isFeeEnough = isFeeEnough,
                    ),
                )
            } else {
                return SwapState.SwapError(it.error)
            }
        }
    }

    @Suppress("LongParameterList")
    private suspend fun updateBalances(
        networkId: String,
        fromTokenStatus: CryptoCurrencyStatus,
        toTokenStatus: CryptoCurrencyStatus,
        fromTokenAmount: SwapAmount,
        toTokenAmount: SwapAmount,
        swapData: SwapDataModel?,
        txFeeState: TxFeeState,
    ): SwapState.QuotesLoadedState {
        val fromToken = fromTokenStatus.currency
        val toToken = toTokenStatus.currency
        val nativeToken = repository.getNativeTokenForNetwork(networkId)

        val rates = getQuotes(fromToken.id, toToken.id, nativeToken.id)
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = fromTokenAmount,
                cryptoCurrencyStatus = fromTokenStatus,
                amountFiat = rates[fromToken.id]?.fiatRate?.multiply(fromTokenAmount.value)
                    ?: BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = toTokenAmount,
                cryptoCurrencyStatus = toTokenStatus,
                amountFiat = rates[toToken.id]?.fiatRate?.multiply(toTokenAmount.value)
                    ?: BigDecimal.ZERO,
            ),
            priceImpact = calculatePriceImpact(
                fromTokenAmount = fromTokenAmount.value,
                fromRate = rates[fromToken.id]?.fiatRate?.toDouble() ?: 0.0,
                toTokenAmount = toTokenAmount.value,
                toRate = rates[toToken.id]?.fiatRate?.toDouble() ?: 0.0,
            ),
            networkCurrency = userWalletManager.getNetworkCurrency(networkId),
            swapDataModel = swapData,
            tangemFee = getTangemFee(),
            txFee = txFeeState,
        )
    }

    private suspend fun getFeeForCex(
        amount: SwapAmount,
        fromToken: CryptoCurrencyStatus,
        networkId: String,
    ): TxFeeState {
        getSelectedWalletSyncUseCase().getOrNull()?.walletId?.let { userWalletId ->
            val txFeeResult = getFeeUseCase(
                amount = amount.value,
                destination = fromToken.value.networkAddress?.defaultAddress ?: "",
                userWalletId = userWalletId,
                cryptoCurrency = fromToken.currency,
            ).firstOrNull()
            return txFeeResult?.fold(
                ifLeft = {
                    TxFeeState.Empty
                },
                ifRight = { txFee ->
                    txFee.toTxFeeState(networkId)
                },
            ) ?: TxFeeState.Empty
        }
        return TxFeeState.Empty
    }

    @Suppress("LongParameterList", "LongMethod")
    private suspend fun updatePermissionState(
        networkId: String,
        fromToken: CryptoCurrency,
        swapAmount: SwapAmount,
        quotesLoadedState: SwapState.QuotesLoadedState,
        spenderAddress: String,
        isAllowedToSpend: Boolean,
    ): SwapState.QuotesLoadedState {
        if (isAllowedToSpend) {
            return quotesLoadedState.copy(
                permissionState = PermissionDataState.Empty,
            )
        }
        // if token balance ZERO not show permission state to avoid user to spend money for fee
        val isTokenZeroBalance = getTokenBalance(networkId, fromToken).value.signum() == 0
        if (isTokenZeroBalance) {
            return quotesLoadedState.copy(
                permissionState = PermissionDataState.Empty,
            )
        }
        if (allowPermissionsHandler.isAddressAllowanceInProgress(getTokenAddress(fromToken))) {
            return quotesLoadedState.copy(
                permissionState = PermissionDataState.PermissionLoading,
            )
        }
        // setting up amount for approve with given amount for swap [SwapApproveType.Limited]
        val transactionData = getApproveData(
            networkId = networkId,
            derivationPath = derivationPath,
            fromToken = fromToken,
            swapAmount = swapAmount,
            spenderAddress = spenderAddress,
        )
        val feeData = try {
            transactionManager.getFee(
                networkId = networkId,
                amountToSend = BigDecimal.ZERO,
                currencyToSend = swapCurrencyConverter.convert(repository.getNativeTokenForNetwork(networkId)),
                destinationAddress = fromToken.getContractAddress(),
                increaseBy = INCREASE_GAS_LIMIT_BY,
                data = transactionData,
                derivationPath = derivationPath,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get fee")
            null
        }
        val feeState = feeData?.let {
            when (feeData) {
                is ProxyFees.MultipleFees -> feeData.proxyFeesToFeeState(networkId)
                is ProxyFees.SingleFee -> feeData.proxyFeesToFeeState(networkId)
            }
        } ?: TxFeeState.Empty
        val fee = when (feeState) {
            TxFeeState.Empty -> BigDecimal.ZERO
            is TxFeeState.MultipleFeeState -> feeState.normalFee.feeValue
            is TxFeeState.SingleFeeState -> feeState.fee.feeValue
        }
        val isFeeEnough = checkFeeIsEnough(
            fee = fee,
            spendAmount = SwapAmount.zeroSwapAmount(),
            networkId = networkId,
            fromToken = fromToken,
        )
        return quotesLoadedState.copy(
            permissionState = PermissionDataState.PermissionReadyForRequest(
                currency = fromToken.symbol,
                amount = INFINITY_SYMBOL,
                walletAddress = getWalletAddress(networkId),
                spenderAddress = getTokenAddress(fromToken),
                requestApproveData = RequestApproveStateData(
                    fee = feeState,
                    approveData = transactionData,
                    fromTokenAmount = swapAmount,
                    spenderAddress = spenderAddress,
                ),
            ),
            preparedSwapConfigState = quotesLoadedState.preparedSwapConfigState.copy(
                isFeeEnough = isFeeEnough,
            ),
        )
    }

    private suspend fun syncWalletBalanceForTokens(networkId: String, tokens: List<CryptoCurrency>) {
        val tokensToSync = tokens.filter { cache.getBalanceForToken(networkId, derivationPath, it.symbol) == null }
        if (tokensToSync.isNotEmpty()) {
            val tokensBalance =
                userWalletManager.getCurrentWalletTokensBalance(
                    networkId = networkId,
                    extraTokens = tokensToSync.map { swapCurrencyConverter.convert(it) },
                    derivationPath = derivationPath,
                )
            cache.cacheBalances(
                networkId = networkId,
                derivationPath = derivationPath,
                balances = tokensBalance.mapValues { SwapAmount(it.value.value, it.value.decimals) },
            )
        }
    }

    private suspend fun ProxyFees.MultipleFees.proxyFeesToFeeState(networkId: String): TxFeeState {
        val normalFeeValue = this.minFee.fee.value // in swap for normal use min fee
        val normalFeeGas = this.minFee.gasLimit.toInt()
        val priorityFeeValue = this.normalFee.fee.value // in swap for priority use normal fee
        val priorityFeeGas = this.normalFee.gasLimit.toInt()
        val feesFiat = getFormattedFiatFees(networkId, normalFeeValue, priorityFeeValue)
        val normalFiatFee = requireNotNull(feesFiat.getOrNull(0)) { "feesFiat item 0 couldn't be null" }
        val priorityFiatFee = requireNotNull(feesFiat.getOrNull(1)) { "feesFiat item 1 couldn't be null" }
        val networkCurrency = userWalletManager.getNetworkCurrency(networkId)
        val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
            amount = normalFeeValue,
            decimals = transactionManager.getNativeTokenDecimals(networkId),
        )
        val priorityCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
            amount = priorityFeeValue,
            decimals = transactionManager.getNativeTokenDecimals(networkId),
        )
        return TxFeeState.MultipleFeeState(
            normalFee = TxFee(
                feeValue = normalFeeValue,
                gasLimit = normalFeeGas,
                feeFiatFormatted = normalFiatFee,
                feeCryptoFormatted = normalCryptoFee,
                cryptoSymbol = networkCurrency,
                feeType = FeeType.NORMAL,
            ),
            priorityFee = TxFee(
                feeValue = priorityFeeValue,
                gasLimit = priorityFeeGas,
                feeFiatFormatted = priorityFiatFee,
                feeCryptoFormatted = priorityCryptoFee,
                cryptoSymbol = networkCurrency,
                feeType = FeeType.PRIORITY,
            ),
        )
    }

    private suspend fun ProxyFees.SingleFee.proxyFeesToFeeState(networkId: String): TxFeeState {
        val normalFeeValue = this.singleFee.fee.value
        val normalFeeGas = this.singleFee.gasLimit.toInt()
        val networkCurrency = userWalletManager.getNetworkCurrency(networkId)
        val feesFiat = getFormattedFiatFees(networkId, normalFeeValue)
        val normalFiatFee = requireNotNull(feesFiat.getOrNull(0)) { "feesFiat item 0 couldn't be null" }
        val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
            amount = normalFeeValue,
            decimals = transactionManager.getNativeTokenDecimals(networkId),
        )
        return TxFeeState.SingleFeeState(
            fee = TxFee(
                feeValue = normalFeeValue,
                gasLimit = normalFeeGas,
                feeFiatFormatted = normalFiatFee,
                feeCryptoFormatted = normalCryptoFee,
                cryptoSymbol = networkCurrency,
                feeType = FeeType.NORMAL,
            ),
        )
    }

    private suspend fun TransactionFee.toTxFeeState(networkId: String): TxFeeState {
        val networkCurrency = userWalletManager.getNetworkCurrency(networkId)
        return when (this) {
            is TransactionFee.Choosable -> {
                val feeNormal = this.normal.amount.value ?: BigDecimal.ZERO
                val feePriority = this.priority.amount.value ?: BigDecimal.ZERO
                val normalFiatValue = getFormattedFiatFees(networkId, feeNormal)[0]
                val priorityFiatValue = getFormattedFiatFees(networkId, feePriority)[0]
                val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = feeNormal,
                    decimals = transactionManager.getNativeTokenDecimals(networkId),
                )
                val priorityCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = feePriority,
                    decimals = transactionManager.getNativeTokenDecimals(networkId),
                )
                TxFeeState.MultipleFeeState(
                    normalFee = TxFee(
                        feeValue = feeNormal,
                        gasLimit = this.normal.getGasLimit(),
                        feeFiatFormatted = normalFiatValue,
                        feeCryptoFormatted = normalCryptoFee,
                        cryptoSymbol = networkCurrency,
                        feeType = FeeType.NORMAL,
                    ),
                    priorityFee = TxFee(
                        feeValue = feePriority,
                        gasLimit = this.priority.getGasLimit(),
                        feeFiatFormatted = priorityFiatValue,
                        feeCryptoFormatted = priorityCryptoFee,
                        cryptoSymbol = networkCurrency,
                        feeType = FeeType.PRIORITY,
                    ),
                )
            }
            is TransactionFee.Single -> {
                val feeNormal = this.normal.amount.value ?: BigDecimal.ZERO
                val normalFiatValue = getFormattedFiatFees(networkId, feeNormal)[0]
                val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = feeNormal,
                    decimals = transactionManager.getNativeTokenDecimals(networkId),
                )
                TxFeeState.SingleFeeState(
                    fee = TxFee(
                        feeValue = this.normal.amount.value ?: BigDecimal.ZERO,
                        gasLimit = this.normal.getGasLimit(),
                        feeFiatFormatted = normalFiatValue,
                        feeCryptoFormatted = normalCryptoFee,
                        cryptoSymbol = networkCurrency,
                        feeType = FeeType.NORMAL,
                    ),
                )
            }
        }
    }

    private fun Fee.getGasLimit(): Int {
        return when (this) {
            is Fee.Common -> 0
            is Fee.Ethereum -> this.gasLimit.toInt()
        }
    }

    private fun selectFeeByType(feeType: FeeType, txFeeState: TxFeeState): BigDecimal {
        return when (txFeeState) {
            TxFeeState.Empty -> BigDecimal.ZERO
            is TxFeeState.SingleFeeState -> txFeeState.fee.feeValue
            is TxFeeState.MultipleFeeState -> when (feeType) {
                FeeType.NORMAL -> txFeeState.normalFee.feeValue
                FeeType.PRIORITY -> txFeeState.priorityFee.feeValue
            }
        }
    }

    private fun isBalanceEnough(
        networkId: String,
        fromToken: CryptoCurrency,
        amount: SwapAmount,
        fee: BigDecimal?,
    ): Boolean {
        val tokenBalance = getTokenBalance(networkId, fromToken).value
        return if (fromToken is CryptoCurrency.Token) {
            tokenBalance >= amount.value
        } else {
            tokenBalance > amount.value.plus(fee ?: BigDecimal.ZERO)
        }
    }

    private suspend fun getWalletAddress(networkId: String): String {
        return userWalletManager.getWalletAddress(networkId, derivationPath)
    }

    private fun getTokenAddress(currency: CryptoCurrency): String {
        return when (currency) {
            is CryptoCurrency.Coin -> {
                "0"
            }
            is CryptoCurrency.Token -> {
                currency.contractAddress
            }
        }
    }

    private suspend fun checkFeeIsEnough(
        fee: BigDecimal?,
        spendAmount: SwapAmount,
        networkId: String,
        fromToken: CryptoCurrency,
    ): Boolean {
        if (fee == null) {
            return false
        }
        val nativeTokenBalance = userWalletManager.getNativeTokenBalance(networkId, derivationPath)
        val percentsToFeeIncrease = BigDecimal.ONE
        return when (fromToken) {
            is CryptoCurrency.Coin -> {
                nativeTokenBalance?.let { balance ->
                    return balance.value.minus(spendAmount.value) > fee.multiply(percentsToFeeIncrease)
                } ?: false
            }
            is CryptoCurrency.Token -> {
                nativeTokenBalance?.let { balance ->
                    return balance.value > fee.multiply(percentsToFeeIncrease)
                } ?: false
            }
        }
    }

    private fun toBigDecimalOrNull(amountToSwap: String): BigDecimal? {
        return amountToSwap.replace(",", ".").toBigDecimalOrNull()
    }

    private fun calculatePriceImpact(
        fromTokenAmount: BigDecimal,
        fromRate: Double,
        toTokenAmount: BigDecimal,
        toRate: Double,
    ): Float {
        val toTokenFiatValue = toTokenAmount.multiply(toRate.toBigDecimal())
        val fromTokenFiatValue = fromTokenAmount.multiply(fromRate.toBigDecimal())
        return (BigDecimal.ONE - toTokenFiatValue.divide(fromTokenFiatValue, 2, RoundingMode.HALF_UP)).toFloat()
    }

    private suspend fun getApproveData(
        networkId: String,
        derivationPath: String?,
        fromToken: CryptoCurrency,
        swapAmount: SwapAmount? = null,
        spenderAddress: String,
    ): String {
        return getSelectedWalletSyncUseCase().fold(
            ifRight = { userWallet ->
                repository.getApproveData(
                    userWalletId = userWallet.walletId,
                    networkId = networkId,
                    derivationPath = derivationPath,
                    currency = fromToken,
                    amount = swapAmount?.value,
                    spenderAddress = spenderAddress,
                )
            },
            ifLeft = {
                Timber.e("Swap Error on getApproveData")
                error("Swap Error on getApproveData")
            },
        )
    }

    private suspend fun getQuotes(vararg ids: CryptoCurrency.ID): Map<CryptoCurrency.ID, Quote> {
        val set = quotesRepository.getQuotesSync(ids.toSet(), false)

        return ids
            .mapNotNull { id -> set.find { it.rawCurrencyId == id.rawCurrencyId }?.let { id to it } }
            .toMap()
    }

    companion object {
        @Suppress("UnusedPrivateMember")
        private const val INCREASE_GAS_LIMIT_BY = 112 // 12%
        private const val INFINITY_SYMBOL = "∞"

        private val ONE_INCH_SUPPORTED_NETWORKS = listOf(
            "ethereum",
            "binance-smart-chain",
            "polygon-pos",
            "optimistic-ethereum",
            "arbitrum-one",
            "xdai",
            "avalanche",
            "fantom",
        )
    }
}