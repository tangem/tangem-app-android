package com.tangem.features.tangempay.account

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tiers.formatNextBillingDateOrNull
import com.tangem.features.tangempay.tiers.formatRecurringFeeOrNull

internal class TangemPayDetailsNotificationFactory(
    private val intents: TangemPayDetailIntents,
    private val isTiersPlusPlanEnabled: Boolean,
) {
    fun createErrorConfig(error: PaymentAccountStatusValue.Error?): NotificationConfig? = when (error) {
        null -> null
        PaymentAccountStatusValue.Error.NotSynced -> createRenewSessionNotificationConfig()
        else -> createAccountUnavailableConfig()
    }

    fun createCardIssueFailedBannerState() = TangemMessageBanner.State(
        title = resourceReference(R.string.tangempay_failed_to_issue_card),
        variant = TangemMessageBanner.Variant.Error,
        description = resourceReference(R.string.tangempay_failed_to_issue_card_support_description),
        secondaryButton = TangemMessageBanner.Button(
            text = resourceReference(R.string.common_contact_support),
            onClick = intents::onCardIssueFailedSupportClick,
        ),
        iconStart = TangemIconUM.Icon(
            imageVector = Icons.ic_error_20,
            tintReference = { TangemTheme.colors3.icon.primary },
        ),
    )

    fun createAccountDeactivatedBannerState() = TangemMessageBanner.State(
        title = resourceReference(R.string.tangempay_account_deactivated_message_title),
        variant = TangemMessageBanner.Variant.Error,
        description = resourceReference(R.string.tangempay_account_deactivated_message_subtitle),
        secondaryButton = TangemMessageBanner.Button(
            text = resourceReference(R.string.tangempay_remove_account),
            onClick = intents::onRemoveAccount,
        ),
        iconStart = TangemIconUM.Icon(
            imageVector = Icons.ic_error_20,
            tintReference = { TangemTheme.colors3.icon.primary },
        ),
    )

    fun createTiersConfig(tariffPlan: TangemPayTariffPlanState?): NotificationConfig? {
        tariffPlan ?: return null
        return when (TangemPayTiersBannerType.fromPlan(isTiersPlusPlanEnabled, tariffPlan)) {
            null -> null
            TangemPayTiersBannerType.TierSystemDowngrade -> createTariffSystemDowngradePendingConfig(tariffPlan)
            TangemPayTiersBannerType.TopUpForTierUpgrade -> createTopUpForTierUpgradeConfig(tariffPlan)
        }
    }

    private fun createTopUpForTierUpgradeConfig(tariffPlan: TangemPayTariffPlanState): NotificationConfig? {
        val orderStep = tariffPlan.order?.step
        if (orderStep !is TangemPayTariffPlanState.OrderStep.AwaitingDeposit) return null
        val feeText = orderStep.toPlan.formatRecurringFeeOrNull() ?: return null

        return NotificationConfig(
            title = resourceReference(R.string.tangempay_card_details_awaiting_deposit_title, wrappedList(feeText)),
            subtitle = resourceReference(R.string.tangempay_card_details_awaiting_deposit_subtitle),
            iconResId = R.drawable.ic_alert_circle_24,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.tangempay_card_details_add_funds),
                onClick = intents::onClickAddFunds,
            ),
        )
    }

    private fun createRenewSessionNotificationConfig() = NotificationConfig(
        title = resourceReference(R.string.tangempay_sync_needed_title),
        subtitle = resourceReference(R.string.tangempay_sync_needed_body),
        iconResId = 0,
        buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
            text = resourceReference(R.string.tangempay_sync_needed_button),
            onClick = intents::onRenewSession,
            iconResId = R.drawable.ic_tangem_24,
        ),
    )

    private fun createAccountUnavailableConfig() = NotificationConfig(
        title = resourceReference(R.string.tangempay_temporarily_unavailable),
        subtitle = resourceReference(R.string.tangempay_service_unreachable_try_later),
        iconResId = R.drawable.ic_alert_circle_24,
    )

    private fun createTariffSystemDowngradePendingConfig(tariffPlan: TangemPayTariffPlanState): NotificationConfig? {
        val date = tariffPlan.tariff.formatNextBillingDateOrNull() ?: return null
        val planName = tariffPlan.tariff.plan.name
        return NotificationConfig(
            title = resourceReference(R.string.tangempay_card_details_system_downgrade_title),
            subtitle = resourceReference(
                R.string.tangempay_card_details_system_downgrade_subtitle,
                wrappedList(planName, date),
            ),
            iconResId = R.drawable.ic_alert_circle_24,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.tangempay_card_details_add_funds),
                onClick = intents::onClickAddFunds,
            ),
        )
    }
}

internal enum class TangemPayTiersBannerType {
    TopUpForTierUpgrade,
    TierSystemDowngrade,
    ;

    internal companion object {
        fun fromPlan(
            isTiersPlusPlanEnabled: Boolean,
            tariffPlan: TangemPayTariffPlanState,
        ): TangemPayTiersBannerType? {
            if (!isTiersPlusPlanEnabled) return null

            if (tariffPlan.tariff.status == TangemPayCustomerTariffPlan.Status.SYSTEM_DOWNGRADE_PENDING) {
                tariffPlan.tariff.formatNextBillingDateOrNull() ?: return null
                return TierSystemDowngrade
            }

            val orderStep = tariffPlan.order?.step
            if (orderStep !is TangemPayTariffPlanState.OrderStep.AwaitingDeposit) return null
            orderStep.toPlan.formatRecurringFeeOrNull() ?: return null

            return TopUpForTierUpgrade
        }
    }
}