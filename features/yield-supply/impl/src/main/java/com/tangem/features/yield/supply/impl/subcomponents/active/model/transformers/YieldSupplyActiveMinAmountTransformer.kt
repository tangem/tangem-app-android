package com.tangem.features.yield.supply.impl.subcomponents.active.model.transformers

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.notSuppliedAmountOrNull
import com.tangem.domain.models.currency.shouldShowNotSuppliedInfoIcon
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.subcomponents.active.entity.YieldSupplyActiveContentUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

/**
 * Transformer that populates minimum supply amount and related hints for the active Yield Supply screen.
 *
 * - Sets the displayed minimum amount in fiat and crypto.
 * - Builds the fee policy note text using the minimum amount.
 * - Adds contextual notifications:
 *   - Approval required notification when spending is not yet allowed (emits analytics on CTA).
 *   - "Not all amount supplied" info when wallet balance exceeds the supplied balance by more than [minAmount].
 *
 * @property cryptoCurrencyStatus Current currency status used to calculate values and flags.
 * @property appCurrency Preferred fiat currency for formatting.
 * @property minAmount Protocol-required minimal amount to deposit/supply (in crypto units).
 * @property analyticsHandler Analytics reporter for user actions.
 * @property onApprove Action invoked when the "Approve" notification button is tapped.
 */
internal class YieldSupplyActiveMinAmountTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val minAmount: BigDecimal,
    private val analyticsHandler: AnalyticsEventHandler,
    private val onApprove: () -> Unit,
) : Transformer<YieldSupplyActiveContentUM> {

    override fun transform(prevState: YieldSupplyActiveContentUM): YieldSupplyActiveContentUM {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val tokenFiatRate = cryptoCurrencyStatus.value.fiatRate

        val minAmountCryptoText = minAmount.format { crypto(cryptoCurrency) }
        val minAmountFiat = tokenFiatRate?.let(minAmount::multiply)
        val minAmountFiatText = minAmountFiat.format { fiat(appCurrency.code, appCurrency.symbol) }

        val minFeeNoteValue = resourceReference(
            id = R.string.yield_module_fee_policy_sheet_min_amount_note,
            formatArgs = wrappedList(
                minAmountFiatText,
                minAmountCryptoText,
            ),
        )

        return prevState.copy(
            minAmount = stringReference(minAmountFiatText),
            minFeeDescription = minFeeNoteValue,
            notifications = getNotifications(),
        )
    }

    private fun getNotifications(): ImmutableList<NotificationUM> {
        val approvalNotification = if (cryptoCurrencyStatus.value.yieldSupplyStatus?.isAllowedToSpend != true) {
            NotificationUM.Error(
                title = resourceReference(R.string.yield_module_approve_needed_notification_title),
                subtitle = resourceReference(R.string.yield_module_approve_needed_notification_description),
                iconResId = R.drawable.ic_alert_triangle_20,
                buttonState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                    text = resourceReference(R.string.yield_module_approve_needed_notification_cta),
                    onClick = onApprove,
                ),
            )
        } else {
            null
        }
        val notSuppliedNotification = getNotSuppliedNotification(cryptoCurrencyStatus)
        return listOfNotNull(
            approvalNotification,
            notSuppliedNotification,
        ).toPersistentList()
    }

    private fun getNotSuppliedNotification(cryptoCurrencyStatus: CryptoCurrencyStatus): NotificationUM? {
        return if (cryptoCurrencyStatus.shouldShowNotSuppliedInfoIcon(minAmount)) {
            val cryptoCurrency = cryptoCurrencyStatus.currency
            val notDepositedAmount = cryptoCurrencyStatus.notSuppliedAmountOrNull()
            val formattedAmount =
                notDepositedAmount.format { crypto(symbol = "", decimals = cryptoCurrencyStatus.currency.decimals) }
            analyticsHandler.send(
                YieldSupplyAnalytics.NoticeAmountNotDeposited(
                    token = cryptoCurrency.symbol,
                    blockchain = cryptoCurrency.network.name,
                ),
            )
            NotificationUM.Info.YieldSupplyNotAllAmountSupplied(
                formattedAmount = formattedAmount,
                symbol = cryptoCurrency.symbol,
            )
        } else {
            null
        }
    }
}