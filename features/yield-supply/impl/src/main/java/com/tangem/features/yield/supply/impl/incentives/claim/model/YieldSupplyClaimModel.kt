package com.tangem.features.yield.supply.impl.incentives.claim.model

import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.domain.yield.supply.usecase.BuildYieldClaimTransactionUseCase
import com.tangem.domain.yield.supply.usecase.GetFreshYieldClaimCalldataUseCase
import com.tangem.domain.yield.supply.usecase.GetYieldRewardsSummaryUseCase
import com.tangem.domain.yield.supply.usecase.ValidateYieldClaimCalldataUseCase
import com.tangem.domain.yield.supply.usecase.YieldClaimValidationContext
import com.tangem.domain.yield.supply.usecase.YieldSupplyMarketFlowUseCase
import com.tangem.features.yield.supply.impl.common.YieldSupplyAlertFactory
import com.tangem.features.yield.supply.impl.common.formatter.YieldSupplyAmountFormatter
import com.tangem.features.yield.supply.impl.common.formatter.formatUsd
import com.tangem.features.yield.supply.impl.incentives.claim.YieldSupplyClaimComponent
import com.tangem.features.yield.supply.impl.incentives.claim.entity.YieldSupplyClaimUM
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsComponent
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsUpdateTrigger
import com.tangem.features.yield.supply.impl.subcomponents.notifications.entity.YieldSupplyNotificationData
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import javax.inject.Inject

/**
 * The network fee can only be estimated for the exact transaction, so the sheet goes all the way to a validated
 * payload before it can show a fee at all: rewards, fresh calldata, the claim gate, then the fee. Anything that
 * fails along the way leaves the fees dashed and the button disabled rather than offering a claim the backend has
 * not confirmed.
 */
@Suppress("LongParameterList")
@ModelScoped
internal class YieldSupplyClaimModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val yieldSupplyMarketFlowUseCase: YieldSupplyMarketFlowUseCase,
    private val getYieldRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase,
    private val getFreshYieldClaimCalldataUseCase: GetFreshYieldClaimCalldataUseCase,
    private val validateYieldClaimCalldataUseCase: ValidateYieldClaimCalldataUseCase,
    private val buildYieldClaimTransactionUseCase: BuildYieldClaimTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val yieldSupplyNotificationsUpdateTrigger: YieldSupplyNotificationsUpdateTrigger,
    private val yieldSupplyAlertFactory: YieldSupplyAlertFactory,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
) : Model(), YieldSupplyNotificationsComponent.ModelCallback {

    private val params: YieldSupplyClaimComponent.Params = paramsContainer.require()

    private val cryptoCurrencyStatus get() = params.cryptoCurrencyStatusFlow.value
    private val cryptoCurrency = cryptoCurrencyStatus.currency

    val feeCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(currency = cryptoCurrency, value = CryptoCurrencyStatus.Loading),
        )

    val uiState: StateFlow<YieldSupplyClaimUM>
        field = MutableStateFlow(
            YieldSupplyClaimUM(
                total = stringReference(DASH_SIGN),
                groups = persistentListOf(),
                serviceFee = null,
                networkFee = null,
                networkFeeCurrency = null,
                isServiceFeeVisible = true,
                warning = null,
                isClaimEnabled = false,
                isClaimInProgress = false,
            ),
        )

    private var appCurrency = AppCurrency.Default

    private val rewardsMapper = YieldSupplyClaimRewardsMapper(cryptoCurrency)

    /** Only the newest run may write to the sheet; a slower earlier one would otherwise land on top of it. */
    private var prepareJob: Job? = null

    /** One claim in flight: every further confirmation is dropped, and nothing is retried on its behalf. */
    private var claimJob: Job? = null

    init {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
            feeCryptoCurrencyStatusFlow.update {
                getFeePaidCryptoCurrencyStatusSyncUseCase(
                    userWalletId = params.userWallet.walletId,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                ).getOrNull() ?: cryptoCurrencyStatus
            }
            // Named only when the fee leaves the wallet in something other than the token being claimed.
            val feeCurrency = feeCryptoCurrencyStatusFlow.value.currency
            if (feeCurrency.id != cryptoCurrency.id) {
                uiState.update { it.copy(networkFeeCurrency = stringReference(feeCurrency.symbol)) }
            }
            prepare()
        }
        subscribeOnNotificationErrors()
    }

    override fun onFeeReload() {
        prepare()
    }

    fun onRetryClick() {
        prepare()
    }

    /** The close icon bypasses the sheet's drag veto, so it is held back the same way while the claim is signed. */
    fun onCloseClick() {
        if (!uiState.value.isClaimInProgress) params.onDismiss()
    }

    fun onClaimClick() {
        if (claimJob?.isActive == true) return

        claimJob = modelScope.launch { claim() }
    }

    private fun prepare() {
        if (claimJob?.isActive == true) return

        prepareJob?.cancel()
        prepareJob = modelScope.launch { loadClaim() }
    }

    /**
     * The sheet is prepared once more here, so the signed payload is the one the service vouches for right now and
     * not the one it vouched for when the sheet opened. A rejection leaves the sheet explaining itself exactly as a
     * reload would, and the card is never asked for anything.
     */
    private suspend fun claim() {
        prepareJob?.cancel()
        uiState.update { it.copy(isClaimInProgress = true) }

        val transactionData = loadClaim()
        if (transactionData == null) {
            uiState.update { it.copy(isClaimInProgress = false) }
            return
        }

        if (!isFeeCovered(transactionData)) {
            uiState.update { it.copy(isClaimInProgress = false, isClaimEnabled = false) }
            return
        }

        sendTransactionUseCase(
            txData = transactionData,
            userWallet = params.userWallet,
            network = cryptoCurrency.network,
        ).fold(
            ifLeft = { error ->
                TangemLogger.e("Yield claim was not submitted: $error")
                uiState.update { it.copy(isClaimInProgress = false) }
                yieldSupplyAlertFactory.getSendTransactionErrorState(
                    error = error,
                    popBack = params.onDismiss,
                    onFailedTxEmailClick = { errorMessage ->
                        modelScope.launch {
                            yieldSupplyAlertFactory.onFailedTxEmailClick(
                                userWallet = params.userWallet,
                                cryptoCurrency = cryptoCurrency,
                                errorMessage = errorMessage,
                            )
                        }
                    },
                )
            },
            ifRight = { _ -> params.onDismiss() },
        )
    }

    /**
     * The account the owner group pays into. Absent while accounts are off or the currency sits in none of them, and
     * the header then names the destination alone.
     */
    private suspend fun resolveStorage(): YieldSupplyClaimUM.StorageUM? {
        if (!isAccountsModeEnabledUseCase.invokeSync()) return null

        val account = getAccountCurrencyStatusUseCase
            .invokeSync(userWalletId = params.userWallet.walletId, currency = cryptoCurrency)
            .getOrNull()
            ?.account
            ?: return null

        return YieldSupplyClaimUM.StorageUM(
            name = account.accountName.toUM().value,
            icon = CryptoPortfolioIconConverter.convert(account.icon),
        )
    }

    /** @return the transaction the owner may sign, or null when the sheet could not get that far. */
    private suspend fun loadClaim(): TransactionData.Uncompiled? {
        uiState.update { it.copy(serviceFee = null, networkFee = null, warning = null, isClaimEnabled = false) }

        val context = resolveContext()
        val summary = context
            ?.let { getYieldRewardsSummaryUseCase(chainId = it.chainId, userAddress = it.userAddress) }
            ?.getOrElse { error ->
                TangemLogger.e("Yield claim sheet has no rewards to show: $error")
                null
            }
            ?.toClaimRewards()

        if (context == null || summary == null) {
            reportUnavailable()
            return null
        }

        uiState.update { state ->
            state.copy(
                total = stringReference(summary.totalNetUsd.formatUsd()),
                groups = rewardsMapper.toGroups(rewards = summary, storage = resolveStorage()),
            )
        }

        val calldata = fetchValidatedCalldata(context = context, rewards = summary)
        if (calldata == null) {
            reportUnavailable()
            return null
        }

        uiState.update { state ->
            state.copy(
                serviceFee = rewardsMapper.serviceFeeValue(
                    rewards = summary.claimable,
                    amountUsd = calldata.totalEstimatedServiceFeeUsd,
                ),
            )
        }

        return loadNetworkFee(calldata)
    }

    /**
     * Both sources publish nothing until their caches warm up, and the markets cache has no single-token fallback,
     * so waiting on them is bounded: a sheet that never resolves would shimmer forever with nothing to retry from.
     */
    private suspend fun resolveContext(): ClaimContext? {
        val token = cryptoCurrency as? CryptoCurrency.Token ?: return null

        return withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) {
            val chainId = yieldSupplyMarketFlowUseCase(token).mapNotNull { market -> market?.chainId }.firstOrNull()
            val userAddress = params.cryptoCurrencyStatusFlow
                .mapNotNull { status -> status.value.networkAddress?.defaultAddress?.value }
                .firstOrNull()

            if (chainId != null && userAddress != null) {
                ClaimContext(chainId = chainId, userAddress = userAddress)
            } else {
                null
            }
        }
    }

    /** Clears what the sheet can no longer vouch for, including fee notifications left by an earlier run. */
    private suspend fun reportUnavailable() {
        uiState.update { state ->
            state.copy(
                total = stringReference(DASH_SIGN),
                groups = persistentListOf(),
                serviceFee = stringReference(DASH_SIGN),
                networkFee = stringReference(DASH_SIGN),
                warning = YieldSupplyClaimUM.Warning.RewardsUnavailable,
                isClaimEnabled = false,
            )
        }
        yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
            data = YieldSupplyNotificationData(feeValue = null, feeError = null),
        )
    }

    private suspend fun fetchValidatedCalldata(
        context: ClaimContext,
        rewards: YieldSupplyClaimRewards,
    ): YieldClaimCalldata? {
        val calldata = getFreshYieldClaimCalldataUseCase(
            chainId = context.chainId,
            userAddress = context.userAddress,
        ).getOrElse { error ->
            TangemLogger.e("Yield claim calldata unavailable: $error")
            return null
        }

        return validateYieldClaimCalldataUseCase(
            calldata = calldata,
            context = YieldClaimValidationContext(
                userWalletId = params.userWallet.walletId,
                network = cryptoCurrency.network,
                chainId = context.chainId,
                userAddress = context.userAddress,
                moduleAddress = rewards.moduleAddress,
                supportedRewardTokens = rewards.supportedRewardTokens,
            ),
        ).getOrElse { error ->
            TangemLogger.e("Yield claim rejected before signing: $error")
            null
        }
    }

    private suspend fun loadNetworkFee(calldata: YieldClaimCalldata): TransactionData.Uncompiled? {
        val transactionData = buildYieldClaimTransactionUseCase(
            userWalletId = params.userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            calldata = calldata,
            fee = null,
        ).getOrElse { error ->
            TangemLogger.e("Yield claim transaction could not be built: $error")
            null
        }

        if (transactionData == null) {
            uiState.update { it.copy(networkFee = stringReference(DASH_SIGN)) }
            return null
        }

        return getFeeUseCase(
            userWallet = params.userWallet,
            network = cryptoCurrency.network,
            transactionData = transactionData,
        ).fold(
            ifLeft = { feeError ->
                TangemLogger.e("Yield claim fee could not be estimated: $feeError")
                uiState.update { it.copy(networkFee = stringReference(DASH_SIGN), isClaimEnabled = false) }
                yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
                    data = YieldSupplyNotificationData(feeValue = null, feeError = feeError),
                )
                null
            },
            ifRight = { fee ->
                val feeValue = fee.normal.amount.value.orZero()
                uiState.update {
                    it.copy(networkFee = feeValue.toFiat(), isClaimEnabled = true)
                }
                yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
                    data = YieldSupplyNotificationData(feeValue = feeValue, feeError = null),
                )
                transactionData.copy(fee = fee.normal)
            },
        )
    }

    /**
     * Only ever disables: unlike the sibling action sheets, a loaded fee is not on its own enough to claim here, so
     * mirroring their `isEnabled = !hasError` would re-enable the button on a sheet whose payload was rejected.
     */
    private fun subscribeOnNotificationErrors() {
        yieldSupplyNotificationsUpdateTrigger.hasErrorFlow
            .onEach { hasError -> if (hasError) uiState.update { state -> state.copy(isClaimEnabled = false) } }
            .launchIn(modelScope)

        // Only this one error drops the service fee: what Tangem would take out of the rewards says nothing useful
        // next to a transaction the owner cannot pay for. A fee that merely failed to load leaves it a known number.
        yieldSupplyNotificationsUpdateTrigger.cannotCoverFeeFlow
            .onEach { cannotCoverFee -> uiState.update { state -> state.copy(isServiceFeeVisible = !cannotCoverFee) } }
            .launchIn(modelScope)
    }

    /**
     * The sheet checked the fee it displayed; this one is the fee the card would be asked to sign for. The claim is
     * dropped rather than sent to fail on the node, and the button goes with it so the sheet is not left offering a
     * claim it has just refused.
     */
    private fun isFeeCovered(transactionData: TransactionData.Uncompiled): Boolean {
        val fee = transactionData.fee?.amount?.value.orZero()

        return fee <= feeCryptoCurrencyStatusFlow.value.value.amount.orZero()
    }

    private fun BigDecimal.toFiat(): TextReference = YieldSupplyAmountFormatter(
        feeCryptoCurrency = feeCryptoCurrencyStatusFlow.value.currency,
        appCurrency = appCurrency,
    ).invoke(feeValue = this, fiatRate = feeCryptoCurrencyStatusFlow.value.value.fiatRate, showCrypto = false)

    private data class ClaimContext(val chainId: Int, val userAddress: String)

    private companion object {
        const val RESOLVE_TIMEOUT_MILLIS = 10_000L
    }
}