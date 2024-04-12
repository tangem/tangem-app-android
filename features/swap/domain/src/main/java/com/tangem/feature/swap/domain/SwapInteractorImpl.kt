package com.tangem.feature.swap.domain

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.extenstions.unwrap
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.common.extensions.minimalAmount
import com.tangem.domain.tokens.GetCryptoCurrencyStatusesSyncUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.utils.convertToAmount
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.converters.SwapCurrencyConverter
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.toStringWithRightOffset
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.*
import com.tangem.lib.crypto.models.transactions.SendTxResult
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import com.tangem.utils.toFiatString
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import javax.inject.Inject

@Suppress("LargeClass", "LongParameterList")
internal class SwapInteractorImpl @Inject constructor(
    private val transactionManager: TransactionManager,
    private val userWalletManager: UserWalletManager,
    private val repository: SwapRepository,
    private val allowPermissionsHandler: AllowPermissionsHandler,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getMultiCryptoCurrencyStatusUseCase: GetCryptoCurrencyStatusesSyncUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val quotesRepository: QuotesRepository,
    private val dispatcher: CoroutineDispatcherProvider,
    private val swapTransactionRepository: SwapTransactionRepository,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val appCurrencyRepository: AppCurrencyRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val initialToCurrencyResolver: InitialToCurrencyResolver,
) : SwapInteractor {

    private val estimateFeeUseCase by lazy(LazyThreadSafetyMode.NONE) {
        EstimateFeeUseCase(walletManagersFacade, dispatcher)
    }

    private val getSelectedAppCurrencyUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetSelectedAppCurrencyUseCase(appCurrencyRepository)
    }

    private val swapCurrencyConverter = SwapCurrencyConverter()
    private val amountFormatter = AmountFormatter()
    private val hundredPercent = BigInteger("100")

    override suspend fun getTokensDataState(currency: CryptoCurrency): TokensDataStateExpress {
        val selectedWallet = getSelectedWalletSyncUseCase().fold(
            ifLeft = { null },
            ifRight = { it },
        )

        requireNotNull(selectedWallet) { "No selected wallet" }

        val walletCurrencyStatuses = getMultiCryptoCurrencyStatusUseCase(selectedWallet.walletId)
            .getOrElse { emptyList() }

        val walletCurrencyStatusesExceptInitial = walletCurrencyStatuses
            .filter {
                val currencyFilter = it.currency.network.backendId != currency.network.backendId ||
                    it.currency.getContractAddress() != currency.getContractAddress()
                val statusFilter = it.value is CryptoCurrencyStatus.Loaded || it.value is CryptoCurrencyStatus.NoAccount
                val notCustomTokenFilter = !it.currency.isCustom
                statusFilter && currencyFilter && notCustomTokenFilter
            }

        if (walletCurrencyStatusesExceptInitial.isEmpty()) {
            return TokensDataStateExpress.EMPTY
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
                leastPairs = pairsLeast.pairs,
                cryptoCurrenciesList = walletCurrencyStatusesExceptInitial,
                tokenInfoForFilter = { it.to },
                tokenInfoForAvailable = { it.from },
            ),
            toGroup = getToCurrenciesGroup(
                currency = currency,
                leastPairs = pairsLeast.pairs,
                cryptoCurrenciesList = walletCurrencyStatusesExceptInitial,
                tokenInfoForFilter = { it.from },
                tokenInfoForAvailable = { it.to },
            ),
            allProviders = pairsLeast.allProviders,
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

        val availableCryptoCurrencies = cryptoCurrenciesList.mapNotNull { cryptoCurrencyStatus ->
            val providers = findProvidersForPair(cryptoCurrencyStatus, filteredPairs, tokenInfoForAvailable)
            if (providers != null) {
                CryptoCurrencySwapInfo(cryptoCurrencyStatus, providers)
            } else {
                null
            }
        }

        val unavailableCryptoCurrencies = cryptoCurrenciesList - availableCryptoCurrencies
            .map { it.currencyStatus }
            .toSet()

        return CurrenciesGroup(
            available = availableCryptoCurrencies,
            unavailable = unavailableCryptoCurrencies.map { CryptoCurrencySwapInfo(it, emptyList()) },
            afterSearch = false,
        )
    }

    private fun findProvidersForPair(
        cryptoCurrencyStatuses: CryptoCurrencyStatus,
        swapPairsLeastList: List<SwapPairLeast>,
        tokenInfoForAvailable: (SwapPairLeast) -> LeastTokenInfo,
    ): List<SwapProvider>? {
        return swapPairsLeastList.firstNotNullOfOrNull {
            val listTokenInfo = tokenInfoForAvailable(it)
            if (cryptoCurrencyStatuses.currency.network.backendId == listTokenInfo.network &&
                cryptoCurrencyStatuses.currency.getContractAddress() == listTokenInfo.contractAddress
            ) {
                it.providers
            } else {
                null
            }
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
    ): PairsWithProviders {
        return repository.getPairs(initialCurrency, currenciesList)
    }

    override suspend fun givePermissionToSwap(
        networkId: String,
        permissionOptions: PermissionOptions,
    ): SwapTransactionState {
        val derivationPath = permissionOptions.fromToken.network.derivationPath.value
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
                SwapTransactionState.TxSent(
                    txHash = userWalletManager.getLastTransactionHash(networkId, derivationPath).orEmpty(),
                    timestamp = System.currentTimeMillis(),
                )
            }
            SendTxResult.UserCancelledError -> SwapTransactionState.UserCancelled
            is SendTxResult.BlockchainSdkError -> SwapTransactionState.BlockchainError
            is SendTxResult.TangemSdkError -> SwapTransactionState.TangemSdkError
            is SendTxResult.NetworkError -> SwapTransactionState.NetworkError
            is SendTxResult.UnknownError -> SwapTransactionState.UnknownError
        }
    }

    override suspend fun findBestQuote(
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        providers: List<SwapProvider>,
        amountToSwap: String,
        selectedFee: FeeType,
    ): Map<SwapProvider, SwapState> {
        return providers.map { provider ->
            val amountDecimal = toBigDecimalOrNull(amountToSwap)
            if (amountDecimal == null || amountDecimal.signum() == 0) {
                return providers.associateWith { createEmptyAmountState() }
            }
            val amount = SwapAmount(amountDecimal, fromToken.currency.decimals)
            val isBalanceWithoutFeeEnough = isBalanceEnough(fromToken, amount, null)
            val networkId = fromToken.currency.network.backendId
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
            toDecimals = toToken.currency.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
        )

        val fromTokenAddress = getTokenAddress(fromToken.currency)
        val isAllowedToSpend = quotes.fold(
            ifRight = {
                it.allowanceContract?.let {
                    isAllowedToSpend(networkId, fromToken.currency, amount, it)
                } ?: true
            },
            ifLeft = { false },
        )

        if (isAllowedToSpend && allowPermissionsHandler.isAddressAllowanceInProgress(fromTokenAddress)) {
            allowPermissionsHandler.removeAddressFromProgress(fromTokenAddress)
            transactionManager.updateWalletManager(
                networkId,
                fromToken.currency.network.derivationPath.value,
            )
        }
        return if (isAllowedToSpend && isBalanceWithoutFeeEnough) {
            provider to loadDexSwapData(
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
                txFee = TxFeeState.Empty,
                includeFeeInAmount = IncludeFeeInAmount.Excluded, // exclude for dex
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
    ): Pair<SwapProvider, SwapState> {
        return provider to loadCexQuoteData(
            exchangeProviderType = ExchangeProviderType.CEX,
            networkId = networkId,
            amount = amount,
            fromTokenStatus = fromToken,
            toTokenStatus = toToken,
            isAllowedToSpend = true,
            isBalanceWithoutFeeEnough = isBalanceWithoutFeeEnough,
            provider = provider,
        )
    }

    private suspend fun manageWarnings(
        fromTokenStatus: CryptoCurrencyStatus,
        amount: SwapAmount,
        feeState: TxFeeState,
    ): List<Warning> {
        val fromToken = fromTokenStatus.currency
        val userWalletId = getSelectedWallet()?.walletId ?: return emptyList()
        val warnings = mutableListOf<Warning>()
        manageExistentialDepositWarning(warnings, userWalletId, amount, fromToken)
        manageDustWarning(warnings, feeState, userWalletId, fromTokenStatus, amount)
        manageReduceAmountWarning(warnings, fromTokenStatus, amount)
        return warnings
    }

    private suspend fun manageExistentialDepositWarning(
        warnings: MutableList<Warning>,
        userWalletId: UserWalletId,
        amount: SwapAmount,
        fromToken: CryptoCurrency,
    ) {
        val existentialDeposit = currencyChecksRepository.getExistentialDeposit(userWalletId, fromToken.network)
        if (existentialDeposit != null) {
            val nativeBalance = userWalletManager.getNativeTokenBalance(
                fromToken.network.backendId,
                fromToken.network.derivationPath.value,
            ) ?: ProxyAmount.empty()
            if (nativeBalance.value.minus(amount.value) < existentialDeposit) {
                warnings.add(Warning.ExistentialDepositWarning(existentialDeposit))
            }
        }
    }

    private suspend fun manageDustWarning(
        warnings: MutableList<Warning>,
        feeState: TxFeeState,
        userWalletId: UserWalletId,
        fromTokenStatus: CryptoCurrencyStatus,
        amount: SwapAmount,
    ) {
        val fee = when (feeState) {
            TxFeeState.Empty -> BigDecimal.ZERO
            is TxFeeState.MultipleFeeState -> feeState.priorityFee.feeValue
            is TxFeeState.SingleFeeState -> feeState.fee.feeValue
        }
        val dust = currencyChecksRepository.getDustValue(userWalletId, fromTokenStatus.currency.network)
        val balance = fromTokenStatus.value.amount ?: BigDecimal.ZERO
        if (dust != null &&
            !balance.isNullOrZero() &&
            amount.value < balance
        ) {
            val change = balance - (amount.value + fee)
            val isChangeLowerThanDust = change < dust && change != BigDecimal.ZERO
            val isShowWarning = amount.value + fee < dust || isChangeLowerThanDust
            if (isShowWarning) {
                warnings.add(Warning.MinAmountWarning(dust))
            }
        }
    }

    private fun manageReduceAmountWarning(
        warnings: MutableList<Warning>,
        fromTokenStatus: CryptoCurrencyStatus,
        amount: SwapAmount,
    ) {
        val isTezos = fromTokenStatus.currency.network.id.value == Blockchain.Tezos.id
        if (isTezos && amount.value == fromTokenStatus.value.amount) {
            warnings.add(Warning.ReduceAmountWarning(Blockchain.Tezos.minimalAmount()))
        }
    }

    override suspend fun onSwap(
        swapProvider: SwapProvider,
        swapData: SwapDataModel?,
        currencyToSend: CryptoCurrencyStatus,
        currencyToGet: CryptoCurrencyStatus,
        amountToSwap: String,
        includeFeeInAmount: IncludeFeeInAmount,
        fee: TxFee,
    ): SwapTransactionState {
        return when (swapProvider.type) {
            ExchangeProviderType.CEX -> {
                val amountDecimal = toBigDecimalOrNull(amountToSwap)
                val amount = SwapAmount(requireNotNull(amountDecimal), currencyToSend.currency.decimals)
                val amountToSwapWithFee = if (includeFeeInAmount is IncludeFeeInAmount.Included) {
                    includeFeeInAmount.amountSubtractFee
                } else {
                    amount
                }
                onSwapCex(
                    currencyToSend = currencyToSend,
                    currencyToGet = currencyToGet,
                    amount = amountToSwapWithFee,
                    txFee = fee,
                    swapProvider = swapProvider,
                    userWalletId = requireNotNull(getSelectedWallet()).walletId,
                )
            }
            ExchangeProviderType.DEX -> {
                onSwapDex(
                    networkId = currencyToSend.currency.network.backendId,
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
    ): SwapState.QuotesLoadedState {
        val amountDecimal = toBigDecimalOrNull(amountToSwap)
        if (amountDecimal == null || amountDecimal.signum() == 0) {
            return state
        }
        val amount = SwapAmount(amountDecimal, fromToken.currency.decimals)
        val includeFeeInAmount = getIncludeFeeInAmount(
            networkId = fromToken.currency.network.backendId,
            txFee = state.txFee,
            amount = amount,
            fromToken = fromToken.currency,
        )
        val fee = when (val txFee = state.txFee) {
            TxFeeState.Empty -> BigDecimal.ZERO
            is TxFeeState.MultipleFeeState -> txFee.priorityFee.feeValue
            is TxFeeState.SingleFeeState -> txFee.fee.feeValue
        }
        val feeState = getFeeState(
            fee = fee,
            spendAmount = amount,
            networkId = fromToken.currency.network.backendId,
            fromTokenStatus = fromToken,
        )
        return state.copy(
            permissionState = PermissionDataState.Empty,
            preparedSwapConfigState = state.preparedSwapConfigState.copy(
                feeState = feeState,
                isBalanceEnough = includeFeeInAmount !is IncludeFeeInAmount.BalanceNotEnough,
                includeFeeInAmount = includeFeeInAmount,
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
    ): SwapTransactionState {
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amountToSwap)) { "wrong amount format" }
        val amount = SwapAmount(amountDecimal, currencyToSend.decimals)
        val derivationPath = currencyToSend.network.derivationPath.value
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
                storeLastCryptoCurrencyId(currencyToGet)
                SwapTransactionState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(
                        amount,
                        currencyToSend.symbol,
                    ),
                    fromAmountValue = amount.value,
                    toAmount = amountFormatter.formatSwapAmountToUI(
                        swapData.toTokenAmount,
                        currencyToGet.symbol,
                    ),
                    toAmountValue = swapData.toTokenAmount.value,
                    txHash = userWalletManager.getLastTransactionHash(networkId, derivationPath).orEmpty(),
                    timestamp = System.currentTimeMillis(),
                )
            }
            SendTxResult.UserCancelledError -> SwapTransactionState.UserCancelled
            is SendTxResult.BlockchainSdkError -> SwapTransactionState.BlockchainError
            is SendTxResult.TangemSdkError -> SwapTransactionState.TangemSdkError
            is SendTxResult.NetworkError -> SwapTransactionState.NetworkError
            is SendTxResult.UnknownError -> SwapTransactionState.UnknownError
        }
    }

    @Suppress("LongMethod")
    private suspend fun onSwapCex(
        currencyToSend: CryptoCurrencyStatus,
        currencyToGet: CryptoCurrencyStatus,
        amount: SwapAmount,
        txFee: TxFee,
        swapProvider: SwapProvider,
        userWalletId: UserWalletId,
    ): SwapTransactionState {
        val exchangeData = repository.getExchangeData(
            fromContractAddress = currencyToSend.currency.getContractAddress(),
            fromNetwork = currencyToSend.currency.network.backendId,
            toContractAddress = currencyToGet.currency.getContractAddress(),
            toNetwork = currencyToGet.currency.network.backendId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = currencyToGet.currency.decimals,
            providerId = swapProvider.providerId,
            rateType = RateType.FLOAT,
            toAddress = currencyToGet.value.networkAddress?.defaultAddress?.value ?: "",
            refundAddress = currencyToSend.value.networkAddress?.defaultAddress?.value,
            refundExtraId = null, // currently always null
        ).getOrElse { return SwapTransactionState.ExpressError(it) }

        val exchangeDataCex =
            exchangeData.transaction as? ExpressTransactionModel.CEX ?: return SwapTransactionState.UnknownError

        val txExtras = transactionManager.getMemoExtras(
            currencyToSend.currency.network.backendId,
            exchangeDataCex.txExtraId,
        )
        if (txExtras == null && exchangeDataCex.txExtraId != null) {
            return SwapTransactionState.UnknownError
        }
        val txData = walletManagersFacade.createTransaction(
            amount = amount.value.convertToAmount(currencyToSend.currency),
            fee = getFeeForTransaction(
                fee = txFee,
                blockchain = Blockchain.fromId(currencyToSend.currency.network.id.value),
            ),
            memo = null,
            destination = exchangeDataCex.txTo,
            userWalletId = userWalletId,
            network = currencyToSend.currency.network,
        )?.copy(
            extras = txExtras,
        )

        val result = sendTransactionUseCase(
            requireNotNull(txData),
            userWallet = requireNotNull(getSelectedWallet()),
            network = currencyToSend.currency.network,
        )

        val txCexModel = exchangeData.transaction as? ExpressTransactionModel.CEX

        val derivationPath = currencyToSend.currency.network.derivationPath.value
        return result.fold(
            ifLeft = {
                when (it) {
                    SendTransactionError.UserCancelledError -> SwapTransactionState.UserCancelled
                    is SendTransactionError.BlockchainSdkError -> SwapTransactionState.BlockchainError
                    is SendTransactionError.TangemSdkError -> SwapTransactionState.TangemSdkError
                    is SendTransactionError.NetworkError -> SwapTransactionState.NetworkError
                    else -> SwapTransactionState.UnknownError
                }
            },
            ifRight = {
                val timestamp = System.currentTimeMillis()
                val txExternalUrl = txCexModel?.externalTxUrl
                storeSwapTransaction(
                    currencyToSend = currencyToSend,
                    currencyToGet = currencyToGet,
                    amount = amount,
                    swapProvider = swapProvider,
                    swapDataModel = exchangeData,
                    timestamp = timestamp,
                    txExternalUrl = txExternalUrl.orEmpty(),
                    txExternalId = txCexModel?.externalTxId.orEmpty(),
                )
                storeLastCryptoCurrencyId(currencyToGet.currency)
                SwapTransactionState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(
                        amount,
                        currencyToSend.currency.symbol,
                    ),
                    fromAmountValue = amount.value,
                    toAmount = amountFormatter.formatSwapAmountToUI(
                        exchangeData.toTokenAmount,
                        currencyToGet.currency.symbol,
                    ),
                    toAmountValue = exchangeData.toTokenAmount.value,
                    txHash = userWalletManager.getLastTransactionHash(
                        currencyToSend.currency.network.backendId,
                        derivationPath,
                    ).orEmpty(),
                    txExternalUrl = txExternalUrl,
                    timestamp = timestamp,
                )
            },
        )
    }

    private fun getFeeForTransaction(fee: TxFee, blockchain: Blockchain): Fee {
        val feeAmountValue = fee.feeValue
        val feeAmount = Amount(
            value = fee.feeValue,
            currencySymbol = fee.cryptoSymbol,
            decimals = fee.decimals,
            type = AmountType.Coin,
        )

        return when {
            blockchain.isEvm() -> {
                val feeAmountWithDecimals = feeAmountValue.movePointRight(fee.decimals)
                Fee.Ethereum(
                    amount = feeAmount,
                    gasLimit = fee.gasLimit.toBigInteger(),
                    gasPrice = (feeAmountWithDecimals / fee.gasLimit.toBigDecimal()).toBigInteger(),
                )
            }
            blockchain == Blockchain.VeChain -> Fee.VeChain(
                amount = feeAmount,
                gasPriceCoef = Fee.VeChain.getGasPriceCoef(fee.gasLimit.toLong(), fee.feeValue),
                gasLimit = fee.gasLimit.toLong(),
            )
            blockchain == Blockchain.Aptos -> {
                Fee.Aptos(
                    amount = feeAmount,
                    gasUnitPrice = fee.feeValue.divide(BigDecimal(fee.gasLimit))
                        .movePointRight(Blockchain.Aptos.decimals())
                        .toLong(),
                    gasLimit = fee.gasLimit.toLong(),
                )
            }
            else -> Fee.Common(feeAmount)
        }
    }

    private suspend fun storeSwapTransaction(
        currencyToSend: CryptoCurrencyStatus,
        currencyToGet: CryptoCurrencyStatus,
        amount: SwapAmount,
        swapProvider: SwapProvider,
        swapDataModel: SwapDataModel,
        timestamp: Long,
        txExternalUrl: String,
        txExternalId: String,
    ) {
        swapTransactionRepository.storeTransaction(
            userWalletId = UserWalletId(userWalletManager.getWalletId()),
            fromCryptoCurrency = currencyToSend.currency,
            toCryptoCurrency = currencyToGet.currency,
            transaction = SavedSwapTransactionModel(
                txId = swapDataModel.transaction.txId,
                provider = swapProvider,
                timestamp = timestamp,
                fromCryptoAmount = amount.value,
                toCryptoAmount = swapDataModel.toTokenAmount.value,
                status = ExchangeStatusModel(
                    providerId = swapProvider.providerId,
                    status = ExchangeStatus.New,
                    txId = swapDataModel.transaction.txId,
                    txExternalUrl = txExternalUrl,
                    txExternalId = txExternalId,
                ),
            ),
        )
    }

    private suspend fun storeLastCryptoCurrencyId(cryptoCurrency: CryptoCurrency) {
        swapTransactionRepository.storeLastSwappedCryptoCurrencyId(
            UserWalletId(userWalletManager.getWalletId()),
            cryptoCurrency.id,
        )
    }

    override fun getTokenBalance(token: CryptoCurrencyStatus): SwapAmount {
        return SwapAmount(token.value.amount ?: BigDecimal.ZERO, token.currency.decimals)
    }

    override suspend fun selectInitialCurrencyToSwap(
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
    ): CryptoCurrencyStatus? {
        return initialToCurrencyResolver.tryGetFromCache(initialCryptoCurrency, state)
            ?: initialToCurrencyResolver.tryGetWithMaxAmount(state)
            ?: state.toGroup.available.firstOrNull()?.currencyStatus
    }

    override fun getNativeToken(networkId: String): CryptoCurrency {
        return repository.getNativeTokenForNetwork(networkId)
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
                    derivationPath = fromToken.network.derivationPath.value,
                    tokenDecimalCount = fromToken.decimals,
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

    private suspend fun createEmptyAmountState(): SwapState {
        val appCurrency = getSelectedAppCurrencyUseCase.unwrap()
        return SwapState.EmptyAmountState(
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
    private suspend fun loadCexQuoteData(
        exchangeProviderType: ExchangeProviderType,
        networkId: String,
        amount: SwapAmount,
        fromTokenStatus: CryptoCurrencyStatus,
        toTokenStatus: CryptoCurrencyStatus,
        provider: SwapProvider,
        isAllowedToSpend: Boolean,
        isBalanceWithoutFeeEnough: Boolean,
    ): SwapState {
        val fromToken = fromTokenStatus.currency
        val toToken = toTokenStatus.currency
        return coroutineScope {
            val txFee = if (provider.type == ExchangeProviderType.CEX) {
                getFeeForCex(amount, fromTokenStatus)
            } else {
                TxFeeState.Empty
            }

            val includeFeeInAmount = getIncludeFeeInAmount(
                networkId = networkId,
                txFee = txFee,
                amount = amount,
                fromToken = fromToken,
            )
            val amountToRequest = if (includeFeeInAmount is IncludeFeeInAmount.Included) {
                includeFeeInAmount.amountSubtractFee
            } else {
                amount
            }

            val quotes = repository.findBestQuote(
                fromContractAddress = fromToken.getContractAddress(),
                fromNetwork = fromToken.network.backendId,
                toContractAddress = toToken.getContractAddress(),
                toNetwork = toToken.network.backendId,
                fromAmount = amountToRequest.toStringWithRightOffset(),
                fromDecimals = amount.decimals,
                providerId = provider.providerId,
                toDecimals = toToken.decimals,
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
                txFee = txFee,
                includeFeeInAmount = includeFeeInAmount,
            )
        }
    }

    private suspend fun getQuotesState(
        exchangeProviderType: ExchangeProviderType,
        quoteDataModel: Either<DataError, QuoteModel>,
        amount: SwapAmount,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        networkId: String,
        isAllowedToSpend: Boolean,
        isBalanceWithoutFeeEnough: Boolean,
        txFee: TxFeeState,
        includeFeeInAmount: IncludeFeeInAmount,
    ): SwapState {
        return quoteDataModel.fold(
            ifRight = { quoteModel ->
                val swapState = updateBalances(
                    networkId = networkId,
                    fromTokenStatus = fromToken,
                    toTokenStatus = toToken,
                    fromTokenAmount = amount,
                    toTokenAmount = quoteModel.toTokenAmount,
                    swapData = null,
                    txFeeState = txFee,
                ).copy(
                    warnings = manageWarnings(fromToken, amount, txFee),
                )

                when (exchangeProviderType) {
                    ExchangeProviderType.DEX -> {
                        val state = updatePermissionState(
                            networkId = networkId,
                            fromTokenStatus = fromToken,
                            swapAmount = amount,
                            quotesLoadedState = swapState,
                            isAllowedToSpend = isAllowedToSpend,
                            spenderAddress = quoteModel.allowanceContract,
                        )
                        state.copy(
                            preparedSwapConfigState = state.preparedSwapConfigState.copy(
                                isAllowedToSpend = isAllowedToSpend,
                                isBalanceEnough = isBalanceWithoutFeeEnough,
                            ),
                        )
                    }
                    ExchangeProviderType.CEX -> {
                        val fee = when (txFee) {
                            TxFeeState.Empty -> BigDecimal.ZERO
                            is TxFeeState.MultipleFeeState -> txFee.priorityFee.feeValue
                            is TxFeeState.SingleFeeState -> txFee.fee.feeValue
                        }
                        val feeState = getFeeState(
                            fee = fee,
                            spendAmount = amount,
                            networkId = networkId,
                            fromTokenStatus = fromToken,
                        )
                        swapState.copy(
                            permissionState = PermissionDataState.Empty,
                            preparedSwapConfigState = PreparedSwapConfigState(
                                feeState = feeState,
                                isAllowedToSpend = isAllowedToSpend,
                                isBalanceEnough = isBalanceWithoutFeeEnough,
                                hasOutgoingTransaction = hasOutgoingTransaction(fromToken),
                                includeFeeInAmount = includeFeeInAmount,
                            ),
                        )
                    }
                }
            },
            ifLeft = { error ->
                val rates = getQuotes(fromToken.currency.id)
                val fromTokenSwapInfo = TokenSwapInfo(
                    tokenAmount = amount,
                    amountFiat = rates[fromToken.currency.id]?.fiatRate?.multiply(amount.value)
                        ?: BigDecimal.ZERO,
                    cryptoCurrencyStatus = fromToken,
                )
                return SwapState.SwapError(fromTokenSwapInfo, error, includeFeeInAmount)
            },
        )
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun getIncludeFeeInAmount(
        networkId: String,
        txFee: TxFeeState,
        amount: SwapAmount,
        fromToken: CryptoCurrency,
    ): IncludeFeeInAmount {
        val feeValue = when (txFee) {
            TxFeeState.Empty -> BigDecimal.ZERO
            is TxFeeState.MultipleFeeState -> txFee.priorityFee.feeValue
            is TxFeeState.SingleFeeState -> txFee.fee.feeValue
        }
        val feePaidCurrency = getFeePaidCurrency(
            userWalletId = requireNotNull(getSelectedWallet()).walletId,
            currency = fromToken,
        )

        return when (feePaidCurrency) {
            is FeePaidCurrency.Token -> {
                if (feePaidCurrency.balance > feeValue) {
                    IncludeFeeInAmount.Excluded
                } else {
                    IncludeFeeInAmount.BalanceNotEnough
                }
            }
            else -> getIncludeFeeAmountForCoinFee(networkId, amount, feeValue, fromToken)
        }
    }

    private suspend fun getIncludeFeeAmountForCoinFee(
        networkId: String,
        amount: SwapAmount,
        feeValue: BigDecimal,
        fromToken: CryptoCurrency,
    ): IncludeFeeInAmount {
        val tokenForFeeBalance =
            userWalletManager.getNativeTokenBalance(
                networkId,
                fromToken.network.derivationPath.value,
            ) ?: ProxyAmount.empty()
        val amountWithFee = amount.value + feeValue
        return when {
            fromToken is CryptoCurrency.Token -> {
                if (feeValue > tokenForFeeBalance.value || tokenForFeeBalance.value.signum() == 0) {
                    IncludeFeeInAmount.BalanceNotEnough
                } else {
                    IncludeFeeInAmount.Excluded
                }
            }
            amount.value > tokenForFeeBalance.value -> {
                IncludeFeeInAmount.BalanceNotEnough
            }
            amountWithFee < tokenForFeeBalance.value -> {
                IncludeFeeInAmount.Excluded
            }
            else -> {
                if (feeValue < amount.value) {
                    IncludeFeeInAmount.Included(
                        SwapAmount(
                            tokenForFeeBalance.value - feeValue,
                            getNativeToken(fromToken.network.backendId).decimals,
                        ),
                    )
                } else {
                    IncludeFeeInAmount.BalanceNotEnough
                }
            }
        }
    }

    private suspend fun getFormattedFiatFees(fromToken: CryptoCurrency, vararg fees: BigDecimal): List<String> {
        val appCurrency = getSelectedAppCurrencyUseCase.unwrap()
        val feePaidCurrency = getFeePaidCurrency(
            userWalletId = requireNotNull(getSelectedWallet()).walletId,
            currency = fromToken,
        )
        val feeCurrencyId: CryptoCurrency.ID = when (feePaidCurrency) {
            is FeePaidCurrency.Token -> feePaidCurrency.tokenId
            else -> getNativeToken(networkId = fromToken.network.backendId).id
        }
        val rates = getQuotes(feeCurrencyId)
        return rates[feeCurrencyId]?.fiatRate?.let { rate ->
            fees.map { fee ->
                fee.toFiatString(rate, appCurrency.symbol, true)
            }
        }.orEmpty()
    }

    /**
     * Load swap data calls only if spend is allowed for token contract address
     */
    @Suppress("LongParameterList", "LongMethod")
    private suspend fun loadDexSwapData(
        provider: SwapProvider,
        networkId: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        amount: SwapAmount,
        selectedFee: FeeType,
    ): SwapState {
        return repository.getExchangeData(
            fromContractAddress = fromToken.currency.getContractAddress(),
            fromNetwork = fromToken.currency.network.backendId,
            toContractAddress = toToken.currency.getContractAddress(),
            toNetwork = toToken.currency.network.backendId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toToken.currency.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
            toAddress = toToken.value.networkAddress?.defaultAddress?.value ?: "",
        ).fold(
            ifRight = { swapData ->
                val feeData = transactionManager.getFee(
                    networkId = networkId,
                    amountToSend = amount.value,
                    currencyToSend = swapCurrencyConverter.convert(fromToken.currency),
                    destinationAddress = swapData.transaction.txTo,
                    increaseBy = INCREASE_GAS_LIMIT_BY,
                    data = (swapData.transaction as ExpressTransactionModel.DEX).txData,
                    derivationPath = fromToken.currency.network.derivationPath.value,
                )
                val txFeeState = when (feeData) {
                    is ProxyFees.MultipleFees -> feeData.proxyFeesToFeeState(fromToken.currency)
                    is ProxyFees.SingleFee -> feeData.proxyFeesToFeeState(fromToken.currency)
                }
                val feeByPriority = selectFeeByType(feeType = selectedFee, txFeeState = txFeeState)
                val isBalanceIncludeFeeEnough = isBalanceEnough(fromToken, amount, feeByPriority)
                val feeState = getFeeState(
                    fee = feeByPriority,
                    spendAmount = amount,
                    networkId = networkId,
                    fromTokenStatus = fromToken,
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
                swapState.copy(
                    permissionState = PermissionDataState.Empty,
                    warnings = manageWarnings(fromToken, amount, txFeeState),
                    preparedSwapConfigState = PreparedSwapConfigState(
                        isAllowedToSpend = true,
                        isBalanceEnough = isBalanceIncludeFeeEnough,
                        feeState = feeState,
                        hasOutgoingTransaction = hasOutgoingTransaction(fromToken),
                        includeFeeInAmount = IncludeFeeInAmount.Excluded, // exclude for dex
                    ),
                )
            },
            ifLeft = { error ->
                val rates = getQuotes(fromToken.currency.id)
                val fromTokenSwapInfo = TokenSwapInfo(
                    tokenAmount = amount,
                    amountFiat = rates[fromToken.currency.id]?.fiatRate?.multiply(amount.value)
                        ?: BigDecimal.ZERO,
                    cryptoCurrencyStatus = fromToken,
                )
                SwapState.SwapError(
                    fromTokenSwapInfo,
                    error,
                    IncludeFeeInAmount.Excluded,
                )
            },
        )
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
            swapDataModel = swapData,
            txFee = txFeeState,
        )
    }

    private suspend fun getFeeForCex(amount: SwapAmount, fromToken: CryptoCurrencyStatus): TxFeeState {
        getSelectedWalletSyncUseCase().getOrNull()?.walletId?.let { userWalletId ->
            val txFeeResult = estimateFeeUseCase(
                amount = amount.value,
                userWalletId = userWalletId,
                cryptoCurrency = fromToken.currency,
            ).firstOrNull()
            return txFeeResult?.fold(
                ifLeft = {
                    TxFeeState.Empty
                },
                ifRight = { txFee ->
                    txFee.toTxFeeState(fromToken.currency)
                },
            ) ?: TxFeeState.Empty
        }
        return TxFeeState.Empty
    }

    @Suppress("LongParameterList", "LongMethod")
    private suspend fun updatePermissionState(
        networkId: String,
        fromTokenStatus: CryptoCurrencyStatus,
        swapAmount: SwapAmount,
        quotesLoadedState: SwapState.QuotesLoadedState,
        spenderAddress: String?,
        isAllowedToSpend: Boolean,
    ): SwapState.QuotesLoadedState {
        val fromToken = fromTokenStatus.currency
        if (isAllowedToSpend) {
            return quotesLoadedState.copy(
                permissionState = PermissionDataState.Empty,
            )
        }
        // if token balance ZERO not show permission state to avoid user to spend money for fee
        val isTokenZeroBalance = getTokenBalance(fromTokenStatus).value.signum() == 0
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
        val derivationPath = fromToken.network.derivationPath.value
        // setting up amount for approve with given amount for swap [SwapApproveType.Limited]
        val transactionData = getApproveData(
            networkId = networkId,
            derivationPath = derivationPath,
            fromToken = fromToken,
            swapAmount = swapAmount,
            spenderAddress = requireNotNull(spenderAddress) { "Spender address is null" },
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
                is ProxyFees.MultipleFees -> feeData.proxyFeesToFeeState(fromToken)
                is ProxyFees.SingleFee -> feeData.proxyFeesToFeeState(fromToken)
            }
        } ?: TxFeeState.Empty
        val fee = when (feeState) {
            TxFeeState.Empty -> BigDecimal.ZERO
            is TxFeeState.MultipleFeeState -> feeState.normalFee.feeValue
            is TxFeeState.SingleFeeState -> feeState.fee.feeValue
        }
        val swapFeeState = getFeeState(
            fee = fee,
            spendAmount = SwapAmount.zeroSwapAmount(),
            networkId = networkId,
            fromTokenStatus = fromTokenStatus,
        )
        return quotesLoadedState.copy(
            permissionState = PermissionDataState.PermissionReadyForRequest(
                currency = fromToken.symbol,
                amount = INFINITY_SYMBOL,
                walletAddress = getWalletAddress(networkId, derivationPath),
                spenderAddress = getTokenAddress(fromToken),
                requestApproveData = RequestApproveStateData(
                    fee = feeState,
                    approveData = transactionData,
                    fromTokenAmount = swapAmount,
                    spenderAddress = spenderAddress,
                ),
            ),
            preparedSwapConfigState = quotesLoadedState.preparedSwapConfigState.copy(
                feeState = swapFeeState,
            ),
        )
    }

    private suspend fun ProxyFees.MultipleFees.proxyFeesToFeeState(fromToken: CryptoCurrency): TxFeeState {
        val normalFeeValue = this.minFee.fee.value // in swap for normal use min fee
        val normalFeeGas = this.minFee.gasLimit.toInt()
        val priorityFeeValue = this.normalFee.fee.value // in swap for priority use normal fee
        val priorityFeeGas = this.normalFee.gasLimit.toInt()
        val feesFiat = getFormattedFiatFees(fromToken, normalFeeValue, priorityFeeValue)
        val normalFiatFee = requireNotNull(feesFiat.getOrNull(0)) { "feesFiat item 0 couldn't be null" }
        val priorityFiatFee = requireNotNull(feesFiat.getOrNull(1)) { "feesFiat item 1 couldn't be null" }
        val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
            amount = normalFeeValue,
            decimals = minFee.fee.decimals,
        )
        val priorityCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
            amount = priorityFeeValue,
            decimals = normalFee.fee.decimals,
        )
        return TxFeeState.MultipleFeeState(
            normalFee = TxFee(
                feeValue = normalFeeValue,
                gasLimit = normalFeeGas,
                feeFiatFormatted = normalFiatFee,
                feeCryptoFormatted = normalCryptoFee,
                decimals = minFee.fee.decimals,
                cryptoSymbol = minFee.fee.currencySymbol,
                feeType = FeeType.NORMAL,
            ),
            priorityFee = TxFee(
                feeValue = priorityFeeValue,
                gasLimit = priorityFeeGas,
                feeFiatFormatted = priorityFiatFee,
                feeCryptoFormatted = priorityCryptoFee,
                decimals = normalFee.fee.decimals,
                cryptoSymbol = normalFee.fee.currencySymbol,
                feeType = FeeType.PRIORITY,
            ),
        )
    }

    private suspend fun ProxyFees.SingleFee.proxyFeesToFeeState(fromToken: CryptoCurrency): TxFeeState {
        val normalFeeValue = this.singleFee.fee.value
        val normalFeeGas = this.singleFee.gasLimit.toInt()
        val feesFiat = getFormattedFiatFees(fromToken, normalFeeValue)
        val normalFiatFee = requireNotNull(feesFiat.getOrNull(0)) { "feesFiat item 0 couldn't be null" }
        val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
            amount = normalFeeValue,
            decimals = singleFee.fee.decimals,
        )
        return TxFeeState.SingleFeeState(
            fee = TxFee(
                feeValue = normalFeeValue,
                gasLimit = normalFeeGas,
                feeFiatFormatted = normalFiatFee,
                feeCryptoFormatted = normalCryptoFee,
                decimals = singleFee.fee.decimals,
                cryptoSymbol = singleFee.fee.currencySymbol,
                feeType = FeeType.NORMAL,
            ),
        )
    }

    private suspend fun TransactionFee.toTxFeeState(fromToken: CryptoCurrency): TxFeeState {
        return when (this) {
            is TransactionFee.Choosable -> {
                val normalFee = this.normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SEND)
                val priorityFee = this.priority.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SEND)
                val feeNormal = normalFee.amount.value ?: BigDecimal.ZERO
                val feePriority = priorityFee.amount.value ?: BigDecimal.ZERO
                val normalFiatValue = getFormattedFiatFees(fromToken, feeNormal)[0]
                val priorityFiatValue = getFormattedFiatFees(fromToken, feePriority)[0]

                val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = feeNormal,
                    decimals = normalFee.amount.decimals,
                )
                val priorityCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = feePriority,
                    decimals = priorityFee.amount.decimals,
                )
                TxFeeState.MultipleFeeState(
                    normalFee = TxFee(
                        feeValue = feeNormal,
                        gasLimit = normalFee.getGasLimit(),
                        feeFiatFormatted = normalFiatValue,
                        feeCryptoFormatted = normalCryptoFee,
                        decimals = normalFee.amount.decimals,
                        cryptoSymbol = normalFee.amount.currencySymbol,
                        feeType = FeeType.NORMAL,
                    ),
                    priorityFee = TxFee(
                        feeValue = feePriority,
                        gasLimit = priorityFee.getGasLimit(),
                        feeFiatFormatted = priorityFiatValue,
                        feeCryptoFormatted = priorityCryptoFee,
                        decimals = priorityFee.amount.decimals,
                        cryptoSymbol = priorityFee.amount.currencySymbol,
                        feeType = FeeType.PRIORITY,
                    ),
                )
            }
            is TransactionFee.Single -> {
                val feeNormal = this.normal.amount.value ?: BigDecimal.ZERO
                val normalFiatValue = getFormattedFiatFees(fromToken, feeNormal)[0]
                val normalCryptoFee = amountFormatter.formatBigDecimalAmountToUI(
                    amount = feeNormal,
                    decimals = this.normal.amount.decimals,
                )
                TxFeeState.SingleFeeState(
                    fee = TxFee(
                        feeValue = this.normal.amount.value ?: BigDecimal.ZERO,
                        gasLimit = this.normal.getGasLimit(),
                        feeFiatFormatted = normalFiatValue,
                        feeCryptoFormatted = normalCryptoFee,
                        decimals = normal.amount.decimals,
                        cryptoSymbol = normal.amount.currencySymbol,
                        feeType = FeeType.NORMAL,
                    ),
                )
            }
        }
    }

    /**
     * Workaround to increase gas limit cause we calculate fee for random address
     */
    private fun Fee.increaseGasLimitBy(percentage: Int): Fee {
        if (this !is Fee.Ethereum) return this
        val gasLimit = this.gasLimit
        val increasedGasPrice = this.amount.value?.movePointRight(this.amount.decimals)
            ?.divide(gasLimit.toBigDecimal(), RoundingMode.HALF_UP)
        val increasedGasLimit = gasLimit
            .multiply(percentage.toBigInteger())
            .divide(hundredPercent)
        val increasedAmount = this.amount.copy(
            value = increasedGasLimit.toBigDecimal().multiply(increasedGasPrice).movePointLeft(this.amount.decimals),
        )
        return this.copy(
            amount = increasedAmount,
            gasLimit = increasedGasLimit,
        )
    }

    private fun hasOutgoingTransaction(cryptoCurrencyStatuses: CryptoCurrencyStatus): Boolean {
        return cryptoCurrencyStatuses.value.pendingTransactions.any { it.isOutgoing }
    }

    private fun Fee.getGasLimit(): Int {
        return when (this) {
            is Fee.Ethereum -> gasLimit.toInt()
            is Fee.VeChain -> gasLimit.toInt()
            is Fee.Aptos -> gasLimit.toInt()
            else -> 0
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

    private suspend fun isBalanceEnough(
        fromToken: CryptoCurrencyStatus,
        amount: SwapAmount,
        fee: BigDecimal?,
    ): Boolean {
        val tokenBalance = getTokenBalance(fromToken).value
        val feePaidCurrency = getFeePaidCurrency(
            userWalletId = requireNotNull(getSelectedWallet()).walletId,
            currency = fromToken.currency,
        )
        return when (feePaidCurrency) {
            is FeePaidCurrency.Token -> tokenBalance >= amount.value
            else -> {
                if (fromToken.currency is CryptoCurrency.Token) {
                    tokenBalance >= amount.value
                } else {
                    tokenBalance > amount.value.plus(fee ?: BigDecimal.ZERO)
                }
            }
        }
    }

    private suspend fun getFeePaidCurrency(userWalletId: UserWalletId, currency: CryptoCurrency): FeePaidCurrency {
        return currenciesRepository.getFeePaidCurrency(
            userWalletId = userWalletId,
            currency = currency,
        )
    }

    private suspend fun getWalletAddress(networkId: String, derivationPath: String?): String {
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

    private fun toBigDecimalOrNull(amountToSwap: String): BigDecimal? {
        return amountToSwap.replace(",", ".").toBigDecimalOrNull()
    }

    private suspend fun getFeeState(
        fee: BigDecimal?,
        spendAmount: SwapAmount,
        networkId: String,
        fromTokenStatus: CryptoCurrencyStatus,
    ): SwapFeeState {
        val userWalletId = requireNotNull(getSelectedWallet()).walletId
        if (fee == null) {
            return SwapFeeState.NotEnough()
        }

        val percentsToFeeIncrease = BigDecimal.ONE
        return when (val feePaidCurrency = getFeePaidCurrency(userWalletId, fromTokenStatus.currency)) {
            FeePaidCurrency.Coin -> {
                val nativeTokenBalance = userWalletManager.getNativeTokenBalance(
                    networkId,
                    fromTokenStatus.currency.network.derivationPath.value,
                )
                nativeTokenBalance?.let { balance ->
                    val balanceToCheck = when (fromTokenStatus.currency) {
                        is CryptoCurrency.Token -> {
                            balance.value
                        }
                        is CryptoCurrency.Coin -> {
                            // need to check balance minus amount only if amount to swap in native token
                            balance.value.minus(spendAmount.value)
                        }
                    }
                    if (balanceToCheck > fee.multiply(percentsToFeeIncrease)) {
                        SwapFeeState.Enough
                    } else {
                        val nativeToken = getNativeToken(fromTokenStatus.currency.network.backendId)
                        SwapFeeState.NotEnough(
                            feeCurrency = nativeToken,
                            currencyName = nativeToken.network.name,
                            currencySymbol = nativeToken.symbol,
                        )
                    }
                } ?: SwapFeeState.NotEnough()
            }
            FeePaidCurrency.SameCurrency -> {
                val balance = fromTokenStatus.value.amount ?: return SwapFeeState.NotEnough()
                if (balance.minus(spendAmount.value) > fee.multiply(percentsToFeeIncrease)) {
                    SwapFeeState.Enough
                } else {
                    SwapFeeState.NotEnough(
                        feeCurrency = fromTokenStatus.currency,
                        currencyName = fromTokenStatus.currency.network.name,
                        currencySymbol = fromTokenStatus.currency.symbol,
                    )
                }
            }
            is FeePaidCurrency.Token -> {
                if (feePaidCurrency.balance > fee.multiply(percentsToFeeIncrease)) {
                    SwapFeeState.Enough
                } else {
                    val token = currenciesRepository
                        .getMultiCurrencyWalletCurrenciesSync(userWalletId)
                        .filterIsInstance<CryptoCurrency.Token>()
                        .find {
                            it.contractAddress.equals(feePaidCurrency.contractAddress, ignoreCase = true) &&
                                it.network.derivationPath == fromTokenStatus.currency.network.derivationPath
                        }
                    SwapFeeState.NotEnough(
                        feeCurrency = token,
                        currencyName = feePaidCurrency.name,
                        currencySymbol = feePaidCurrency.symbol,
                    )
                }
            }
        }
    }

    private fun calculatePriceImpact(
        fromTokenAmount: BigDecimal,
        fromRate: Double,
        toTokenAmount: BigDecimal,
        toRate: Double,
    ): PriceImpact {
        val fromTokenFiatValue = fromTokenAmount.multiply(fromRate.toBigDecimal())
        val toTokenFiatValue = toTokenAmount.multiply(toRate.toBigDecimal())
        val value = (BigDecimal.ONE - toTokenFiatValue.divide(fromTokenFiatValue, 2, RoundingMode.HALF_UP)).toFloat()
        return PriceImpact.Value(value)
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
        private const val INCREASE_GAS_LIMIT_FOR_SEND = 105 // 5%
        private const val INFINITY_SYMBOL = "∞"
    }
}
