package com.tangem.features.tangempay.verified

import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.pay.TangemPayProduct

internal class TangemPayVerifiedUMFactory(
    private val onChoosePlanClick: () -> Unit,
    private val onOpenMoneyAccountClick: () -> Unit,
    private val onNotifyClick: () -> Unit,
    private val onCloseClick: () -> Unit,
) {

    fun create(availableProducts: Set<TangemPayProduct>): TangemPayVerifiedUM.Content {
        val isMoneyAccountAvailable = TangemPayProduct.BANKING_ACCOUNT in availableProducts
        val isVisaCardAvailable = TangemPayProduct.PAYMENT_ACCOUNT in availableProducts
        return TangemPayVerifiedUM.Content(
            description = createDescription(isMoneyAccountAvailable, isVisaCardAvailable),
            moneyAccount = TangemPayVerifiedUM.ProductUM(
                title = resourceReference(R.string.money_account_main_name),
                subtitle = if (isMoneyAccountAvailable) {
                    resourceReference(R.string.tangempay_verified_money_account_subtitle)
                } else {
                    resourceReference(R.string.tangempay_verified_product_not_available)
                },
                isAvailable = isMoneyAccountAvailable,
                badge = createBadge(isMoneyAccountAvailable),
            ),
            visaCard = TangemPayVerifiedUM.ProductUM(
                title = resourceReference(R.string.tangempay_verified_payment_account_title),
                subtitle = if (isVisaCardAvailable) {
                    resourceReference(R.string.tangempay_verified_payment_account_subtitle)
                } else {
                    resourceReference(R.string.tangempay_verified_product_not_available)
                },
                isAvailable = isVisaCardAvailable,
                badge = createBadge(isVisaCardAvailable),
            ),
            primaryButton = when {
                isVisaCardAvailable -> TangemPayVerifiedUM.ButtonUM(
                    text = resourceReference(R.string.tangempay_verified_choose_plan),
                    onClick = onChoosePlanClick,
                )
                isMoneyAccountAvailable -> TangemPayVerifiedUM.ButtonUM(
                    text = resourceReference(R.string.tangempay_verified_open_money_account),
                    onClick = onOpenMoneyAccountClick,
                )
                else -> TangemPayVerifiedUM.ButtonUM(
                    text = resourceReference(R.string.common_close),
                    onClick = onCloseClick,
                )
            },
            secondaryButton = when {
                !isMoneyAccountAvailable && isVisaCardAvailable -> TangemPayVerifiedUM.ButtonUM(
                    text = resourceReference(R.string.tangempay_verified_notify_money_account),
                    onClick = onNotifyClick,
                )
                isMoneyAccountAvailable && !isVisaCardAvailable -> TangemPayVerifiedUM.ButtonUM(
                    text = resourceReference(R.string.tangempay_verified_notify_payment_account),
                    onClick = onNotifyClick,
                )
                else -> null
            },
            onCloseClick = onCloseClick,
        )
    }

    private fun createDescription(isMoneyAccountAvailable: Boolean, isVisaCardAvailable: Boolean): TextReference =
        when {
            isMoneyAccountAvailable && isVisaCardAvailable ->
                resourceReference(R.string.tangempay_verified_description_all)
            isMoneyAccountAvailable -> resourceReference(R.string.tangempay_verified_description_money_account)
            isVisaCardAvailable -> resourceReference(R.string.tangempay_verified_description_payment_account)
            else -> resourceReference(R.string.tangempay_verified_description_none)
        }

    private fun createBadge(isAvailable: Boolean): TangemBadgeUM = if (isAvailable) {
        TangemBadgeUM(
            text = resourceReference(R.string.tangempay_verified_badge_available),
            status = TangemBadge.Status.Success,
            size = TangemBadge.Size.X6,
        )
    } else {
        TangemBadgeUM(
            text = resourceReference(R.string.common_later),
            status = TangemBadge.Status.Warning,
            size = TangemBadge.Size.X6,
        )
    }
}