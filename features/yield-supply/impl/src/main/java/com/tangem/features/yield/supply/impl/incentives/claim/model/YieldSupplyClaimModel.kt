package com.tangem.features.yield.supply.impl.incentives.claim.model

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.ui.account.AccountTitleUM
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
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.usecase.BuildYieldClaimTransactionUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.features.yield.supply.impl.common.YieldSupplyAlertFactory
import com.tangem.features.yield.supply.impl.common.formatter.YieldSupplyAmountFormatter
import com.tangem.features.yield.supply.impl.common.formatter.formatUsd
import com.tangem.features.yield.supply.impl.incentives.claim.YieldSupplyClaimComponent
import com.tangem.features.yield.supply.impl.incentives.claim.di.YieldClaimTimeSource
import com.tangem.features.yield.supply.impl.incentives.claim.entity.YieldSupplyClaimNotificationUM
import com.tangem.features.yield.supply.impl.incentives.claim.entity.YieldSupplyClaimUM
import com.tangem.features.yield.supply.impl.incentives.claim.result.entity.YieldSupplyClaimResultUM
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsComponent
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsUpdateTrigger
import com.tangem.features.yield.supply.impl.subcomponents.notifications.entity.YieldSupplyNotificationData
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * The network fee needs the exact transaction, so the payload is validated before the fee is estimated; any failure
 * leaves the fees dashed and the claim disabled.
 */
@Suppress("LongParameterList")
@ModelScoped
internal class YieldSupplyClaimModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val yieldSupplyClaimPreparer: YieldSupplyClaimPreparer,
    private val buildYieldClaimTransactionUseCase: BuildYieldClaimTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val yieldSupplyClaimTracker: YieldSupplyClaimTracker,
    private val yieldSupplyNotificationsUpdateTrigger: YieldSupplyNotificationsUpdateTrigger,
    private val yieldSupplyAlertFactory: YieldSupplyAlertFactory,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    @YieldClaimTimeSource private val timeSource: TimeSource,
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
                notification = null,
                isClaimEnabled = false,
                isClaimInProgress = false,
                isHoldToConfirmEnabled = params.userWallet.isHotWallet,
            ),
        )

    private var appCurrency = AppCurrency.Default

    private val rewardsMapper = YieldSupplyClaimRewardsMapper(cryptoCurrency)

    private var prepareJob: Job? = null

    private var claimJob: Job? = null

    /** The only transaction a confirmation may sign: the one whose fee is on screen. */
    private var preparedClaim: PreparedClaim? = null

    init {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
            prepare()
        }
        subscribeOnNotificationErrors()
    }

    override fun onFeeReload() {
        prepare()
    }

    /** The close icon bypasses the drag veto, so it checks the same flag. */
    fun onCloseClick() {
        if (!uiState.value.isClaimInProgress) params.onDismiss()
    }

    fun onClaimClick() {
        if (claimJob?.isActive == true) return

        val prepared = preparedClaim ?: return
        if (prepared.preparedAt.elapsedNow() >= PREPARATION_VALIDITY) {
            prepare()
            return
        }

        claimJob = modelScope.launch { claim(prepared) }
    }

    private fun prepare() {
        if (claimJob?.isActive == true) return

        preparedClaim = null
        prepareJob?.cancel()
        prepareJob = modelScope.launch { loadClaim() }
    }

    private suspend fun claim(prepared: PreparedClaim) {
        val transactionData = prepared.transactionData
        uiState.update { it.copy(isClaimInProgress = true) }

        if (!isFeeCovered(transactionData)) {
            uiState.update { it.copy(isClaimInProgress = false, isClaimEnabled = false) }
            return
        }

        // Scrim and back may close the sheet mid-send; a transaction already on its way is not abandoned with it, but
        // its dialog and dismissal no longer belong to the screen, which may be showing another sheet by then
        withContext(NonCancellable) {
            val result = sendTransactionUseCase(
                txData = transactionData,
                userWallet = params.userWallet,
                network = cryptoCurrency.network,
            )
            result.onRight { transactionHash -> trackClaim(transactionHash, prepared.claimedRewards) }
            if (modelScope.isActive) onTransactionSent(result)
        }
    }

    private fun onTransactionSent(result: Either<SendTransactionError, String>) {
        result.fold(
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
            // The result replaces this sheet in the slot; dismissing would close it
            ifRight = { onClaimSubmitted() },
        )
    }

    private fun onClaimSubmitted() {
        val state = uiState.value

        params.onSubmitted(
            YieldSupplyClaimResultUM(
                status = YieldSupplyClaimResultUM.Status.Submitted,
                total = state.total,
                serviceFee = state.serviceFee,
                networkFee = state.networkFee,
            ),
        )
    }

    private fun trackClaim(transactionHash: String, claimedRewards: List<YieldReward>) {
        val token = cryptoCurrency as? CryptoCurrency.Token ?: return

        yieldSupplyClaimTracker.track(
            userWalletId = params.userWallet.walletId,
            cryptoCurrency = token,
            chainId = params.chainId,
            userAddress = params.userAddress,
            transactionHash = transactionHash,
            claimedRewards = claimedRewards,
        )
    }

    private suspend fun resolveStorage(): AccountTitleUM.Account? {
        if (!isAccountsModeEnabledUseCase.invokeSync()) return null

        val account = getAccountCurrencyStatusUseCase
            .invokeSync(userWalletId = params.userWallet.walletId, currency = cryptoCurrency)
            .getOrNull()
            ?.account
            ?: return null

        return AccountTitleUM.Account(
            prefixText = TextReference.EMPTY,
            name = account.accountName.toUM().value,
            icon = CryptoPortfolioIconConverter.convert(account.icon),
        )
    }

    private suspend fun loadClaim() {
        uiState.update { it.copy(serviceFee = null, networkFee = null, notification = null, isClaimEnabled = false) }

        val summary = yieldSupplyClaimPreparer.loadRewards(chainId = params.chainId, userAddress = params.userAddress)

        if (summary == null) {
            reportUnavailable()
            return
        }

        uiState.update { state ->
            state.copy(
                total = summary.totalNetUsd.formatUsd(),
                groups = rewardsMapper.toGroups(rewards = summary, storage = resolveStorage()),
            )
        }

        val calldata = yieldSupplyClaimPreparer.loadValidatedCalldata(
            userWalletId = params.userWallet.walletId,
            network = cryptoCurrency.network,
            chainId = params.chainId,
            userAddress = params.userAddress,
            rewards = summary,
        )
        if (calldata == null) {
            reportUnavailable()
            return
        }
        val preparedAt = timeSource.markNow()

        uiState.update { state ->
            state.copy(
                serviceFee = rewardsMapper.serviceFeeValue(
                    rewards = summary.claimable,
                    amountUsd = calldata.totalEstimatedServiceFeeUsd,
                ),
            )
        }

        val transactionData = loadNetworkFee(calldata) ?: return
        preparedClaim = PreparedClaim(
            transactionData = transactionData,
            preparedAt = preparedAt,
            claimedRewards = summary.claimable,
        )
    }

    /** Also clears fee notifications left by a previous run. */
    private suspend fun reportUnavailable() {
        uiState.update { state ->
            state.copy(
                total = stringReference(DASH_SIGN),
                groups = persistentListOf(),
                serviceFee = stringReference(DASH_SIGN),
                networkFee = stringReference(DASH_SIGN),
                notification = YieldSupplyClaimNotificationUM.RewardsUnavailable(onReloadClick = ::onFeeReload),
                isClaimEnabled = false,
            )
        }
        yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
            data = YieldSupplyNotificationData(feeValue = null, feeError = null),
        )
    }

    private suspend fun loadNetworkFee(calldata: YieldClaimCalldata): TransactionData.Uncompiled? {
        val result = either {
            ensureNotNull(refreshFeeStatus()) { GetFeeError.UnknownError }
            val transactionData = buildYieldClaimTransactionUseCase(
                userWalletId = params.userWallet.walletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                calldata = calldata,
                fee = null,
            ).mapLeft { error ->
                TangemLogger.e("Yield claim transaction could not be built: $error")
                GetFeeError.UnknownError
            }.bind()
            val fee = getFeeUseCase(
                userWallet = params.userWallet,
                network = cryptoCurrency.network,
                transactionData = transactionData,
            ).onLeft { error -> TangemLogger.e("Yield claim fee could not be estimated: $error") }.bind()

            transactionData.copy(fee = fee.normal)
        }
        val transactionData = result.getOrNull()
        val feeValue = if (transactionData != null) transactionData.fee?.amount?.value.orZero() else null

        uiState.update { state ->
            state.copy(
                networkFee = feeValue?.let { value ->
                    val feeStatus = feeCryptoCurrencyStatusFlow.value
                    YieldSupplyAmountFormatter(feeCryptoCurrency = feeStatus.currency, appCurrency = appCurrency)
                        .invoke(feeValue = value, fiatRate = feeStatus.value.fiatRate, showCrypto = false)
                } ?: stringReference(DASH_SIGN),
                isClaimEnabled = feeValue != null,
            )
        }
        yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
            data = YieldSupplyNotificationData(feeValue = feeValue, feeError = result.leftOrNull()),
        )

        return transactionData
    }

    /** Only disables: unlike the sibling sheets, a loaded fee alone does not make the claim valid. */
    private fun subscribeOnNotificationErrors() {
        yieldSupplyNotificationsUpdateTrigger.hasErrorFlow
            .onEach { hasError -> if (hasError) uiState.update { state -> state.copy(isClaimEnabled = false) } }
            .launchIn(modelScope)

        // Design rule: the service fee is hidden only when the network fee cannot be paid
        yieldSupplyNotificationsUpdateTrigger.cannotCoverFeeFlow
            .onEach { cannotCoverFee -> uiState.update { state -> state.copy(isServiceFeeVisible = !cannotCoverFee) } }
            .launchIn(modelScope)
    }

    /** The balance may have changed since the fee was shown, so it is re-checked right before signing. */
    private suspend fun isFeeCovered(transactionData: TransactionData.Uncompiled): Boolean {
        val fee = transactionData.fee?.amount?.value.orZero()
        val feeStatus = refreshFeeStatus()
        val warning = feeStatus?.let {
            getBalanceNotEnoughForFeeWarningUseCase(
                fee = fee,
                userWalletId = params.userWallet.walletId,
                tokenStatus = cryptoCurrencyStatus,
                feeStatus = feeStatus,
            )
        }
        val isCovered = warning?.getOrNull() == null && warning?.isRight() == true

        if (!isCovered) {
            yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
                data = YieldSupplyNotificationData(
                    feeValue = fee,
                    feeError = GetFeeError.UnknownError.takeIf { warning == null || warning.isLeft() },
                ),
            )
        }

        return isCovered
    }

    /** No fallback to the claimed token's status: it is not what pays the fee. */
    private suspend fun refreshFeeStatus(): CryptoCurrencyStatus? {
        val feeStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(
            userWalletId = params.userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        ).getOrElse { error ->
            TangemLogger.e("Yield claim fee currency status could not be read: $error")
            return null
        } ?: run {
            TangemLogger.e("Yield claim fee currency status is not in the status cache")
            return null
        }

        feeCryptoCurrencyStatusFlow.update { feeStatus }
        if (feeStatus.currency.id != cryptoCurrency.id) {
            uiState.update { it.copy(networkFeeCurrency = stringReference(feeStatus.currency.symbol)) }
        }

        return feeStatus
    }

    private class PreparedClaim(
        val transactionData: TransactionData.Uncompiled,
        val preparedAt: TimeMark,
        val claimedRewards: List<YieldReward>,
    )

    private companion object {
        /** Shorter than the Merkl proof rollover; bound to the proof, not to the fee the owner already accepted. */
        val PREPARATION_VALIDITY = 2.minutes
    }
}