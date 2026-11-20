package com.tangem.features.earn.flow.impl.model

import androidx.annotation.DrawableRes
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.res.R
import com.tangem.core.ui.components.buttons.predefined.PredefinedPercentButtonUM
import com.tangem.core.ui.ds.button.TangemButtonIconPosition
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.features.earn.api.ui.EarnHowItWorksUM
import com.tangem.features.earn.flow.api.EarnFlowProduct
import com.tangem.features.earn.flow.impl.ui.state.EarnAmountUM
import com.tangem.features.earn.flow.impl.ui.state.EarnConfirmUM
import com.tangem.features.earn.flow.impl.ui.state.EarnIconsUM
import com.tangem.features.earn.flow.impl.ui.state.EarnLegalUM
import com.tangem.features.earn.flow.impl.ui.state.EarnResultUM
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.toUM
import com.tangem.features.earn.product.api.session.EarnActionInput
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.EarnPlan
import com.tangem.features.earn.product.api.session.EarnResult
import com.tangem.features.earn.product.api.session.EarnResultButton
import com.tangem.features.earn.product.api.session.EarnStatusDetail
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

internal class EarnFlowConverter @Inject constructor() {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    /** `null` for an action without an amount: its flow opens on the confirmation. */
    fun amount(state: EarnFlowState, screens: EarnFlowScreens): EarnAmountUM? {
        if (screens.action.input !is EarnActionInput.Amount) return null
        return amount(
            action = screens.action,
            product = screens.product,
            amountState = state.amountState,
            balance = state.maxAmount,
            status = state.status,
            isFeeReady = state.feeSelection != null,
            isPreparing = state.isPreparing,
            notifications = EarnFlowNotifications.amount(state, screens.onFeeReload),
            clickIntents = screens.clickIntents,
            callbacks = screens.amountCallbacks,
            source = state.source,
        )
    }

    fun confirm(state: EarnFlowState, screens: EarnFlowScreens): EarnConfirmUM? {
        val status = state.status ?: return null
        return confirm(
            action = screens.action,
            product = screens.product,
            plan = state.plan,
            status = status,
            appCurrency = state.appCurrency,
            signing = state.signing,
            isFeeReady = state.feeSelection != null,
            isPreparing = state.isPreparing,
            notifications = EarnFlowNotifications.confirm(state, screens.onFeeReload),
            walletIcon = state.walletIcon,
            callbacks = screens.confirmCallbacks,
        )
    }

    fun result(state: EarnFlowState, screens: EarnFlowScreens): EarnResultUM? {
        val result = state.result ?: return null
        val txStatus = state.txStatus ?: return null
        val status = state.status ?: return null
        return result(
            result = result,
            txStatus = txStatus,
            status = status,
            explorerUrl = state.explorerUrl,
            subtitle = result.subtitle ?: amountAndDate(state),
            product = screens.product,
            callbacks = screens.resultCallbacks,
        )
    }

    @Suppress("LongParameterList")
    fun amount(
        action: EarnFlowAction,
        product: EarnFlowProduct,
        amountState: AmountState,
        balance: BigDecimal?,
        status: CryptoCurrencyStatus?,
        isFeeReady: Boolean,
        isPreparing: Boolean,
        notifications: ImmutableList<NotificationUM>,
        clickIntents: AmountScreenClickIntents,
        callbacks: EarnAmountCallbacks,
        source: AccountTitleUM? = null,
    ): EarnAmountUM = EarnAmountUM(
        title = action.title(product.title),
        amountState = amountState.forEarnCard(
            balance = balance,
            status = status,
            source = source,
            tokenName = if (action.isFromPosition) product.title else status?.currency?.symbol?.let(::stringReference),
        ),
        clickIntents = clickIntents,
        percents = if (action.isFromPosition) persistentListOf() else percents(callbacks),
        notice = action.notice,
        notifications = notifications,
        primary = TangemButtonUM(
            text = action.label,
            type = TangemButtonType.Primary,
            isEnabled = isFeeReady &&
                !isPreparing &&
                (amountState as? AmountState.Data)?.isPrimaryButtonEnabled == true &&
                notifications.none { it is NotificationUM.Error },
            isLoading = isPreparing,
            onClick = callbacks.onSubmit,
        ),
        onHowItWorksClick = callbacks.onHowItWorksClick.takeIf { product.howItWorks != null },
        isFromPosition = action.isFromPosition,
    )

    /** [plan] is `null` until the first `prepare` succeeds: the screen still shows the fee block and its cards. */
    @Suppress("LongParameterList")
    fun confirm(
        action: EarnFlowAction,
        product: EarnFlowProduct,
        plan: EarnPlan?,
        status: CryptoCurrencyStatus,
        appCurrency: AppCurrency?,
        signing: EarnSigningState,
        isFeeReady: Boolean,
        isPreparing: Boolean,
        notifications: ImmutableList<NotificationUM>,
        @DrawableRes walletIcon: Int?,
        callbacks: EarnConfirmCallbacks,
    ): EarnConfirmUM = EarnConfirmUM(
        icons = icons(status, product),
        amountFiat = appCurrency
            ?.let { getFiatReference(plan?.expectedAmount, status.value.fiatRate, it) }
            ?: TextReference.EMPTY,
        amountCrypto = plan?.expectedAmount
            ?.let { stringReference(it.format { crypto(status.currency, ignoreSymbolPosition = true) }) }
            ?: TextReference.EMPTY,
        rows = plan?.rows.orEmpty().map { it.toUM(callbacks.onInfoClick) }.toImmutableList(),
        notice = plan?.notice ?: action.notice,
        notifications = notifications,
        legal = product.legal?.let { legal ->
            EarnLegalUM(
                termsOfUseUrl = legal.termsOfUseUrl,
                privacyPolicyUrl = legal.privacyPolicyUrl,
                onClick = callbacks.onLegalClick,
            )
        },
        stepLabel = signing.stepLabel,
        primary = TangemButtonUM(
            text = action.label,
            type = TangemButtonType.Primary,
            isEnabled = isFeeReady && !isPreparing && notifications.none { it is NotificationUM.Error },
            isLoading = signing.isExecuting || isPreparing,
            tangemIconUM = walletIcon?.let { TangemIconUM.Icon(iconRes = it) },
            iconPosition = TangemButtonIconPosition.End,
            onClick = callbacks.onConfirm,
        ),
    )

    @Suppress("LongParameterList")
    fun result(
        result: EarnResult,
        txStatus: EarnTxStatus,
        status: CryptoCurrencyStatus,
        explorerUrl: String?,
        subtitle: TextReference?,
        product: EarnFlowProduct,
        callbacks: EarnResultCallbacks,
    ): EarnResultUM = EarnResultUM(
        icons = icons(status, product),
        asset = result.asset,
        variant = txStatus.toVariant(),
        title = result.title,
        subtitle = subtitle,
        chip = result.chip,
        rows = result.rows.map { it.toUM(callbacks.onInfoClick) }.toImmutableList(),
        primary = result.primary?.toButton(TangemButtonType.Primary, callbacks.onExit)
            ?: closeButton(callbacks.onExit).takeIf { result.secondary == null },
        secondary = result.secondary?.toButton(TangemButtonType.Secondary, callbacks.onExit),
        explorer = explorerUrl?.takeIf { result.isExplorerShown && result.secondary == null }?.let { url ->
            EarnResultUM.ExplorerUM(
                onExploreClick = { callbacks.onExploreClick(url) },
                onShareClick = { callbacks.onShareClick(url) },
            )
        },
        onClose = { callbacks.onExit(EarnFlowExit.Done) },
    )

    fun howItWorks(howItWorks: EarnHowItWorks, onLearnMoreClick: (String) -> Unit): EarnHowItWorksUM = EarnHowItWorksUM(
        title = howItWorks.title,
        steps = howItWorks.entries
            .map { entry -> EarnHowItWorksUM.Step(title = entry.title, description = entry.description) }
            .toImmutableList(),
        footnote = howItWorks.footnote,
        onLearnMoreClick = howItWorks.learnMoreUrl?.let { url -> { onLearnMoreClick(url) } },
    )

    /** "$2,130.88 · 2 May 2024, 09:41": the default subtitle of a result the product leaves without one. */
    private fun amountAndDate(state: EarnFlowState): TextReference? {
        val timestamp = state.resultTimestamp ?: return null
        val date = resourceReference(
            id = R.string.send_date_format,
            formatArgs = wrappedList(
                timestamp.toTimeFormat(DateTimeFormatters.dateFormatter),
                timestamp.toTimeFormat(),
            ),
        )
        val appCurrency = state.appCurrency ?: return date
        val amount = state.sentAmount ?: return date
        val fiat = getFiatReference(amount, state.status?.value?.fiatRate, appCurrency) ?: return date
        return combinedReference(fiat, stringReference(SUBTITLE_SEPARATOR), date)
    }

    private fun percents(callbacks: EarnAmountCallbacks): ImmutableList<PredefinedPercentButtonUM> =
        PERCENTS.map { percent ->
            PredefinedPercentButtonUM(
                id = percent.toString(),
                label = stringReference("$percent${StringsSigns.PERCENT}"),
                onClick = { callbacks.onPercentClick(percent) },
            )
        }.toImmutableList()

    private fun icons(status: CryptoCurrencyStatus, product: EarnFlowProduct) = EarnIconsUM(
        currency = iconStateConverter.convert(status.currency),
        provider = product.provider.icon,
    )

    /**
     * The shared card writes "2.01 ETH • $…" under the currency name, the Earn layout shows "Balance: 2.01 ETH" under
     * the symbol, or under the product for a position. [source] loads after the card, so it replaces the title here.
     */
    private fun AmountState.forEarnCard(
        balance: BigDecimal?,
        status: CryptoCurrencyStatus?,
        source: AccountTitleUM?,
        tokenName: TextReference?,
    ): AmountState {
        if (this !is AmountState.Data) return this
        return copy(
            accountTitleUM = source ?: accountTitleUM,
            tokenName = tokenName ?: this.tokenName,
            availableBalanceCrypto = if (balance != null && status != null) {
                resourceReference(
                    id = R.string.common_balance,
                    formatArgs = wrappedList(balance.format { crypto(status.currency, ignoreSymbolPosition = true) }),
                )
            } else {
                availableBalanceCrypto
            },
            availableBalanceFiat = TextReference.EMPTY,
        )
    }

    private fun closeButton(onExit: (EarnFlowExit) -> Unit) = TangemButtonUM(
        text = resourceReference(R.string.common_close),
        type = TangemButtonType.Primary,
        onClick = { onExit(EarnFlowExit.Done) },
    )

    private fun EarnResultButton.toButton(type: TangemButtonType, onExit: (EarnFlowExit) -> Unit) =
        TangemButtonUM(text = text, type = type, onClick = { onExit(exit) })

    private fun EarnTxStatus.toVariant(): TangemStatusScreen.Variant = when {
        status == TxInfo.TransactionStatus.Failed -> TangemStatusScreen.Variant.Error
        detail is EarnStatusDetail.Returned -> TangemStatusScreen.Variant.Warning
        status == TxInfo.TransactionStatus.Unconfirmed || detail is EarnStatusDetail.AwaitingSettlement ->
            TangemStatusScreen.Variant.Info
        else -> TangemStatusScreen.Variant.Success
    }

    private companion object {
        val PERCENTS = listOf(25, 50, 75, 100)
        const val SUBTITLE_SEPARATOR = " ${StringsSigns.MIDDLE_DOT} "
    }
}

internal data class EarnAmountCallbacks(
    val onPercentClick: (Int) -> Unit,
    val onSubmit: () -> Unit,
    val onHowItWorksClick: () -> Unit,
)

internal data class EarnConfirmCallbacks(
    val onInfoClick: (EarnTextBlock) -> Unit,
    val onConfirm: () -> Unit,
    val onLegalClick: (String) -> Unit,
)

internal data class EarnResultCallbacks(
    val onInfoClick: (EarnTextBlock) -> Unit,
    val onExit: (EarnFlowExit) -> Unit,
    val onExploreClick: (String) -> Unit,
    val onShareClick: (String) -> Unit,
)

/** What the screens take from the flow besides its state: the action, the product and the callbacks. */
internal data class EarnFlowScreens(
    val action: EarnFlowAction,
    val product: EarnFlowProduct,
    val clickIntents: AmountScreenClickIntents,
    val amountCallbacks: EarnAmountCallbacks,
    val confirmCallbacks: EarnConfirmCallbacks,
    val resultCallbacks: EarnResultCallbacks,
    val onFeeReload: () -> Unit,
)

private val EarnFlowAction.isFromPosition: Boolean
    get() = (input as? EarnActionInput.Amount)?.source == EarnActionInput.Amount.Source.Position