package com.tangem.features.earn.flow.impl.model

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.res.R
import com.tangem.core.ui.components.buttons.predefined.PredefinedPercentButtonUM
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.features.earn.flow.api.EarnFlowProduct
import com.tangem.features.earn.flow.impl.ui.state.EarnAmountUM
import com.tangem.features.earn.flow.impl.ui.state.EarnConfirmUM
import com.tangem.features.earn.flow.impl.ui.state.EarnIconsUM
import com.tangem.features.earn.flow.impl.ui.state.EarnResultUM
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
    ): EarnAmountUM = EarnAmountUM(
        title = action.title(product.title),
        amountState = amountState.withBalanceLabel(balance, status),
        clickIntents = clickIntents,
        percents = PERCENTS.map { percent ->
            PredefinedPercentButtonUM(
                id = percent.toString(),
                label = stringReference("$percent${StringsSigns.PERCENT}"),
                onClick = { callbacks.onPercentClick(percent) },
            )
        }.toImmutableList(),
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
        callbacks: EarnConfirmCallbacks,
    ): EarnConfirmUM = EarnConfirmUM(
        icons = icons(status, product),
        amountFiat = appCurrency
            ?.let { getFiatReference(plan?.expectedAmount, status.value.fiatRate, it) }
            ?: TextReference.EMPTY,
        amountCrypto = plan?.expectedAmount
            ?.let { stringReference(it.format { crypto(status.currency) }) }
            ?: TextReference.EMPTY,
        rows = plan?.rows.orEmpty().map { it.toUM(callbacks.onInfoClick) }.toImmutableList(),
        notice = plan?.notice,
        notifications = notifications,
        legal = null,
        stepLabel = signing.stepLabel,
        primary = TangemButtonUM(
            text = action.label,
            type = TangemButtonType.Primary,
            isEnabled = isFeeReady && !isPreparing && notifications.none { it is NotificationUM.Error },
            isLoading = signing.isExecuting || isPreparing,
            onClick = callbacks.onConfirm,
        ),
    )

    fun result(
        result: EarnResult,
        txStatus: EarnTxStatus,
        status: CryptoCurrencyStatus,
        product: EarnFlowProduct,
        callbacks: EarnResultCallbacks,
    ): EarnResultUM = EarnResultUM(
        icons = icons(status, product),
        variant = txStatus.toVariant(),
        title = result.title,
        subtitle = result.subtitle,
        chip = result.chip,
        rows = result.rows.map { it.toUM(callbacks.onInfoClick) }.toImmutableList(),
        primary = result.primary?.toButton(TangemButtonType.Primary, callbacks.onExit)
            ?: closeButton(callbacks.onExit).takeIf { result.secondary == null },
        secondary = result.secondary?.toButton(TangemButtonType.Secondary, callbacks.onExit),
        onClose = { callbacks.onExit(EarnFlowExit.Done) },
    )

    private fun icons(status: CryptoCurrencyStatus, product: EarnFlowProduct) = EarnIconsUM(
        currency = iconStateConverter.convert(status.currency),
        provider = product.provider.icon,
    )

    /** The shared card writes "2.01 ETH • $…", the Earn layout shows "Balance: 2.01 ETH". */
    private fun AmountState.withBalanceLabel(balance: BigDecimal?, status: CryptoCurrencyStatus?): AmountState {
        if (this !is AmountState.Data || balance == null || status == null) return this
        return copy(
            availableBalanceCrypto = resourceReference(
                id = R.string.common_balance,
                formatArgs = wrappedList(balance.format { crypto(status.currency) }),
            ),
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
    }
}

internal data class EarnAmountCallbacks(
    val onPercentClick: (Int) -> Unit,
    val onSubmit: () -> Unit,
)

internal data class EarnConfirmCallbacks(
    val onInfoClick: (EarnTextBlock) -> Unit,
    val onConfirm: () -> Unit,
)

internal data class EarnResultCallbacks(
    val onInfoClick: (EarnTextBlock) -> Unit,
    val onExit: (EarnFlowExit) -> Unit,
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