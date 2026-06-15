package com.tangem.feature.swap.domain

import android.util.Base64
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.extenstions.unwrap
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.express.models.*
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayWithdrawExchangeState
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.swap.models.SwapTxType
import com.tangem.domain.swap.usecase.GetSwapPairUseCase
import com.tangem.domain.tokens.GetAssetRequirementsUseCase
import com.tangem.domain.tokens.GetCurrencyCheckUseCase
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.YieldModuleAddressProvider
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.fee.CexSwapFeeCalculator
import com.tangem.feature.swap.domain.fee.DexSwapFeeCalculator
import com.tangem.feature.swap.domain.fee.SwapFeeFactory
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.toStringWithRightOffset
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Collections.newSetFromMap
import java.util.concurrent.ConcurrentHashMap

@Suppress("LargeClass", "LongParameterList")
internal class SwapInteractorImpl @Inject constructor(
    private val repository: SwapRepository,
    private val allowPermissionsHandler: AllowPermissionsHandler,
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val createTransactionExtrasUseCase: CreateTransactionDataExtrasUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val quotesRepository: QuotesRepository,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val swapTransactionRepository: SwapTransactionRepository,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val appCurrencyRepository: AppCurrencyRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase,
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase,
    private val getAssetRequirementsUseCase: GetAssetRequirementsUseCase,
    private val amountFormatter: AmountFormatter,
    private val rampStateManager: RampStateManager,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val getAllowanceInfoUseCase: GetAllowanceInfoUseCase,
    private val getSwapPairUseCase: GetSwapPairUseCase,
    private val dexSwapFeeCalculator: DexSwapFeeCalculator,
    private val cexSwapFeeCalculator: CexSwapFeeCalculator,
    private val swapFeatureToggles: SwapFeatureToggles,
    private val yieldModuleAddressProvider: YieldModuleAddressProvider,
) : SwapInteractor {

    private val getSelectedAppCurrencyUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetSelectedAppCurrencyUseCase(appCurrencyRepository)
    }

    private val SwapCurrencyStatus.isYieldSwapActive: Boolean
        get() = swapFeatureToggles.isYieldSwapEnabled && isYieldSupplyActive

    /**
     * Set of integrated-approve contexts for which the simulated swap-fee estimation
     * failed with [GetFeeError.EstimateOverrideError]. Once a context is recorded here, the
     * integrated path is abandoned for the remainder of the session: the permission state is
     * derived as [PermissionDataState.PermissionRequired] (legacy separate-approval flow).
     * This survives the periodic quote-refresh task so the failing simulated estimation is not retried every cycle.
     */
    private val integratedApprovalFallbackContexts = newSetFromMap(
        ConcurrentHashMap<IntegratedApprovalFallbackKey, Boolean>(),
    )

    private fun hasIntegratedApprovalFallenBack(fromSwapCurrencyStatus: SwapCurrencyStatus, spenderAddress: String?) =
        integratedApprovalFallbackContexts.contains(
            IntegratedApprovalFallbackKey.of(fromSwapCurrencyStatus, spenderAddress),
        )

    override suspend fun getPair(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        filterProviderTypes: List<ExchangeProviderType>,
    ): Either<ExpressError, List<SwapPairLeast>> {
        return getSwapPairUseCase(
            primarySwapCurrencyStatus = fromSwapCurrencyStatus,
            secondarySwapCurrencyStatus = toSwapCurrencyStatus,
            filterProviderTypes = filterProviderTypes.map { type ->
                // Temporary solution until domain layer is migrated
                when (type) {
                    ExchangeProviderType.DEX -> ExpressProviderType.DEX
                    ExchangeProviderType.CEX -> ExpressProviderType.CEX
                    ExchangeProviderType.DEX_BRIDGE -> ExpressProviderType.DEX_BRIDGE
                }
            },
            swapTxType = SwapTxType.Swap,
        ).map { pairs ->
            pairs.map { pair ->
                SwapPairLeast(
                    from = LeastTokenInfo(
                        contractAddress = pair.from.currency.getContractAddress(),
                        network = pair.from.currency.network.rawId,
                    ),
                    to = LeastTokenInfo(
                        contractAddress = pair.to.currency.getContractAddress(),
                        network = pair.to.currency.network.rawId,
                    ),
                    providers = pair.providers.map { provider ->
                        provider.toSwapProvider()
                    },
                )
            }
        }
    }

    override fun findProvidersForPair(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        pairs: List<SwapPairLeast>,
    ): List<SwapProvider> {
        return pairs.firstOrNull { pair ->
            pair.from.network == fromSwapCurrencyStatus.currency.network.rawId &&
                pair.from.contractAddress == fromSwapCurrencyStatus.currency.getContractAddress() &&
                pair.to.network == toSwapCurrencyStatus.currency.network.rawId &&
                pair.to.contractAddress == toSwapCurrencyStatus.currency.getContractAddress()
        }?.providers.orEmpty()
    }

    override fun extractFromSwapCurrencyFromPair(
        pair: SwapPairLeast,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
    ): SwapCurrencyStatus? {
        return if (pair.from.network == fromSwapCurrencyStatus.currency.network.rawId &&
            pair.from.contractAddress == fromSwapCurrencyStatus.currency.getContractAddress()
        ) {
            fromSwapCurrencyStatus
        } else if (
            pair.from.network == toSwapCurrencyStatus.currency.network.rawId &&
            pair.from.contractAddress == toSwapCurrencyStatus.currency.getContractAddress()
        ) {
            toSwapCurrencyStatus
        } else {
            null
        }
    }

    override suspend fun findProvidersForPairWithCheck(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        pairs: List<SwapPairLeast>,
    ): List<SwapProvider> {
        val requirements = getAssetRequirementsUseCase.invoke(
            fromSwapCurrencyStatus.userWalletId,
            fromSwapCurrencyStatus.currency,
        ).getOrNull()

        if (!rampStateManager.checkAssetRequirements(requirements)) {
            return emptyList()
        }

        return findProvidersForPair(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            pairs = pairs,
        )
    }

    override suspend fun findBestQuote(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        providers: List<SwapProvider>,
        amountToSwap: String,
        reduceBalanceBy: BigDecimal,
    ): Map<SwapProvider, SwapState> {
        TangemLogger.i(
            """
               Find the best quote
               |- fromSwapCurrencyStatus:
               |---- walletId: ${fromSwapCurrencyStatus.userWalletId}
               |---- accountId: ${fromSwapCurrencyStatus.account.accountId}
               |---- currencyId: ${fromSwapCurrencyStatus.currency.id}
               |- toSwapCurrencyStatus: $toSwapCurrencyStatus
               |---- walletId: ${toSwapCurrencyStatus.userWalletId}
               |---- accountId: ${toSwapCurrencyStatus.account.accountId}
               |---- currencyId: ${toSwapCurrencyStatus.currency.id}
               |- providers: $providers
               |- amountToSwap: $amountToSwap
            """.trimIndent(),
            shouldSanitize = false,
        )

        val amountDecimal = toBigDecimalOrNull(amountToSwap)
        if (amountDecimal == null || amountDecimal.signum() == 0) {
            return providers.associateWith { createEmptyAmountState() }
        }
        val amount = SwapAmount(amountDecimal, fromSwapCurrencyStatus.currency.decimals)

        return supervisorScope {
            providers.map { provider ->
                async {
                    when (provider.type) {
                        ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> {
                            if (isSolana(fromSwapCurrencyStatus.currency.network.rawId)) {
                                manageDexSolana(
                                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                                    provider = provider,
                                    amount = amount,
                                    reduceBalanceBy = reduceBalanceBy,
                                    expressOperationType = ExpressOperationType.SWAP,
                                )
                            } else {
                                manageDex(
                                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                                    provider = provider,
                                    amount = amount,
                                    reduceBalanceBy = reduceBalanceBy,
                                    expressOperationType = ExpressOperationType.SWAP,
                                )
                            }
                        }
                        ExchangeProviderType.CEX -> {
                            manageCex(
                                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                                toSwapCurrencyStatus = toSwapCurrencyStatus,
                                provider = provider,
                                amount = amount,
                                reduceBalanceBy = reduceBalanceBy,
                            )
                        }
                    }
                }
            }.awaitAll().toMap()
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private suspend fun manageDex(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        expressOperationType: ExpressOperationType,
    ): Pair<SwapProvider, SwapState> {
        if (fromSwapCurrencyStatus.status.value.yieldSupplyStatus?.isActive == true &&
            !swapFeatureToggles.isYieldSwapEnabled
        ) {
            return provider to produceDexSwapDataError(
                error = ExpressDataError.DexActiveSupplyError(),
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                amount = amount,
            )
        }

        val maybeQuote = repository.findBestQuote(
            userWallet = fromSwapCurrencyStatus.userWallet,
            fromContractAddress = fromSwapCurrencyStatus.currency.getContractAddress(),
            fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
            toContractAddress = toSwapCurrencyStatus.currency.getContractAddress(),
            toNetwork = toSwapCurrencyStatus.currency.network.rawId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toSwapCurrencyStatus.currency.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
        )

        if (maybeQuote.getOrNull()?.txType == ExpressTxType.SEND) {
            return manageCex(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                provider = provider,
                amount = amount,
                reduceBalanceBy = reduceBalanceBy,
            )
        }

        val fromTokenAddress = getTokenAddress(fromSwapCurrencyStatus.currency)

        val isYieldSwap = fromSwapCurrencyStatus.isYieldSwapActive &&
            fromSwapCurrencyStatus.currency is CryptoCurrency.Token

        val spenderAddress = if (isYieldSwap) {
            yieldModuleAddressProvider.getOrFetch(
                userWalletId = fromSwapCurrencyStatus.userWalletId,
                network = fromSwapCurrencyStatus.currency.network,
            )
        } else {
            maybeQuote.getOrNull()?.allowanceContract
        }

        val dexRouterSpenderAddress = maybeQuote.getOrNull()?.allowanceContract

        val allowanceInfo = spenderAddress?.let { allowanceContract ->
            getAllowanceInfoUseCase(
                userWalletId = fromSwapCurrencyStatus.userWalletId,
                cryptoCurrency = fromSwapCurrencyStatus.currency,
                spenderAddress = allowanceContract,
                requiredAmount = amount.value,
            ).getOrNull()
        } ?: AllowanceInfo.Enough(allowance = BigDecimal.ZERO)

        if (allowanceInfo is AllowanceInfo.Enough &&
            allowPermissionsHandler.isAddressAllowanceInProgress(fromTokenAddress)
        ) {
            allowPermissionsHandler.removeAddressFromProgress(fromTokenAddress)
            cryptoCurrencyBalanceFetcher(
                userWalletId = fromSwapCurrencyStatus.userWalletId,
                currency = fromSwapCurrencyStatus.currency,
            )
        }
        val isBalanceWithoutFeeEnough = isBalanceEnough(fromSwapCurrencyStatus, amount, null)
        val isIntegratedApproveActive = swapFeatureToggles.isSwapIntegratedApproveEnabled &&
            !hasIntegratedApprovalFallenBack(fromSwapCurrencyStatus, spenderAddress)

        val isAllowanceSatisfied = if (isIntegratedApproveActive) {
            allowanceInfo !is AllowanceInfo.ResetNeeded
        } else {
            allowanceInfo is AllowanceInfo.Enough
        }
        // For yield swaps the on-chain allowance is not sufficient on its own: spending also
        // requires the yield-module proxy approval (yieldSupplyStatus.isAllowedToSpend).
        // For regular swaps a failed quote must not proceed to exchange-data loading.
        val isAllowedToSpend = if (isYieldSwap) {
            isAllowanceSatisfied &&
                fromSwapCurrencyStatus.status.value.yieldSupplyStatus?.isAllowedToSpend == true
        } else {
            isAllowanceSatisfied && maybeQuote.isRight()
        }
        return if (isAllowedToSpend && isBalanceWithoutFeeEnough) {
            provider to loadDexSwapDataNoFee(
                provider = provider,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                amount = amount,
                expressOperationType = expressOperationType,
                allowanceInfo = allowanceInfo,
                spenderAddress = spenderAddress,
                dexRouterSpenderAddress = dexRouterSpenderAddress,
            )
        } else {
            val quoteBalanceStatus = if (isBalanceWithoutFeeEnough) {
                SwapBalanceStatus.Pending // fee not resolved yet
            } else {
                SwapBalanceStatus.InsufficientAmount
            }
            provider to getQuotesState(
                provider = provider,
                quoteDataModel = maybeQuote,
                amount = amount,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                isAllowedToSpend = isAllowedToSpend,
                quoteBalanceStatus = quoteBalanceStatus,
            )
        }
    }

    private suspend fun manageDexSolana(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        expressOperationType: ExpressOperationType,
    ): Pair<SwapProvider, SwapState> {
        val maybeQuotes = repository.findBestQuote(
            userWallet = fromSwapCurrencyStatus.userWallet,
            fromContractAddress = fromSwapCurrencyStatus.currency.getContractAddress(),
            fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
            toContractAddress = toSwapCurrencyStatus.currency.getContractAddress(),
            toNetwork = toSwapCurrencyStatus.currency.network.rawId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toSwapCurrencyStatus.currency.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
        )

        if (maybeQuotes.getOrNull()?.txType == ExpressTxType.SEND) {
            return manageCex(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                provider = provider,
                amount = amount,
                reduceBalanceBy = reduceBalanceBy,
            )
        }

        val quoteBalanceStatus = if (isBalanceEnough(fromSwapCurrencyStatus, amount, null)) {
            SwapBalanceStatus.Pending // fee not resolved yet
        } else {
            SwapBalanceStatus.InsufficientAmount
        }
        return if (quoteBalanceStatus != SwapBalanceStatus.InsufficientAmount && maybeQuotes.isRight()) {
            provider to loadDexSwapDataNoFee(
                provider = provider,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                amount = amount,
                expressOperationType = expressOperationType,
                allowanceInfo = null,
                spenderAddress = null,
                dexRouterSpenderAddress = null,
            )
        } else {
            provider to getQuotesState(
                provider = provider,
                quoteDataModel = maybeQuotes,
                amount = amount,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                isAllowedToSpend = true,
                quoteBalanceStatus = quoteBalanceStatus,
            )
        }
    }

    private suspend fun manageCex(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
    ): Pair<SwapProvider, SwapState> {
        val fromToken = fromSwapCurrencyStatus.currency
        val toToken = toSwapCurrencyStatus.currency

        val includeFeeInAmount = getIncludeFeeInAmountInternal(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            amount = amount,
            reduceBalanceBy = reduceBalanceBy,
            feeValue = BigDecimal.ZERO,
        )

        val amountToRequest = if (includeFeeInAmount is IncludeFeeInAmountInternal.Included) {
            includeFeeInAmount.amountSubtractFee
        } else {
            amount
        }

        val quotes = repository.findBestQuote(
            userWallet = fromSwapCurrencyStatus.userWallet,
            fromContractAddress = fromToken.getContractAddress(),
            fromNetwork = fromToken.network.rawId,
            toContractAddress = toToken.getContractAddress(),
            toNetwork = toToken.network.rawId,
            fromAmount = amountToRequest.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toToken.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
        )

        val quoteBalanceStatus = if (includeFeeInAmount == IncludeFeeInAmountInternal.BalanceNotEnough) {
            SwapBalanceStatus.InsufficientAmount
        } else {
            SwapBalanceStatus.Pending // fee not resolved yet
        }

        return provider to getQuotesState(
            provider = provider,
            quoteDataModel = quotes,
            amount = amount,
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            isAllowedToSpend = true,
            quoteBalanceStatus = quoteBalanceStatus,
        )
    }

    private suspend fun manageWarnings(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        fee: BigDecimal,
        balanceStatus: SwapBalanceStatus,
    ): CryptoCurrencyCheck {
        val balanceAfterTransaction = getCoinBalanceAfterTransaction(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            amount = amount,
            balanceStatus = balanceStatus,
            fee = fee,
        )
        val amountToRequest = (balanceStatus as? SwapBalanceStatus.FeeAdjustedAmount)?.adjustedAmount ?: amount
        val feePaidCurrencyStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
        ).getOrNull()
        val currencyCheck = getCurrencyCheckUseCase(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            currencyStatus = fromSwapCurrencyStatus.status,
            feeCurrencyStatus = feePaidCurrencyStatus,
            amount = amountToRequest.value,
            fee = fee,
            feeCurrencyBalanceAfterTransaction = balanceAfterTransaction,
        )

        return currencyCheck
    }

    /**
     * - `FeeAdjustedAmount`  → equivalent to `Included(adjusted)`: subtract adjusted + fee
     * - `Sufficient` / `InsufficientFee` → equivalent to `Excluded`: subtract amount + fee
     * - `InsufficientAmount` / `Pending` → returns null
     */
    private suspend fun getCoinBalanceAfterTransaction(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        balanceStatus: SwapBalanceStatus,
        fee: BigDecimal,
    ): BigDecimal? {
        return when (fromSwapCurrencyStatus.currency) {
            is CryptoCurrency.Coin -> {
                val statusValue = fromSwapCurrencyStatus.status.value as? CryptoCurrencyStatus.Loaded ?: return null
                when (balanceStatus) {
                    is SwapBalanceStatus.FeeAdjustedAmount -> {
                        statusValue.amount - balanceStatus.adjustedAmount.value - fee
                    }
                    is SwapBalanceStatus.Sufficient,
                    is SwapBalanceStatus.InsufficientFee,
                    -> statusValue.amount - amount.value - fee
                    is SwapBalanceStatus.InsufficientAmount,
                    is SwapBalanceStatus.Pending,
                    -> null
                }
            }
            is CryptoCurrency.Token -> {
                val feePaidCurrency = getFeePaidCurrency(fromSwapCurrencyStatus)
                when (feePaidCurrency) {
                    FeePaidCurrency.Coin -> {
                        val nativeBalance = walletManagersFacade.getNativeTokenBalance(
                            userWalletId = fromSwapCurrencyStatus.userWalletId,
                            networkId = fromSwapCurrencyStatus.currency.network.rawId,
                            derivationPath = fromSwapCurrencyStatus.currency.network.derivationPath.value,
                        )

                        nativeBalance - fee
                    }
                    else -> null // it doesn't matter for this fun
                }
            }
        }
    }

    private suspend fun manageTransactionValidationWarnings(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        feeValue: BigDecimal,
    ): Throwable? {
        val currency = fromSwapCurrencyStatus.currency
        val blockchain = currency.network.toBlockchain()
        // Stellar validation removed because swap uses destination = "0" and throws an error
        if (blockchain == Blockchain.Stellar) {
            return null
        }

        val fee = Fee.Common(
            amount = Amount(
                value = feeValue,
                blockchain = blockchain,
            ),
        )

        val result = validateTransactionUseCase(
            amount = amount.value.convertToSdkAmount(fromSwapCurrencyStatus.status),
            fee = fee,
            memo = null,
            destination = getTokenAddress(fromSwapCurrencyStatus.currency),
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            network = currency.network,
        ).leftOrNull()

        return result
    }

    @Suppress("NullableToStringCall", "LongParameterList")
    override suspend fun onSwap(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        swapProvider: SwapProvider,
        swapData: SwapDataModel?,
        amountToSwap: String,
        balanceStatus: SwapBalanceStatus,
        fee: SwapFee?,
        expressOperationType: ExpressOperationType,
        isTangemPayWithdrawal: Boolean,
        integratedApproval: IntegratedApprovalData?,
    ): SwapTransactionState {
        TangemLogger.i(
            """
               Swap
               |- swapProvider: $swapProvider
               |- swapData: $swapData
               |- fromSwapCurrencyStatus:
               |---- walletId: ${fromSwapCurrencyStatus.userWalletId}
               |---- accountId: ${fromSwapCurrencyStatus.account.accountId}
               |---- currencyId: ${fromSwapCurrencyStatus.currency.id}
               |- toSwapCurrencyStatus: $toSwapCurrencyStatus
               |---- walletId: ${toSwapCurrencyStatus.userWalletId}
               |---- accountId: ${toSwapCurrencyStatus.account.accountId}
               |---- currencyId: ${toSwapCurrencyStatus.currency.id}
               |- amountToSwap: $amountToSwap
               |- balanceStatus: $balanceStatus
               |- fee: $fee
            """.trimIndent(),
            shouldSanitize = false,
        )

        val userWallet = fromSwapCurrencyStatus.userWallet
        if (userWallet is UserWallet.Cold && isDemoCardUseCase(userWallet.scanResponse.card.cardId)) {
            return SwapTransactionState.DemoMode
        }

        return when (resolveSwapDataFlow(swapProvider, swapData)) {
            ResolvedFlow.CexLike -> {
                val amountDecimal = toBigDecimalOrNull(amountToSwap)
                val amount = SwapAmount(requireNotNull(amountDecimal), fromSwapCurrencyStatus.currency.decimals)
                val amountToSwapWithFee = (balanceStatus as? SwapBalanceStatus.FeeAdjustedAmount)?.adjustedAmount
                    ?: amount
                onSwapCex(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    amount = amountToSwapWithFee,
                    swapFee = fee,
                    swapProvider = swapProvider,
                    expressOperationType = expressOperationType,
                    isTangemPayWithdrawal = isTangemPayWithdrawal,
                )
            }
            ResolvedFlow.DexLike -> {
                val networkId = fromSwapCurrencyStatus.currency.network.rawId
                if (isSolana(networkId)) {
                    onSwapSolanaDex(
                        provider = swapProvider,
                        swapData = requireNotNull(swapData),
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                        amountToSwap = amountToSwap,
                    )
                } else {
                    if (fee == null) return SwapTransactionState.Error.UnknownError
                    onSwapDex(
                        provider = swapProvider,
                        swapData = requireNotNull(swapData),
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                        swapFee = fee,
                        amountToSwap = amountToSwap,
                        integratedApproval = integratedApproval,
                    )
                }
            }
        }
    }

    @Suppress("LongParameterList")
    private suspend fun onSwapDex(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        swapData: SwapDataModel,
        amountToSwap: String,
        swapFee: SwapFee,
        integratedApproval: IntegratedApprovalData?,
    ): SwapTransactionState {
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amountToSwap)) { "wrong amount format" }
        val amount = SwapAmount(amountDecimal, fromSwapCurrencyStatus.currency.decimals)
        val dexTransaction = swapData.transaction as ExpressTransactionModel.DEX
        val dataToSign = dexTransaction.txData
        val isYieldSwap = fromSwapCurrencyStatus.isYieldSwapActive
        val fromCurrency = fromSwapCurrencyStatus.currency

        val txDataResult = if (isYieldSwap && fromCurrency is CryptoCurrency.Token) {
            val spenderAddress = dexTransaction.allowanceContract ?: return SwapTransactionState.Error.UnknownError
            createYieldSwapDexTransaction(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                swapData = swapData,
                dexCallData = dataToSign,
                amount = amountDecimal,
                fee = swapFee.fee,
                spenderAddress = spenderAddress,
            )
        } else {
            val txValue = requireNotNull(swapData.transaction.txValue) { "txValue is null" }
            val amountToSend = createNativeAmountForDex(txValue, fromCurrency.network)
            createTransactionUseCase(
                amount = amountToSend,
                fee = swapFee.fee,
                memo = null,
                destination = swapData.transaction.txTo,
                userWalletId = fromSwapCurrencyStatus.userWalletId,
                network = fromCurrency.network,
                txExtras = createDexTxExtras(
                    dataToSign,
                    fromCurrency.network,
                    swapFee.fee.getGasLimit(),
                ),
            )
        }

        val txData = txDataResult.getOrElse { error ->
            TangemLogger.e("Failed to create swap dex tx data", error)
            return SwapTransactionState.Error.UnknownError
        }

        val payInAddress = if (isYieldSwap && fromCurrency is CryptoCurrency.Token) {
            swapData.transaction.txTo
        } else if (txData is TransactionData.Uncompiled) {
            getPayoutAddress(txData)
        } else {
            swapData.transaction.txTo
        }

        return if (integratedApproval != null) {
            // TODO YIELD payInAddress [REDACTED_TASK_KEY]
            sendIntegratedApproveAndSwap(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                provider = provider,
                swapData = swapData,
                amount = amount,
                swapTxData = txData,
                swapFee = swapFee,
                integratedApproval = integratedApproval,
            )
        } else {
            handleSwapResult(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                provider = provider,
                swapData = swapData,
                amount = amount,
                txData = txData,
                payInAddress = payInAddress,
            )
        }
    }

    /**
     * Integrated approve+swap submission. Selects the approval-fee bucket matching
     * the user's swap-fee selection, attaches it to the prepared approval tx, and sends both
     * transactions in a single [TransactionSender.MultipleTransactionSendMode.DEFAULT] batch.
     *
     * The success path is identical to the standalone swap path — only the approval-side hash
     * is dropped (the swap tx hash is what surfaces as the transaction result).
     */
    @Suppress("LongParameterList")
    private suspend fun sendIntegratedApproveAndSwap(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        swapData: SwapDataModel,
        amount: SwapAmount,
        swapTxData: TransactionData.Uncompiled,
        swapFee: SwapFee,
        integratedApproval: IntegratedApprovalData,
    ): SwapTransactionState {
        val approvalFee = selectFeeForBucket(integratedApproval.approvalFee, swapFee.feeBucket)
        val approvalTx = integratedApproval.approvalTransaction.copy(fee = approvalFee)

        val sendResult = sendTransactionUseCase(
            txsData = listOf(approvalTx, swapTxData),
            userWallet = fromSwapCurrencyStatus.userWallet,
            network = fromSwapCurrencyStatus.currency.network,
            sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
        ).fold(
            ifLeft = { error -> return SwapTransactionState.Error.TransactionError(error) },
            ifRight = { hashes -> hashes },
        )

        // The swap tx is the second (and last) hash; the approval hash is intentionally dropped.
        val swapTxHash = sendResult.lastOrNull() ?: return SwapTransactionState.Error.UnknownError

        return finalizeDexSwapSuccess(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            provider = provider,
            swapData = swapData,
            amount = amount,
            txHash = swapTxHash,
            payInAddress = getPayoutAddress(swapTxData),
        )
    }

    /**
     * [REDACTED_TASK_KEY] — selects the approval [Fee] matching the user-picked [FeeBucket] tier. Mirrors
     * the bucket-to-field mapping used by `GiveApprovalModel.sendApprovalTransaction`:
     *  - `SLOW`  → `Choosable.minimum`  (fallback `Single.normal`)
     *  - `FAST`  → `Choosable.priority` (fallback `Single.normal`)
     *  - all other buckets → `normal`
     */
    private fun selectFeeForBucket(transactionFee: TransactionFee, bucket: FeeBucket): Fee {
        return when (transactionFee) {
            is TransactionFee.Choosable -> when (bucket) {
                FeeBucket.SLOW -> transactionFee.minimum
                FeeBucket.FAST -> transactionFee.priority
                else -> transactionFee.normal
            }
            is TransactionFee.Single -> transactionFee.normal
        }
    }

    /**
     * Branch selection:
     *  - Gasless token path: `swapFee.transactionFeeResult is LoadedExtended && selectedFeeToken.currency is Token`
     *    → `createAndSendGaslessTransactionUseCase`.
     *  - Otherwise → `sendTransactionUseCase` with `swapFee.fee`.
     */
    @Suppress("LongMethod", "CanBeNonNullable")
    private suspend fun onSwapCex(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        swapFee: SwapFee?,
        swapProvider: SwapProvider,
        expressOperationType: ExpressOperationType,
        isTangemPayWithdrawal: Boolean,
    ): SwapTransactionState {
        val fromNetworkAddress = fromSwapCurrencyStatus.status.value.networkAddress
        val fromAddress = fromNetworkAddress?.defaultAddress?.value.orEmpty()
        val toNetworkAddress = toSwapCurrencyStatus.status.value.networkAddress
        val toAddress = toNetworkAddress?.defaultAddress?.value.orEmpty()
        val exchangeData = repository.getExchangeData(
            userWallet = fromSwapCurrencyStatus.userWallet,
            fromContractAddress = fromSwapCurrencyStatus.currency.getContractAddress(),
            fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
            toContractAddress = toSwapCurrencyStatus.currency.getContractAddress(),
            fromAddress = fromAddress,
            toNetwork = toSwapCurrencyStatus.currency.network.rawId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toSwapCurrencyStatus.currency.decimals,
            providerId = swapProvider.providerId,
            rateType = RateType.FLOAT,
            expressOperationType = expressOperationType,
            toAddress = toAddress,
            refundAddress = fromNetworkAddress?.defaultAddress?.value,
            refundExtraId = null, // currently always null,
        ).getOrElse { error -> return SwapTransactionState.Error.ExpressError(error) }

        val exchangeDataCex =
            exchangeData.transaction as? ExpressTransactionModel.CEX ?: return SwapTransactionState.Error.UnknownError

        if (isTangemPayWithdrawal) {
            return SwapTransactionState.TangemPayWithdrawalData(
                cryptoAmount = amount.value,
                cryptoCurrencyId = requireNotNull(fromSwapCurrencyStatus.currency.id.rawCurrencyId),
                cexAddress = exchangeDataCex.txTo,
                fromAmount = amountFormatter.formatSwapAmountToUI(
                    amount,
                    fromSwapCurrencyStatus.currency.symbol,
                ),
                fromAmountValue = amount.value,
                toAmount = amountFormatter.formatSwapAmountToUI(
                    exchangeData.toTokenAmount,
                    toSwapCurrencyStatus.currency.symbol,
                ),
                toAmountValue = exchangeData.toTokenAmount.value,
                storeData = SwapTransactionState.TangemPayWithdrawalData.StoreTransactionData(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    amount = amount,
                    swapProvider = swapProvider,
                    swapDataModel = exchangeData,
                    txExternalUrl = exchangeDataCex.externalTxUrl,
                    txExternalId = exchangeDataCex.externalTxId,
                    averageDuration = null,
                ),
                exchangeData = TangemPayWithdrawExchangeState(
                    txId = exchangeDataCex.txId,
                    fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
                    fromAddress = fromNetworkAddress?.defaultAddress?.value.orEmpty(),
                    payInAddress = exchangeData.transaction.txTo,
                    payInExtraId = exchangeDataCex.txExtraId,
                ),
            )
        }

        val userWallet = fromSwapCurrencyStatus.userWallet
        if (userWallet is UserWallet.Cold && isDemoCardUseCase(userWallet.scanResponse.card.cardId)) {
            return SwapTransactionState.Error.UnknownError
        }
        val fee = requireNotNull(swapFee)
        val txData = createTransferTransactionUseCase(
            amount = amount.value.convertToSdkAmount(fromSwapCurrencyStatus.status),
            fee = fee.fee,
            memo = exchangeDataCex.txExtraId,
            destination = exchangeDataCex.txTo,
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            network = fromSwapCurrencyStatus.currency.network,
        ).getOrElse { error ->
            TangemLogger.e("Failed to create swap CEX tx data", error)
            return SwapTransactionState.Error.UnknownError
        }

        if (txData.extras == null && exchangeDataCex.txExtraId != null) {
            return SwapTransactionState.Error.UnknownError
        }

        val isGaslessToken = fee.selectedFeeToken.currency is CryptoCurrency.Token &&
            fee.transactionFeeResult is TransactionFeeResult.LoadedExtended
        val result = if (isGaslessToken) {
            createAndSendGaslessTransactionUseCase.invoke(
                transactionData = txData,
                userWallet = userWallet,
                fee = fee.transactionFeeResult.fee,
            )
        } else {
            sendTransactionUseCase(
                txData = txData,
                userWallet = userWallet,
                network = fromSwapCurrencyStatus.currency.network,
            )
        }

        val cexNetworkAddress = fromSwapCurrencyStatus.status.value.networkAddress
        val cexFromAddress = cexNetworkAddress?.defaultAddress?.value.orEmpty()
        return result.fold(
            ifLeft = { error -> SwapTransactionState.Error.TransactionError(error) },
            ifRight = { txHash ->
                repository.exchangeSent(
                    userWallet = userWallet,
                    txId = exchangeDataCex.txId,
                    fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
                    fromAddress = cexFromAddress,
                    payInAddress = getPayoutAddress(txData),
                    txHash = txHash,
                    payInExtraId = exchangeDataCex.txExtraId,
                )
                val timestamp = System.currentTimeMillis()
                val txExternalUrl = exchangeDataCex.externalTxUrl
                storeSwapTransaction(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    amount = amount,
                    swapProvider = swapProvider,
                    swapDataModel = exchangeData,
                    timestamp = timestamp,
                    txExternalUrl = txExternalUrl,
                    txExternalId = exchangeDataCex.externalTxId,
                )
                storeLastCryptoCurrencyId(toSwapCurrencyStatus)
                SwapTransactionState.TxSent(
                    fromAmount = amountFormatter.formatSwapAmountToUI(
                        amount,
                        fromSwapCurrencyStatus.currency.symbol,
                    ),
                    fromAmountValue = amount.value,
                    toAmount = amountFormatter.formatSwapAmountToUI(
                        exchangeData.toTokenAmount,
                        toSwapCurrencyStatus.currency.symbol,
                    ),
                    toAmountValue = exchangeData.toTokenAmount.value,
                    txHash = txHash,
                    txExternalUrl = txExternalUrl,
                    timestamp = timestamp,
                )
            },
        )
    }

    private suspend fun onSwapSolanaDex(
        provider: SwapProvider,
        swapData: SwapDataModel,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amountToSwap: String,
    ): SwapTransactionState {
        val dexTransaction = swapData.transaction as? ExpressTransactionModel.DEX
        val amountDecimal = requireNotNull(toBigDecimalOrNull(amountToSwap)) { "wrong amount format" }
        val txDataBase64 = requireNotNull(dexTransaction?.txData) { "txData is null" }
        val amount = SwapAmount(amountDecimal, fromSwapCurrencyStatus.currency.decimals)
        val compiledTransaction = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.Bytes(Base64.decode(txDataBase64, Base64.NO_WRAP)),
        )
        return handleSwapResult(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            provider = provider,
            swapData = swapData,
            amount = amount,
            txData = compiledTransaction,
            payInAddress = swapData.transaction.txTo,
        )
    }

    private suspend fun handleSwapResult(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        swapData: SwapDataModel,
        amount: SwapAmount,
        txData: TransactionData,
        payInAddress: String,
    ): SwapTransactionState {
        val result = sendTransactionUseCase(
            txData = txData,
            userWallet = fromSwapCurrencyStatus.userWallet,
            network = fromSwapCurrencyStatus.currency.network,
        )
        return result.fold(
            ifRight = { txHash ->
                finalizeDexSwapSuccess(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    provider = provider,
                    swapData = swapData,
                    amount = amount,
                    txHash = txHash,
                    payInAddress = payInAddress,
                )
            },
            ifLeft = { SwapTransactionState.Error.TransactionError(it) },
        )
    }

    /**
     * Shared success path for DEX (single-tx and integrated approve+swap multi-tx). Notifies the
     * exchange backend, stores the transaction locally for status tracking, records the last-used
     * crypto currency id, and returns the [SwapTransactionState.TxSent] payload.
     */
    @Suppress("LongParameterList")
    private suspend fun finalizeDexSwapSuccess(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        provider: SwapProvider,
        swapData: SwapDataModel,
        amount: SwapAmount,
        txHash: String,
        payInAddress: String,
    ): SwapTransactionState.TxSent {
        val networkAddress = fromSwapCurrencyStatus.status.value.networkAddress
        val fromAddress = networkAddress?.defaultAddress?.value.orEmpty()
        repository.exchangeSent(
            userWallet = fromSwapCurrencyStatus.userWallet,
            txId = swapData.transaction.txId,
            fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
            fromAddress = fromAddress,
            payInAddress = payInAddress,
            txHash = txHash,
            payInExtraId = swapData.transaction.txExtraId,
        )
        val timestamp = System.currentTimeMillis()
        storeSwapTransaction(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            amount = amount,
            swapProvider = provider,
            swapDataModel = swapData,
            timestamp = timestamp,
        )
        storeLastCryptoCurrencyId(fromSwapCurrencyStatus)
        return SwapTransactionState.TxSent(
            fromAmount = amountFormatter.formatSwapAmountToUI(
                amount,
                fromSwapCurrencyStatus.currency.symbol,
            ),
            fromAmountValue = amount.value,
            toAmount = amountFormatter.formatSwapAmountToUI(
                swapData.toTokenAmount,
                toSwapCurrencyStatus.currency.symbol,
            ),
            toAmountValue = swapData.toTokenAmount.value,
            txHash = txHash,
            timestamp = timestamp,
        )
    }

    private fun createDexTxExtras(data: String, network: Network, gasLimit: Int?): TransactionExtras {
        return createTransactionExtrasUseCase(
            data = data,
            network = network,
            gasLimit = gasLimit?.toBigInteger(),
        ).getOrNull() ?: error("failed to create extras")
    }

    override suspend fun storeSwapTransaction(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        swapProvider: SwapProvider,
        swapDataModel: SwapDataModel,
        timestamp: Long,
        txExternalUrl: String?,
        txExternalId: String?,
        averageDuration: Int?,
    ) {
        swapTransactionRepository.storeTransaction(
            fromUserWalletId = fromSwapCurrencyStatus.userWalletId,
            toUserWalletId = toSwapCurrencyStatus.userWalletId,
            fromCryptoCurrency = fromSwapCurrencyStatus.currency,
            toCryptoCurrency = toSwapCurrencyStatus.currency,
            fromAccount = fromSwapCurrencyStatus.account,
            toAccount = toSwapCurrencyStatus.account,
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
                    averageDuration = averageDuration,
                ),
            ),
        )
    }

    /**
     * Delegates to [DexSwapFeeCalculator] / [CexSwapFeeCalculator] and wraps the result in a [SwapFee].
     * The only fee load entry point used by the swap feature;
     *
     * See `SwapInteractor.loadSwapFee` for the full contract.
     */
    @Suppress("LongParameterList")
    override suspend fun loadSwapFee(
        quotesLoadedState: SwapState.QuotesLoadedState,
        fromStatus: SwapCurrencyStatus,
        toStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        swapData: SwapDataModel?,
        selectedFeeToken: CryptoCurrencyStatus?,
        isGasless: Boolean,
        txType: ExpressTxType?,
    ): Either<GetFeeError, SwapFee> = either {
        if (amount.value.signum() == 0) {
            raise(GetFeeError.UnknownError)
        }
        return when (resolveQuoteFlow(quotesLoadedState.swapProvider, txType)) {
            ResolvedFlow.DexLike -> loadDexSwapFee(
                fromStatus = fromStatus,
                swapData = swapData,
                selectedFeeToken = selectedFeeToken,
                permissionState = quotesLoadedState.permissionState,
            )
            ResolvedFlow.CexLike -> loadCexSwapFee(
                fromStatus = fromStatus,
                amount = amount,
                selectedFeeToken = selectedFeeToken,
                isGasless = isGasless,
            )
        }
    }

    /**
     * DEX branch of [loadSwapFee]. Pulls the cached `ExpressTransactionModel.DEX`
     * out of [swapData] and hands it to [DexSwapFeeCalculator]. Maps [ExpressDataError] →
     * `Left(GetFeeError.UnknownError)` to keep the unified surface a single error type, matching
     * what the legacy `loadFeeForSwapTransaction` overload 2 does for DEX failures (line 1027 of
     * the original code).
     */
    private suspend fun loadDexSwapFee(
        fromStatus: SwapCurrencyStatus,
        swapData: SwapDataModel?,
        selectedFeeToken: CryptoCurrencyStatus?,
        permissionState: PermissionDataState,
    ): Either<GetFeeError, SwapFee> {
        val transaction = swapData?.transaction as? ExpressTransactionModel.DEX
            ?: return GetFeeError.UnknownError.left()

        // If the integrated-approve simulation already failed for this context, skip the
        // simulated estimation entirely and use the plain getFee path (legacy separate-approval flow).
        val effectivePermissionState = if (
            permissionState is PermissionDataState.PermissionSettings &&
            hasIntegratedApprovalFallenBack(fromStatus, permissionState.spenderAddress)
        ) {
            PermissionDataState.Empty
        } else {
            permissionState
        }

        val dexFeeResultEither = if (fromStatus.isYieldSwapActive && fromStatus.currency is CryptoCurrency.Token) {
            val network = (fromStatus.currency as CryptoCurrency.Token).network
            val yieldModuleAddress = yieldModuleAddressProvider.getOrFetch(fromStatus.userWalletId, network)
            // TODO YIELD [REDACTED_TASK_KEY]
            dexSwapFeeCalculator.calculateYield(
                fromSwapCurrencyStatus = fromStatus,
                transaction = transaction,
                yieldModuleAddress = yieldModuleAddress,
            )
        } else {
            dexSwapFeeCalculator.calculate(
                fromSwapCurrencyStatus = fromStatus,
                transaction = transaction,
                selectedToken = selectedFeeToken,
                permissionState = effectivePermissionState,
            )
        }

        return dexFeeResultEither.fold(
            ifLeft = { error -> error.left() },
            ifRight = { dexFeeResult ->
                val feeToken = selectedFeeToken
                    ?: resolveNativeFeeTokenStatus(fromStatus)
                    ?: return@fold GetFeeError.UnknownError.left()
                SwapFeeFactory.from(
                    transactionFeeResult = dexFeeResult.transactionFee,
                    selectedFeeToken = feeToken,
                    otherNativeFee = dexFeeResult.otherNativeFee,
                    feeBucket = FeeBucket.MARKET,
                ).right()
            },
        )
    }

    private suspend fun createYieldSwapDexTransaction(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        swapData: SwapDataModel,
        dexCallData: String,
        amount: BigDecimal,
        fee: Fee,
        spenderAddress: String,
    ): Either<Throwable, TransactionData.Uncompiled> {
        val fromCurrency = fromSwapCurrencyStatus.currency as CryptoCurrency.Token
        val network = fromCurrency.network
        val yieldModuleAddress = yieldModuleAddressProvider.getOrFetch(fromSwapCurrencyStatus.userWalletId, network)
            ?: return Either.Left(IllegalStateException("Yield module address is not available for ${network.id}"))
        val wrappedCallData = dexSwapFeeCalculator.buildYieldSwapCallData(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            txTo = swapData.transaction.txTo,
            dexCallData = dexCallData,
            amount = amount,
            spenderAddress = spenderAddress,
        )
        val txExtras = createTransactionExtrasUseCase(
            callData = wrappedCallData,
            network = network,
            gasLimit = fee.getGasLimit()?.toBigInteger(),
        ).getOrNull() ?: error("Failed to create yield swap extras")

        return createTransactionUseCase(
            amount = createNativeAmountForDex("0", network),
            fee = fee,
            memo = null,
            destination = yieldModuleAddress,
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            network = network,
            txExtras = txExtras,
        )
    }

    /**
     * CEX branch of [loadSwapFee]. Native-fallback behavior is preserved: when
     * [selectedFeeToken] is null the gasless use case (invoked inside [CexSwapFeeCalculator])
     * decides native vs token. The resulting `SwapFee.selectedFeeToken` is the explicit choice
     * if provided, otherwise the native coin status of the from-token's network.
     */
    private suspend fun loadCexSwapFee(
        fromStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        selectedFeeToken: CryptoCurrencyStatus?,
        isGasless: Boolean,
    ): Either<GetFeeError, SwapFee> {
        return cexSwapFeeCalculator.calculate(
            userWallet = fromStatus.userWallet,
            fromSwapCurrencyStatus = fromStatus,
            amount = amount.value,
            selectedFeeToken = selectedFeeToken,
            isGasless = isGasless,
        ).fold(
            ifLeft = { it.left() },
            ifRight = { cexFeeResult ->
                val feeToken = selectedFeeToken
                    ?: resolveNativeFeeTokenStatus(fromStatus)
                    ?: return@fold GetFeeError.UnknownError.left()
                SwapFeeFactory.from(
                    transactionFeeResult = cexFeeResult.transactionFee,
                    selectedFeeToken = feeToken,
                    otherNativeFee = BigDecimal.ZERO,
                    feeBucket = FeeBucket.MARKET,
                ).right()
            },
        )
    }

    override fun integratedApprovalFallback(fromSwapCurrencyStatus: SwapCurrencyStatus, spenderAddress: String) {
        integratedApprovalFallbackContexts.add(
            element = IntegratedApprovalFallbackKey(
                userWalletId = fromSwapCurrencyStatus.userWalletId,
                fromCurrencyId = fromSwapCurrencyStatus.currency.id,
                spenderAddress = spenderAddress,
            ),
        )
    }

    /**
     * Builds the approval [TransactionData.Uncompiled] for the integrated
     * approval + swap path and loads its [TransactionFee] via [getFeeUseCase]. The amount honors
     * [ApproveType]: `UNLIMITED` → null (unbounded allowance), `LIMITED` → the swap amount.
     */
    override suspend fun loadIntegratedApprovalData(
        fromStatus: SwapCurrencyStatus,
        spenderAddress: String,
        approveType: ApproveType,
        approvalAmount: BigDecimal,
    ): Either<GetFeeError, IntegratedApprovalData> = either {
        val tokenCurrency = fromStatus.currency as? CryptoCurrency.Token
            ?: raise(GetFeeError.DataError(IllegalStateException("Integrated approval requires a Token from-currency")))

        val amountForApprove: BigDecimal? = when (approveType) {
            ApproveType.LIMITED -> approvalAmount
            ApproveType.UNLIMITED -> null
        }

        val approvalTx = createApprovalTransactionUseCase(
            userWalletId = fromStatus.userWalletId,
            cryptoCurrencyStatus = fromStatus.status,
            amount = amountForApprove,
            contractAddress = tokenCurrency.contractAddress,
            spenderAddress = spenderAddress,
        ).getOrElse { error ->
            TangemLogger.e("loadIntegratedApprovalData: failed to create approval tx", error)
            raise(GetFeeError.DataError(error))
        }

        val approvalFee = getFeeUseCase(
            transactionData = approvalTx,
            userWallet = fromStatus.userWallet,
            network = fromStatus.currency.network,
        ).bind()

        IntegratedApprovalData(
            approvalTransaction = approvalTx,
            approvalFee = approvalFee,
            approveType = approveType,
        )
    }

    /**
     * Resolves the native-coin [CryptoCurrencyStatus] for the from-token's network.
     * Used as the default `selectedFeeToken` of [SwapFee] when the caller did not provide an
     * explicit choice. Mirrors how `SwapModel.updateFeePaidCryptoCurrencyFor` populates
     * `dataState.feePaidCryptoCurrency`.
     */
    private suspend fun resolveNativeFeeTokenStatus(fromStatus: SwapCurrencyStatus): CryptoCurrencyStatus? {
        return getFeePaidCryptoCurrencyStatusSyncUseCase(
            userWalletId = fromStatus.userWalletId,
            cryptoCurrencyStatus = fromStatus.status,
        ).getOrNull() ?: run {
            val feeNetwork = fromStatus.currency.network

            val feePaidCurrency = currenciesRepository.getFeePaidCurrency(
                fromStatus.userWalletId,
                feeNetwork,
            )

            val (feeCurrency, balance) = when (feePaidCurrency) {
                FeePaidCurrency.Coin -> currenciesRepository.createCoinCurrency(feeNetwork) to
                    walletManagersFacade.getNativeTokenBalance(
                        userWalletId = fromStatus.userWalletId,
                        networkId = feeNetwork.rawId,
                        derivationPath = feeNetwork.derivationPath.value,
                    )
                is FeePaidCurrency.Token -> currenciesRepository.createTokenCurrency(
                    userWalletId = fromStatus.userWalletId,
                    contractAddress = feePaidCurrency.contractAddress,
                    networkId = feeNetwork.rawId,
                ) to feePaidCurrency.balance
                is FeePaidCurrency.FeeResource,
                FeePaidCurrency.SameCurrency,
                -> fromStatus.currency to fromStatus.status.value.amount
            }

            val feeCurrencyRawID = feeCurrency.id.rawCurrencyId ?: return@run null
            val quote = quotesRepository.getMultiQuoteSyncOrNull(setOf(feeCurrencyRawID))
                ?.firstOrNull()?.value as? QuoteStatus.Data

            CryptoCurrencyStatus(
                currency = feeCurrency,
                value = if (quote == null) {
                    CryptoCurrencyStatus.NoQuote(
                        amount = balance.orZero(),
                        stakingBalance = null,
                        yieldSupplyStatus = null,
                        hasCurrentNetworkTransactions = false,
                        pendingTransactions = emptySet(),
                        networkAddress = fromStatus.status.value.networkAddress ?: return@run null,
                        sources = CryptoCurrencyStatus.Sources(),
                    )
                } else {
                    CryptoCurrencyStatus.Loaded(
                        amount = balance.orZero(),
                        fiatAmount = quote.fiatRate.multiply(balance),
                        fiatRate = quote.fiatRate,
                        priceChange = quote.priceChange,
                        stakingBalance = null,
                        yieldSupplyStatus = null,
                        hasCurrentNetworkTransactions = false,
                        pendingTransactions = emptySet(),
                        networkAddress = fromStatus.status.value.networkAddress ?: return@run null,
                        sources = CryptoCurrencyStatus.Sources(),
                    )
                },
            )
        }
    }

    /**
     * Patches an existing [SwapState.QuotesLoadedState] with a freshly resolved [SwapFee].
     * See [SwapInteractor.applySwapFee] for the full contract.
     *
     * Numeric fee used for downstream computation:
     *  - If `fee.selectedFeeToken.currency` is a token → `0` for the balance / include-fee math
     *    when the fee currency differs from the from-token (matches legacy `manageWarnings`
     *    semantics at line 422 of the pre-Phase-4 code).
     *  - Otherwise → `fee.fee.amount.value + fee.otherNativeFee` (the bridge-aware native fee).
     *
     * The fee is folded into a single [SwapBalanceStatus] by [computeBalanceStatus], which is
     * then assigned to `preparedSwapConfigState.balanceStatus`.
     */
    override suspend fun applySwapFee(
        state: SwapState.QuotesLoadedState,
        fee: SwapFee,
        lastReducedBalanceBy: BigDecimal,
    ): SwapState.QuotesLoadedState {
        val fromSwapCurrencyStatus = state.fromTokenInfo.swapCurrencyStatus
        val amount = state.fromTokenInfo.tokenAmount
        val isFeeInToken = fee.selectedFeeToken.currency is CryptoCurrency.Token
        val nativeFee = (fee.fee.amount.value ?: BigDecimal.ZERO) + fee.otherNativeFee

        // Mirrors legacy manageWarnings: token-fee paths skip the native deduction.
        val warningsFee = if (isFeeInToken && fromSwapCurrencyStatus.currency.id != fee.selectedFeeToken.currency.id) {
            BigDecimal.ZERO
        } else {
            nativeFee
        }

        val balanceStatus = computeBalanceStatus(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            amount = amount,
            reduceBalanceBy = lastReducedBalanceBy,
            feeValue = nativeFee,
            selectedFeeToken = fee.selectedFeeToken,
            provider = state.swapProvider,
        )
        val currencyCheck = manageWarnings(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            amount = amount,
            fee = warningsFee,
            balanceStatus = balanceStatus,
        )
        val validationResult = manageTransactionValidationWarnings(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            amount = amount,
            feeValue = nativeFee,
        )
        val minAdaValue = (fee.fee as? Fee.CardanoToken)?.minAdaValue

        return state.copy(
            preparedSwapConfigState = state.preparedSwapConfigState.copy(
                balanceStatus = balanceStatus,
            ),
            currencyCheck = currencyCheck,
            validationResult = validationResult,
            minAdaValue = minAdaValue,
        )
    }

    /**
     * Decision tree (matches the user-approved derivation table plus the implicit Token-fee sub-case):
     *  1. `Included` from `getIncludeFeeInAmountInternal` ⇒ [SwapBalanceStatus.FeeAdjustedAmount].
     *  2. `!isBalanceEnough` (from-token balance can't cover the amount itself) ⇒
     *     [SwapBalanceStatus.InsufficientAmount].
     *  3. `feeBalanceState is NotEnough` ⇒ [SwapBalanceStatus.InsufficientFee]. This catches:
     *     - From-token is a Token, native balance can't cover the fee
     *       (legacy `includeFeeInAmount=BalanceNotEnough` for the Token branch).
     *     - From-token is a Coin and `balance - amount < fee`
     *       (legacy `feeState=NotEnough && includeFeeInAmount=Excluded`).
     *  4. Otherwise ⇒ [SwapBalanceStatus.Sufficient].
     *
     * The legacy ambiguity where `BalanceNotEnough` meant "amount > balance" for Coin
     * from-currencies but "fee > native balance" for Token from-currencies is resolved here
     * by consulting `isBalanceEnough` (amount-alone check) directly.
     */
    private suspend fun computeBalanceStatus(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        feeValue: BigDecimal,
        selectedFeeToken: CryptoCurrencyStatus?,
        provider: SwapProvider,
    ): SwapBalanceStatus {
        when (provider.type) {
            ExchangeProviderType.CEX -> {
                val includeStatus = getIncludeFeeInAmountInternal(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    amount = amount,
                    reduceBalanceBy = reduceBalanceBy,
                    feeValue = feeValue,
                    selectedFeeToken = selectedFeeToken,
                )
                if (includeStatus is IncludeFeeInAmountInternal.Included) {
                    return SwapBalanceStatus.FeeAdjustedAmount(adjustedAmount = includeStatus.amountSubtractFee)
                }
            }
            ExchangeProviderType.DEX,
            ExchangeProviderType.DEX_BRIDGE,
            -> Unit
        }

        val isAmountAlone = isBalanceEnough(fromSwapCurrencyStatus, amount, fee = feeValue)
        if (!isAmountAlone) {
            return SwapBalanceStatus.InsufficientAmount
        }

        val feeBalanceState = getFeeBalanceState(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            fee = feeValue,
            spendAmount = amount,
            selectedFeeToken = selectedFeeToken,
        )
        return when (feeBalanceState) {
            is FeeBalanceState.Enough -> SwapBalanceStatus.Sufficient
            is FeeBalanceState.NotEnough -> SwapBalanceStatus.InsufficientFee(
                feeCurrencyName = feeBalanceState.currencyName,
                feeCurrencySymbol = feeBalanceState.currencySymbol,
            )
        }
    }

    private suspend fun storeLastCryptoCurrencyId(swapCurrencyStatus: SwapCurrencyStatus) {
        swapTransactionRepository.storeLastSwappedCryptoCurrencyId(
            userWalletId = swapCurrencyStatus.userWalletId,
            cryptoCurrencyId = swapCurrencyStatus.currency.id,
        )
    }

    override fun getTokenBalance(token: CryptoCurrencyStatus): SwapAmount {
        return SwapAmount(token.value.amount.orZero(), token.currency.decimals)
    }

    private suspend fun createEmptyAmountState(): SwapState {
        val appCurrency = getSelectedAppCurrencyUseCase.unwrap()
        return SwapState.EmptyAmountState(
            zeroAmountEquivalent = stringReference(
                BigDecimal.ZERO.format {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                    )
                },
            ),
        )
    }

    @Suppress("LongMethod")
    private suspend fun getQuotesState(
        provider: SwapProvider,
        quoteDataModel: Either<ExpressDataError, QuoteModel>,
        amount: SwapAmount,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        isAllowedToSpend: Boolean,
        quoteBalanceStatus: SwapBalanceStatus,
    ): SwapState {
        return quoteDataModel.fold(
            ifRight = { quoteModel ->
                val swapState = updateBalances(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    fromTokenAmount = amount,
                    toTokenAmount = quoteModel.toTokenAmount,
                    swapData = null,
                    provider = provider,
                ).copy(
                    currencyCheck = manageWarnings(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        amount = amount,
                        fee = BigDecimal.ZERO,
                        balanceStatus = quoteBalanceStatus,
                    ),
                    validationResult = manageTransactionValidationWarnings(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        amount = amount,
                        feeValue = BigDecimal.ZERO,
                    ),
                    minAdaValue = null,
                    txType = quoteModel.txType,
                )

                when (resolveQuoteFlow(provider, quoteModel.txType)) {
                    ResolvedFlow.DexLike -> {
                        val state = updatePermissionState(
                            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                            quotesLoadedState = swapState,
                            isAllowedToSpend = isAllowedToSpend,
                            swapAmount = amount,
                            quoteModel = quoteModel,
                        )
                        if (state !is SwapState.QuotesLoadedState) return state
                        state.copy(
                            preparedSwapConfigState = state.preparedSwapConfigState.copy(
                                balanceStatus = quoteBalanceStatus,
                            ),
                        )
                    }
                    ResolvedFlow.CexLike -> {
                        swapState.copy(
                            permissionState = PermissionDataState.Empty,
                            preparedSwapConfigState = PreparedSwapConfigState(
                                balanceStatus = quoteBalanceStatus,
                                hasOutgoingTransaction = hasOutgoingTransaction(fromSwapCurrencyStatus.status),
                            ),
                        )
                    }
                }
            },
            ifLeft = { error ->
                createSwapErrorWith(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    amount = amount,
                    balanceStatus = quoteBalanceStatus,
                    expressDataError = error,
                )
            },
        )
    }

    private suspend fun createSwapErrorWith(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        balanceStatus: SwapBalanceStatus,
        expressDataError: ExpressDataError,
    ): SwapState.SwapError {
        val rates = getQuotes(fromSwapCurrencyStatus.currency.id)
        val fromTokenSwapInfo = TokenSwapInfo(
            swapCurrencyStatus = fromSwapCurrencyStatus,
            tokenAmount = amount,
            amountFiat = rates[fromSwapCurrencyStatus.currency.id]?.fiatRate?.multiply(amount.value) ?: BigDecimal.ZERO,
        )
        return SwapState.SwapError(fromTokenSwapInfo, expressDataError, balanceStatus)
    }

    /**
     * Branches:
     *  - [selectedFeeToken] is the same currency as [fromSwapCurrencyStatus] (and not a coin) →
     *    same-currency-token path: balance check on the from-token's own balance.
     *  - Otherwise → native-fee branch via [getIncludeFeeInAmountForNative].
     *
     * Used both by [loadCexQuoteData] (with `feeValue = ZERO` at quote stage) and by
     * [computeBalanceStatus] (with the actual fee once the selector resolves).
     */
    private suspend fun getIncludeFeeInAmountInternal(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        feeValue: BigDecimal,
        selectedFeeToken: CryptoCurrencyStatus? = null,
    ): IncludeFeeInAmountInternal {
        return if (fromSwapCurrencyStatus.account is Account.Payment) {
            val fromBalance = fromSwapCurrencyStatus.status.value.amount.orZero()
            if (amount.value > fromBalance) {
                IncludeFeeInAmountInternal.BalanceNotEnough
            } else {
                IncludeFeeInAmountInternal.Excluded
            }
        } else {
            val isFeeInSameCurrencyToken = selectedFeeToken != null &&
                fromSwapCurrencyStatus.currency.id == selectedFeeToken.currency.id &&
                selectedFeeToken.currency is CryptoCurrency.Token

            if (isFeeInSameCurrencyToken) {
                // we have a token selected for fee payment the same as sending token
                val fromBalance = fromSwapCurrencyStatus.status.value.amount
                val reducedBalance = fromBalance?.minus(reduceBalanceBy).orZero()
                when {
                    amount.value > reducedBalance -> IncludeFeeInAmountInternal.BalanceNotEnough
                    amount.value + feeValue <= reducedBalance -> IncludeFeeInAmountInternal.Excluded
                    else -> {
                        if (feeValue < amount.value) {
                            IncludeFeeInAmountInternal.Included(
                                amountSubtractFee = SwapAmount(
                                    value = reducedBalance - feeValue,
                                    decimals = fromSwapCurrencyStatus.currency.decimals,
                                ),
                            )
                        } else {
                            IncludeFeeInAmountInternal.Excluded
                        }
                    }
                }
            } else {
                getIncludeFeeInAmountForNative(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    amount = amount,
                    reduceBalanceBy = reduceBalanceBy,
                    feeValue = feeValue,
                )
            }
        }
    }

    private suspend fun getIncludeFeeInAmountForNative(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        feeValue: BigDecimal,
    ): IncludeFeeInAmountInternal {
        return when (val feePaidCurrency = getFeePaidCurrency(fromSwapCurrencyStatus)) {
            is FeePaidCurrency.Token -> {
                if (feePaidCurrency.balance > feeValue) {
                    IncludeFeeInAmountInternal.Excluded
                } else {
                    IncludeFeeInAmountInternal.BalanceNotEnough
                }
            }
            else -> getIncludeFeeAmountForCoinFee(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                amount = amount,
                reduceBalanceBy = reduceBalanceBy,
                feeValue = feeValue,
            )
        }
    }

    private suspend fun getIncludeFeeAmountForCoinFee(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        reduceBalanceBy: BigDecimal,
        feeValue: BigDecimal,
    ): IncludeFeeInAmountInternal {
        val networkId = fromSwapCurrencyStatus.currency.network.rawId
        val tokenForFeeBalance = walletManagersFacade.getNativeTokenBalance(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            networkId = networkId,
            derivationPath = fromSwapCurrencyStatus.currency.network.derivationPath.value,
        )
        val reducedBalance = tokenForFeeBalance - reduceBalanceBy
        val amountWithFee = amount.value + feeValue
        return when {
            fromSwapCurrencyStatus.currency is CryptoCurrency.Token -> {
                if (feeValue > reducedBalance || reducedBalance.signum() == 0) {
                    IncludeFeeInAmountInternal.BalanceNotEnough
                } else {
                    IncludeFeeInAmountInternal.Excluded
                }
            }
            amount.value > reducedBalance -> {
                IncludeFeeInAmountInternal.BalanceNotEnough
            }
            amountWithFee <= reducedBalance -> {
                IncludeFeeInAmountInternal.Excluded
            }
            else -> {
                if (feeValue < amount.value) {
                    val nativeCoinDecimals =
                        Blockchain.fromNetworkId(networkId)?.decimals() ?: error("Blockchain not found")
                    IncludeFeeInAmountInternal.Included(
                        amountSubtractFee = SwapAmount(
                            reducedBalance - feeValue,
                            nativeCoinDecimals,
                        ),
                    )
                } else {
                    IncludeFeeInAmountInternal.BalanceNotEnough
                }
            }
        }
    }

    /**
     * DEX-swap-data loader that does not compute a fee.
     *
     * The fee is owned exclusively by the fee selector (`FeeSelectorBlockComponent`). This method
     * fetches the swap data via [SwapRepository.getExchangeData], populates `swapDataModel`, and
     * returns an initial [SwapState.QuotesLoadedState] with:
     *  - `preparedSwapConfigState.balanceStatus = SwapBalanceStatus.Pending` — transient until
     *    `applySwapFee` is called.
     *  - `currencyCheck`, `validationResult`, `minAdaValue` populated with `fee = 0` (re-derived once fee is known).
     */
    @Suppress("LongMethod", "LongParameterList")
    private suspend fun loadDexSwapDataNoFee(
        provider: SwapProvider,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        expressOperationType: ExpressOperationType,
        allowanceInfo: AllowanceInfo?,
        spenderAddress: String?,
        dexRouterSpenderAddress: String?,
    ): SwapState {
        val fromNetworkAddress = fromSwapCurrencyStatus.status.value.networkAddress
        val dexFromAddress = fromNetworkAddress?.defaultAddress?.value.orEmpty()
        val toNetworkAddress = toSwapCurrencyStatus.status.value.networkAddress
        val dexToAddress = toNetworkAddress?.defaultAddress?.value.orEmpty()
        return repository.getExchangeData(
            userWallet = fromSwapCurrencyStatus.userWallet,
            fromContractAddress = fromSwapCurrencyStatus.currency.getContractAddress(),
            fromNetwork = fromSwapCurrencyStatus.currency.network.rawId,
            toContractAddress = toSwapCurrencyStatus.currency.getContractAddress(),
            fromAddress = dexFromAddress,
            toNetwork = toSwapCurrencyStatus.currency.network.rawId,
            fromAmount = amount.toStringWithRightOffset(),
            fromDecimals = amount.decimals,
            toDecimals = toSwapCurrencyStatus.currency.decimals,
            providerId = provider.providerId,
            rateType = RateType.FLOAT,
            toAddress = dexToAddress,
            refundAddress = fromNetworkAddress?.defaultAddress?.value,
            expressOperationType = expressOperationType,
        ).map { swapData ->
            val dexTx = swapData.transaction as? ExpressTransactionModel.DEX
            if (dexTx != null && dexRouterSpenderAddress != null && dexTx.allowanceContract == null) {
                swapData.copy(transaction = dexTx.copy(allowanceContract = dexRouterSpenderAddress))
            } else {
                swapData
            }
        }.fold(
            ifRight = { swapData ->
                val preparedSwapConfigState = PreparedSwapConfigState(
                    balanceStatus = SwapBalanceStatus.Pending,
                    hasOutgoingTransaction = hasOutgoingTransaction(fromSwapCurrencyStatus.status),
                )
                val swapState = updateBalances(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    fromTokenAmount = amount,
                    toTokenAmount = swapData.toTokenAmount,
                    swapData = swapData,
                    provider = provider,
                )
                val isIntegratedApprovalNeeded = swapFeatureToggles.isSwapIntegratedApproveEnabled &&
                    allowanceInfo is AllowanceInfo.NotEnough &&
                    !hasIntegratedApprovalFallenBack(fromSwapCurrencyStatus, spenderAddress)
                swapState.copy(
                    permissionState = if (isIntegratedApprovalNeeded) {
                        PermissionDataState.PermissionSettings(
                            type = ApproveType.LIMITED,
                            spenderAddress = spenderAddress.orEmpty(),
                        )
                    } else if (
                        allowanceInfo is AllowanceInfo.NotEnough &&
                        hasIntegratedApprovalFallenBack(fromSwapCurrencyStatus, spenderAddress)
                    ) {
                        // Integrated estimation failed earlier this session — show the legacy
                        // separate-approval UI so the user approves before swapping.
                        PermissionDataState.PermissionRequired(
                            isResetApproval = false,
                            spenderAddress = spenderAddress.orEmpty(),
                        )
                    } else {
                        PermissionDataState.Empty
                    },
                    currencyCheck = manageWarnings(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        amount = amount,
                        fee = BigDecimal.ZERO,
                        balanceStatus = SwapBalanceStatus.Pending,
                    ),
                    validationResult = manageTransactionValidationWarnings(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        amount = amount,
                        feeValue = BigDecimal.ZERO,
                    ),
                    preparedSwapConfigState = preparedSwapConfigState,
                )
            },
            ifLeft = { error ->
                produceDexSwapDataError(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    error = error,
                    amount = amount,
                )
            },
        )
    }

    private suspend fun produceDexSwapDataError(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        error: ExpressDataError,
        amount: SwapAmount,
    ): SwapState.SwapError {
        val rates = getQuotes(fromSwapCurrencyStatus.currency.id)
        val fromTokenSwapInfo = TokenSwapInfo(
            swapCurrencyStatus = fromSwapCurrencyStatus,
            tokenAmount = amount,
            amountFiat = rates[fromSwapCurrencyStatus.currency.id]?.fiatRate?.multiply(amount.value) ?: BigDecimal.ZERO,
        )
        return SwapState.SwapError(
            fromTokenInfo = fromTokenSwapInfo,
            error = error,
            balanceStatus = SwapBalanceStatus.Pending,
        )
    }

    @Suppress("LongParameterList", "MaxChainedCallsOnSameLine")
    private suspend fun updateBalances(
        provider: SwapProvider,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: SwapAmount,
        toTokenAmount: SwapAmount,
        swapData: SwapDataModel?,
    ): SwapState.QuotesLoadedState {
        val fromToken = fromSwapCurrencyStatus.currency
        val toToken = toSwapCurrencyStatus.currency
        val rates = getQuotes(fromToken.id, toToken.id)
        return SwapState.QuotesLoadedState(
            fromTokenInfo = TokenSwapInfo(
                tokenAmount = fromTokenAmount,
                swapCurrencyStatus = fromSwapCurrencyStatus,
                amountFiat = rates[fromToken.id]?.fiatRate?.multiply(fromTokenAmount.value) ?: BigDecimal.ZERO,
            ),
            toTokenInfo = TokenSwapInfo(
                tokenAmount = toTokenAmount,
                swapCurrencyStatus = toSwapCurrencyStatus,
                amountFiat = rates[toToken.id]?.fiatRate?.multiply(toTokenAmount.value) ?: BigDecimal.ZERO,
            ),
            priceImpact = calculatePriceImpact(
                fromTokenAmount = fromTokenAmount.value,
                fromQuoteStatus = rates[fromToken.id],
                toTokenAmount = toTokenAmount.value,
                toQuoteStatus = rates[toToken.id],
            ),
            swapDataModel = swapData,
            swapProvider = provider,
            minAdaValue = null,
        )
    }

    private suspend fun updatePermissionState(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        swapAmount: SwapAmount,
        quotesLoadedState: SwapState.QuotesLoadedState,
        quoteModel: QuoteModel,
        isAllowedToSpend: Boolean,
    ): SwapState {
        val fromToken = fromSwapCurrencyStatus.currency
        if (isAllowedToSpend) {
            return quotesLoadedState.copy(
                permissionState = PermissionDataState.Empty,
            )
        }
        // if token balance ZERO not show permission state to avoid user to spend money for fee
        val isTokenZeroBalance = getTokenBalance(fromSwapCurrencyStatus.status).value.signum() == 0
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

        val isYieldSwap = fromSwapCurrencyStatus.isYieldSwapActive && fromToken is CryptoCurrency.Token
        val spenderAddress = if (isYieldSwap) {
            yieldModuleAddressProvider.getOrFetch(fromSwapCurrencyStatus.userWalletId, fromToken.network)
                ?: run {
                    TangemLogger.e(
                        "Yield-swap approval skipped: yield-module address unresolved for " +
                            "walletId=${fromSwapCurrencyStatus.userWalletId} network=${fromToken.network.rawId}",
                    )
                    return quotesLoadedState.copy(permissionState = PermissionDataState.Empty)
                }
        } else {
            requireNotNull(quoteModel.allowanceContract) { "spenderAddress cant be null" }
        }

        val allowanceInfo = getAllowanceInfoUseCase(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            cryptoCurrency = fromToken,
            spenderAddress = spenderAddress,
            requiredAmount = swapAmount.value,
        ).getOrNull() ?: return quotesLoadedState.copy(permissionState = PermissionDataState.Empty)

        val isIntegratedApprovalNeeded = swapFeatureToggles.isSwapIntegratedApproveEnabled &&
            allowanceInfo is AllowanceInfo.NotEnough &&
            !hasIntegratedApprovalFallenBack(fromSwapCurrencyStatus, quoteModel.allowanceContract)
        return quotesLoadedState.copy(
            permissionState = if (isIntegratedApprovalNeeded) {
                PermissionDataState.PermissionSettings(
                    type = ApproveType.LIMITED,
                    spenderAddress = spenderAddress,
                )
            } else {
                PermissionDataState.PermissionRequired(
                    isResetApproval = allowanceInfo is AllowanceInfo.ResetNeeded,
                    spenderAddress = spenderAddress,
                )
            },
        )
    }

    private fun createNativeAmountForDex(txValueAmount: String, network: Network): Amount {
        val nativeDecimals = Blockchain.fromNetworkId(network.rawId)?.decimals()
            ?: error("Blockchain not found")
        val decimalValue = txValueAmount.toBigDecimalOrNull()?.movePointLeft(nativeDecimals)
            ?: error("txValue parse error")
        return Amount(
            currencySymbol = network.currencySymbol,
            value = decimalValue,
            decimals = nativeDecimals,
        )
    }

    private fun hasOutgoingTransaction(cryptoCurrencyStatuses: CryptoCurrencyStatus): Boolean {
        return cryptoCurrencyStatuses.value.pendingTransactions.any { it.isOutgoing }
    }

    private fun Fee.getGasLimit(): Int? {
        return when (this) {
            is Fee.Ethereum -> gasLimit.toInt()
            is Fee.VeChain -> gasLimit.toInt()
            is Fee.Aptos -> gasLimit.toInt()
            is Fee.Filecoin -> gasLimit.toInt()
            else -> null
        }
    }

    private suspend fun isBalanceEnough(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: SwapAmount,
        fee: BigDecimal?,
    ): Boolean {
        val tokenBalance = getTokenBalance(fromSwapCurrencyStatus.status).value
        val feePaidCurrency = getFeePaidCurrency(fromSwapCurrencyStatus)
        return when (feePaidCurrency) {
            is FeePaidCurrency.Token -> tokenBalance >= amount.value
            else -> {
                if (fromSwapCurrencyStatus.currency is CryptoCurrency.Token) {
                    tokenBalance >= amount.value
                } else {
                    tokenBalance >= amount.value.plus(fee ?: BigDecimal.ZERO)
                }
            }
        }
    }

    private suspend fun getFeePaidCurrency(swapCurrencyStatus: SwapCurrencyStatus): FeePaidCurrency {
        return currenciesRepository.getFeePaidCurrency(
            userWalletId = swapCurrencyStatus.userWalletId,
            network = swapCurrencyStatus.currency.network,
        )
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

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private suspend fun getFeeBalanceState(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        fee: BigDecimal?,
        spendAmount: SwapAmount,
        selectedFeeToken: CryptoCurrencyStatus? = null,
    ): FeeBalanceState {
        if (fee == null) {
            return FeeBalanceState.NotEnough()
        }
        val fromCurrency = fromSwapCurrencyStatus.currency
        val percentsToFeeIncrease = BigDecimal.ONE
        // When the user explicitly picked a non-native fee token (gasless flow),
        // the balance check must verify the chosen token's balance, not the network's native coin.
        if (selectedFeeToken != null && selectedFeeToken.currency is CryptoCurrency.Token) {
            val feeTokenBalance = selectedFeeToken.value.amount ?: BigDecimal.ZERO
            return if (feeTokenBalance > fee.multiply(percentsToFeeIncrease)) {
                FeeBalanceState.Enough
            } else {
                FeeBalanceState.NotEnough(
                    currencyName = selectedFeeToken.currency.name,
                    currencySymbol = selectedFeeToken.currency.symbol,
                )
            }
        }
        return when (val feePaidCurrency = getFeePaidCurrency(fromSwapCurrencyStatus)) {
            FeePaidCurrency.Coin -> {
                val nativeTokenBalance = walletManagersFacade.getNativeTokenBalance(
                    userWalletId = fromSwapCurrencyStatus.userWalletId,
                    networkId = fromCurrency.network.rawId,
                    derivationPath = fromCurrency.network.derivationPath.value,
                )

                val balanceToCheck = when (fromCurrency) {
                    is CryptoCurrency.Token -> nativeTokenBalance
                    is CryptoCurrency.Coin -> {
                        // need to check balance minus amount only if amount to swap in native token
                        nativeTokenBalance.minus(spendAmount.value)
                    }
                }
                if (balanceToCheck > fee.multiply(percentsToFeeIncrease)) {
                    FeeBalanceState.Enough
                } else {
                    FeeBalanceState.NotEnough(
                        currencyName = fromSwapCurrencyStatus.currency.name,
                        currencySymbol = fromSwapCurrencyStatus.currency.symbol,
                    )
                }
            }
            FeePaidCurrency.SameCurrency -> {
                val balance = fromSwapCurrencyStatus.status.value.amount ?: return FeeBalanceState.NotEnough()
                if (balance.minus(spendAmount.value) > fee.multiply(percentsToFeeIncrease)) {
                    FeeBalanceState.Enough
                } else {
                    FeeBalanceState.NotEnough(
                        currencyName = fromCurrency.name,
                        currencySymbol = fromCurrency.symbol,
                    )
                }
            }
            is FeePaidCurrency.Token -> {
                if (feePaidCurrency.balance > fee.multiply(percentsToFeeIncrease)) {
                    FeeBalanceState.Enough
                } else {
                    FeeBalanceState.NotEnough(
                        currencyName = feePaidCurrency.name,
                        currencySymbol = feePaidCurrency.symbol,
                    )
                }
            }
            is FeePaidCurrency.FeeResource -> {
                val isFeeResourceEnough = currencyChecksRepository.checkIfFeeResourceEnough(
                    amount = spendAmount.value,
                    userWalletId = fromSwapCurrencyStatus.userWalletId,
                    network = fromCurrency.network,
                )

                if (isFeeResourceEnough) {
                    FeeBalanceState.Enough
                } else {
                    FeeBalanceState.NotEnough()
                }
            }
        }
    }

    private fun calculatePriceImpact(
        fromTokenAmount: BigDecimal,
        fromQuoteStatus: QuoteStatus.Data?,
        toTokenAmount: BigDecimal,
        toQuoteStatus: QuoteStatus.Data?,
    ): PriceImpact {
        if (fromQuoteStatus == null) return PriceImpact.Empty

        val toRate = toQuoteStatus?.fiatRate
        val fromTokenFiatValue = fromTokenAmount.multiply(fromQuoteStatus.fiatRate)
        val toTokenFiatValue = toRate?.let { toTokenAmount.multiply(toRate) } ?: return PriceImpact.Empty

        val value = BigDecimal.ONE - toTokenFiatValue.divide(fromTokenFiatValue, 2, RoundingMode.HALF_UP)

        val fromAmountUSD = if (fromQuoteStatus.fiatRateUSD != BigDecimal.ZERO) {
            fromTokenAmount.multiply(fromQuoteStatus.fiatRateUSD)
        } else {
            PRICE_IMPACT_AMOUNT_MIN_THRESHOLD
        }
        val toAmountUSD = if (toQuoteStatus.fiatRateUSD != BigDecimal.ZERO) {
            toTokenAmount.multiply(toQuoteStatus.fiatRateUSD)
        } else {
            PRICE_IMPACT_AMOUNT_MIN_THRESHOLD
        }
        val amountDiff = fromAmountUSD - toAmountUSD

        val amountSignificance = when {
            fromAmountUSD <= PRICE_IMPACT_AMOUNT_MIN_THRESHOLD -> PriceImpact.AmountSignificance.LOW
            fromAmountUSD > PRICE_IMPACT_AMOUNT_MAX_THRESHOLD -> PriceImpact.AmountSignificance.HIGH
            else -> PriceImpact.AmountSignificance.MEDIUM
        }

        val type = when {
            value < PRICE_IMPACT_LOW_THRESHOLD &&
                amountDiff <= PRICE_IMPACT_AMOUNT_LOW_THRESHOLD -> PriceImpact.Type.LOW
            value > PRICE_IMPACT_HIGH_THRESHOLD -> PriceImpact.Type.HIGH
            else -> PriceImpact.Type.MEDIUM
        }

        return PriceImpact(
            value = value,
            amountSignificance = amountSignificance,
            type = type,
        )
    }

    private suspend fun getQuotes(vararg ids: CryptoCurrency.ID): Map<CryptoCurrency.ID, QuoteStatus.Data> {
        val set = ids.mapNotNullTo(destination = hashSetOf(), transform = CryptoCurrency.ID::rawCurrencyId)
            .getQuotesOrEmpty()

        return ids.mapNotNull { id ->
            val found = set.find { it.rawCurrencyId == id.rawCurrencyId && it.value is QuoteStatus.Data }
                ?: return@mapNotNull null

            id to found.value as QuoteStatus.Data
        }.toMap()
    }

    private suspend fun Set<CryptoCurrency.RawID>.getQuotesOrEmpty(): Set<QuoteStatus> {
        return runSuspendCatching {
            val cachedQuotes = quotesRepository.getMultiQuoteSyncOrNull(currenciesIds = this@getQuotesOrEmpty)

            val areAllQuotesFound = cachedQuotes?.all { quote -> quote.value !is QuoteStatus.Empty } == true

            if (areAllQuotesFound) return@runSuspendCatching cachedQuotes

            val currenciesIds = if (cachedQuotes.isNullOrEmpty()) {
                this@getQuotesOrEmpty
            } else {
                cachedQuotes.mapNotNullTo(hashSetOf()) { quote ->
                    if (quote.value is QuoteStatus.Empty) quote.rawCurrencyId else null
                }
            }

            multiQuoteStatusFetcher(
                params = MultiQuoteStatusFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = null),
            )

            quotesRepository.getMultiQuoteSyncOrNull(currenciesIds = this@getQuotesOrEmpty).orEmpty()
        }.getOrElse { e ->
            TangemLogger.e("Failed to get quotes: ${e.message.orEmpty()}", e)
            emptySet()
        }
    }

    private fun isSolana(networkId: String): Boolean {
        return networkId == Blockchain.Solana.toNetworkId()
    }

    private fun getPayoutAddress(txData: TransactionData.Uncompiled): String {
        val ethereumCallData = (txData.extras as? EthereumTransactionExtras)?.callData
        return if (ethereumCallData is EthereumYieldSupplySendCallData) {
            ethereumCallData.destinationAddress
        } else {
            txData.destinationAddress
        }
    }

    // region temporary. will be removed
    private fun CryptoCurrency.getContractAddress(): String {
        return when (this) {
            is CryptoCurrency.Token -> this.contractAddress
            is CryptoCurrency.Coin -> "0"
        }
    }

    private fun ExpressProvider.toSwapProvider(): SwapProvider {
        return SwapProvider(
            providerId = providerId,
            rateTypes = rateTypes.map { rateType ->
                when (rateType) {
                    ExpressRateType.Float -> RateType.FLOAT
                    ExpressRateType.Fixed -> RateType.FIXED
                }
            },
            name = name,
            type = when (type) {
                ExpressProviderType.DEX -> ExchangeProviderType.DEX
                ExpressProviderType.CEX -> ExchangeProviderType.CEX
                ExpressProviderType.DEX_BRIDGE -> ExchangeProviderType.DEX_BRIDGE
                ExpressProviderType.ONRAMP -> error("Invalid provider type")
            },
            imageLarge = imageLarge,
            termsOfUse = termsOfUse,
            privacyPolicy = privacyPolicy,
            isRecommended = isRecommended,
            slippage = slippage,
            isExtraIdSupported = isExtraIdSupported,
        )
    }
    // endregion

    /**
     * Whether to drive the swap flow as a DEX (sign a provider-built transaction, possibly with
     * allowance) or as a CEX-style transfer (send native funds to a provider-supplied address).
     */
    private enum class ResolvedFlow { DexLike, CexLike }

    /**
     * `provider.type` is the primary gate. Inside the DEX/DEX_BRIDGE branch a quote with
     * `txType=SEND` switches to the CEX-style path; other values keep the DEX path.
     */
    private fun resolveQuoteFlow(provider: SwapProvider, quoteTxType: ExpressTxType?): ResolvedFlow =
        when (provider.type) {
            ExchangeProviderType.CEX -> ResolvedFlow.CexLike
            ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> when (quoteTxType) {
                ExpressTxType.SEND -> ResolvedFlow.CexLike
                ExpressTxType.SWAP, null -> ResolvedFlow.DexLike
            }
        }

    /**
     * Execution-stage counterpart of [resolveQuoteFlow]. For DEX/DEX_BRIDGE the shape is decided by
     * `swapData.transaction`: a DEX transaction stays on the DEX path, a CEX transaction or null
     * routes to the CEX path (null means the quote already re-routed and didn't pre-build swapData).
     */
    private fun resolveSwapDataFlow(swapProvider: SwapProvider, swapData: SwapDataModel?): ResolvedFlow =
        when (swapProvider.type) {
            ExchangeProviderType.CEX -> ResolvedFlow.CexLike
            ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> when (swapData?.transaction) {
                is ExpressTransactionModel.DEX -> ResolvedFlow.DexLike
                is ExpressTransactionModel.CEX, null -> ResolvedFlow.CexLike
            }
        }

    companion object {
        private val PRICE_IMPACT_AMOUNT_MIN_THRESHOLD = 25.toBigDecimal() // in USD
        private val PRICE_IMPACT_AMOUNT_MAX_THRESHOLD = 5000.toBigDecimal() // in USD
        private val PRICE_IMPACT_AMOUNT_LOW_THRESHOLD = 100_000.toBigDecimal() // in USD
        private val PRICE_IMPACT_LOW_THRESHOLD = 0.1.toBigDecimal() // 10%
        private val PRICE_IMPACT_HIGH_THRESHOLD = 0.5.toBigDecimal() // 50%
    }
}

/**
 * Identity of an integrated-approve fee context, used to remember that the simulated
 * swap-fee estimation failed (and hence the legacy separate-approval flow must be used). Keyed by
 * wallet + from-currency + spender; intentionally amount-independent because the estimate-override
 * failure is structural (the approval simply does not exist yet) and changing the amount cannot fix
 * it — so we must not retry the simulation on every amount change either.
 */
private data class IntegratedApprovalFallbackKey(
    val userWalletId: UserWalletId,
    val fromCurrencyId: CryptoCurrency.ID,
    val spenderAddress: String?,
) {
    companion object {
        fun of(fromSwapCurrencyStatus: SwapCurrencyStatus, spenderAddress: String?): IntegratedApprovalFallbackKey =
            IntegratedApprovalFallbackKey(
                userWalletId = fromSwapCurrencyStatus.userWalletId,
                fromCurrencyId = fromSwapCurrencyStatus.currency.id,
                spenderAddress = spenderAddress,
            )
    }
}

/**
 * [REDACTED_TASK_KEY] — internal classifier replacing the deleted public `IncludeFeeInAmount` enum.
 * Kept private to [SwapInteractorImpl]; consumers see only [SwapBalanceStatus].
 */
private sealed interface IncludeFeeInAmountInternal {
    data class Included(val amountSubtractFee: SwapAmount) : IncludeFeeInAmountInternal
    data object Excluded : IncludeFeeInAmountInternal
    data object BalanceNotEnough : IncludeFeeInAmountInternal
}

/**
 * [REDACTED_TASK_KEY] — internal classifier replacing the deleted public `SwapFeeState`. Kept private to
 * [SwapInteractorImpl]; consumers see only [SwapBalanceStatus].
 */
private sealed interface FeeBalanceState {
    data object Enough : FeeBalanceState
    data class NotEnough(
        val currencyName: String? = null,
        val currencySymbol: String? = null,
    ) : FeeBalanceState
}