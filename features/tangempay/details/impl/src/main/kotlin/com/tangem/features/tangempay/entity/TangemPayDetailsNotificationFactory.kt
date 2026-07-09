package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tiers.formatNextBillingDateOrNull
import com.tangem.features.tangempay.tiers.formatRecurringFeeOrNull
import com.tangem.features.tangempay.utils.TangemPayDetailIntents

internal class TangemPayDetailsNotificationFactory(
    private val intents: TangemPayDetailIntents,
    private val isRemoveAccountEnabled: Boolean,
    private val isTiersPlusPlanEnabled: Boolean,
    private val isRedesignEnabled: Boolean,
) {
    fun createErrorConfig(error: PaymentAccountStatusValue.Error?): NotificationConfig? = when (error) {
        null -> null
        PaymentAccountStatusValue.Error.NotSynced -> createRenewSessionNotificationConfig(isRedesignEnabled)
        else -> createAccountUnavailableConfig(isRedesignEnabled)
    }

    fun createAccountDeactivatedConfig(isRedesignEnabled: Boolean) = NotificationConfig(
        title = resourceReference(R.string.tangempay_account_deactivated_message_title),
        subtitle = resourceReference(R.string.tangempay_account_deactivated_message_subtitle),
        iconResId = if (isRedesignEnabled) R.drawable.ic_alert_circle_24 else R.drawable.img_attention_20,
        buttonsState = if (isRemoveAccountEnabled) {
            NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.tangempay_remove_account),
                onClick = intents::onRemoveAccount,
            )
        } else {
            null
        },
    )

    // TODO v_rodionov: #[REDACTED_TASK_KEY] fix hardcoded strings
    fun createAwaitingDepositConfig(tariffPlan: TangemPayTariffPlanState?): NotificationConfig? {
        if (!isTiersPlusPlanEnabled) return null
        if (tariffPlan == null) return null

        if (tariffPlan.tariff.status == TangemPayCustomerTariffPlan.Status.SYSTEM_DOWNGRADE_PENDING) {
            return createTariffSystemDownGradePendingConfig(tariffPlan)
        }

        val order = tariffPlan.order ?: return null
        val orderStep = order.step
        if (orderStep !is TangemPayTariffPlanState.OrderStep.AwaitingDeposit) return null

        val feeText = orderStep.toPlan.formatRecurringFeeOrNull() ?: return null

        val title = "Top-up your account on $feeText"
        return NotificationConfig(
            title = stringReference(title),
            subtitle = stringReference("To pay monthly fee for plan and start use card"),
            iconResId = R.drawable.ic_alert_circle_24,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = stringReference("Cancel ${orderStep.toPlan.name}, move to ${orderStep.fromPlan.name}"),
                onClick = { intents.onCancelPlusTransition(order.orderId) },
            ),
        )
    }

    private fun createRenewSessionNotificationConfig(isRedesignEnabled: Boolean) = NotificationConfig(
        title = resourceReference(R.string.tangempay_sync_needed_title),
        subtitle = resourceReference(R.string.tangempay_sync_needed_body),
        iconResId = if (isRedesignEnabled) 0 else R.drawable.img_attention_20,
        buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
            text = resourceReference(R.string.tangempay_sync_needed_button),
            onClick = intents::onRenewSession,
            iconResId = R.drawable.ic_tangem_24,
        ),
    )

    private fun createAccountUnavailableConfig(isRedesignEnabled: Boolean) = NotificationConfig(
        title = resourceReference(R.string.tangempay_temporarily_unavailable),
        subtitle = resourceReference(R.string.tangempay_service_unreachable_try_later),
        iconResId = if (isRedesignEnabled) R.drawable.ic_alert_circle_24 else R.drawable.img_attention_20,
    )

    // TODO v_rodionov: #[REDACTED_TASK_KEY] fix hardcoded strings
    private fun createTariffSystemDownGradePendingConfig(tariffPlan: TangemPayTariffPlanState): NotificationConfig? {
        val date = tariffPlan.tariff.formatNextBillingDateOrNull() ?: return null
        val planName = tariffPlan.tariff.plan.name
        return NotificationConfig(
            title = stringReference("Top up your account shortly"),
            subtitle = stringReference("If it will remain below zero your $planName cards will be closed on $date"),
            iconResId = R.drawable.ic_alert_circle_24,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = stringReference("Add funds"),
                onClick = intents::onClickAddFunds,
            ),
        )
    }
}