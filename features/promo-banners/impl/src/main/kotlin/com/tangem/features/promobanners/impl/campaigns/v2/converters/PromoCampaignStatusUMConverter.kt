package com.tangem.features.promobanners.impl.campaigns.v2.converters

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignStatusUM
import com.tangem.features.promobanners.impl.campaigns.v2.model.PromoRegistrationOutcome
import com.tangem.features.promobanners.impl.campaigns.v2.model.PromoRegistrationOutcome.Coverage
import com.tangem.utils.toBriefAddressFormat
import kotlinx.collections.immutable.persistentListOf

internal class PromoCampaignStatusUMConverter(
    private val campaign: PromoCampaign,
    private val recipientWalletName: TextReference,
    private val recipientIcon: CurrencyIconState?,
    private val onDismiss: () -> Unit,
) {

    fun convert(outcome: PromoRegistrationOutcome): PromoCampaignStatusUM? = when (outcome) {
        is PromoRegistrationOutcome.Enrolled -> enrolled(outcome.coverage)
        is PromoRegistrationOutcome.AlreadyEnrolled -> alreadyEnrolled(outcome)
        PromoRegistrationOutcome.Unavailable -> warning(
            title = resourceReference(R.string.promo_campaign_unavailable_title),
            description = resourceReference(R.string.promo_campaign_not_active_subtitle),
        )
        PromoRegistrationOutcome.NotStarted -> warning(
            title = resourceReference(R.string.promo_campaign_not_started_title),
            description = resourceReference(R.string.common_try_again_later),
        )
        PromoRegistrationOutcome.Failed -> null
    }

    private fun enrolled(coverage: Coverage) = PromoCampaignStatusUM(
        variant = TangemStatusScreen.Variant.Success,
        icon = TangemStatusIcon.Kind.Checkmark,
        title = resourceReference(R.string.promo_campaign_enroll_success_title, wrappedList(campaign.content.name)),
        description = when (coverage) {
            Coverage.Single -> null
            Coverage.All -> resourceReference(R.string.promo_campaign_all_wallets_enrolled)
            Coverage.Partial -> resourceReference(R.string.promo_campaign_some_wallets_enrolled)
        },
        details = if (coverage == Coverage.Partial) {
            PromoCampaignStatusUM.Details.EnrolledWallets(wallets = persistentListOf())
        } else {
            null
        },
        buttonText = resourceReference(R.string.common_got_it),
        onButtonClick = onDismiss,
    )

    private fun alreadyEnrolled(outcome: PromoRegistrationOutcome.AlreadyEnrolled) = PromoCampaignStatusUM(
        variant = TangemStatusScreen.Variant.Info,
        icon = TangemStatusIcon.Kind.Info,
        title = resourceReference(
            R.string.promo_campaign_already_activated_title,
            wrappedList(campaign.content.name),
        ),
        description = null,
        details = outcome.payout?.let { payout ->
            PromoCampaignStatusUM.Details.Recipient(
                walletName = recipientWalletName,
                tokenIcon = recipientIcon,
                address = payout.userAddress.toBriefAddressFormat(),
            )
        },
        buttonText = resourceReference(R.string.common_close),
        onButtonClick = onDismiss,
    )

    private fun warning(title: TextReference, description: TextReference) = PromoCampaignStatusUM(
        variant = TangemStatusScreen.Variant.Warning,
        icon = TangemStatusIcon.Kind.Warning,
        title = title,
        description = description,
        details = null,
        buttonText = resourceReference(R.string.common_close),
        onButtonClick = onDismiss,
    )
}