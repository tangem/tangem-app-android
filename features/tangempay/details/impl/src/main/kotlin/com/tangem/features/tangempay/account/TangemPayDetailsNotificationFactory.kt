package com.tangem.features.tangempay.account

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
) {
    fun createErrorConfig(error: PaymentAccountStatusValue.Error?): TangemMessageBanner.State? = when (error) {
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

    fun createTiersConfig(tariffPlan: TangemPayTariffPlanState?): TangemMessageBanner.State? {
        tariffPlan ?: return null
        return when (TangemPayTiersBannerType.fromPlan(tariffPlan)) {
            null -> null
            TangemPayTiersBannerType.TierSystemDowngrade -> createTariffSystemDowngradePendingConfig(tariffPlan)
            TangemPayTiersBannerType.TopUpForTierUpgrade -> createTopUpForTierUpgradeConfig(tariffPlan)
        }
    }

    private fun createTopUpForTierUpgradeConfig(tariffPlan: TangemPayTariffPlanState): TangemMessageBanner.State? {
        val orderStep = tariffPlan.order?.step
        if (orderStep !is TangemPayTariffPlanState.OrderStep.AwaitingDeposit) return null
        val feeText = orderStep.toPlan.formatRecurringFeeOrNull() ?: return null

        return TangemMessageBanner.State(
            title = resourceReference(R.string.tangempay_card_details_awaiting_deposit_title, wrappedList(feeText)),
            variant = TangemMessageBanner.Variant.Error,
            description = resourceReference(R.string.tangempay_card_details_awaiting_deposit_subtitle),
            secondaryButton = TangemMessageBanner.Button(
                text = resourceReference(R.string.tangempay_card_details_add_funds),
                onClick = intents::onClickAddFunds,
            ),
            iconEnd = alertIcon(),
        )
    }

    private fun createRenewSessionNotificationConfig() = TangemMessageBanner.State(
        title = resourceReference(R.string.tangempay_sync_needed_title),
        variant = TangemMessageBanner.Variant.Error,
        description = resourceReference(R.string.tangempay_sync_needed_body),
        secondaryButton = TangemMessageBanner.Button(
            text = resourceReference(R.string.tangempay_sync_needed_button),
            onClick = intents::onRenewSession,
            iconEnd = TangemIconUM.Icon(
                iconRes = R.drawable.ic_tangem_24,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
        ),
    )

    private fun createAccountUnavailableConfig() = TangemMessageBanner.State(
        title = resourceReference(R.string.tangempay_temporarily_unavailable),
        variant = TangemMessageBanner.Variant.Error,
        description = resourceReference(R.string.tangempay_service_unreachable_try_later),
        iconEnd = alertIcon(),
    )

    private fun createTariffSystemDowngradePendingConfig(
        tariffPlan: TangemPayTariffPlanState,
    ): TangemMessageBanner.State? {
        val date = tariffPlan.tariff.formatNextBillingDateOrNull() ?: return null
        val planName = tariffPlan.tariff.plan.name
        return TangemMessageBanner.State(
            title = resourceReference(R.string.tangempay_card_details_system_downgrade_title),
            variant = TangemMessageBanner.Variant.Error,
            description = resourceReference(
                R.string.tangempay_card_details_system_downgrade_subtitle,
                wrappedList(planName, date),
            ),
            secondaryButton = TangemMessageBanner.Button(
                text = resourceReference(R.string.tangempay_card_details_add_funds),
                onClick = intents::onClickAddFunds,
            ),
            iconEnd = alertIcon(),
        )
    }

    private fun alertIcon() = TangemIconUM.Icon(
        imageVector = Icons.ic_error_20,
        tintReference = { TangemTheme.colors3.icon.primary },
    )
}

internal enum class TangemPayTiersBannerType {
    TopUpForTierUpgrade,
    TierSystemDowngrade,
    ;

    internal companion object {
        fun fromPlan(tariffPlan: TangemPayTariffPlanState): TangemPayTiersBannerType? {
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