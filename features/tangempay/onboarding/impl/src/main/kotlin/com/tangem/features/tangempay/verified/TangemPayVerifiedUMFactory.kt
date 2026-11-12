package com.tangem.features.tangempay.verified

import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.domain.models.tangempay.TangemPayProduct

internal class TangemPayVerifiedUMFactory(
    private val onChoosePlanClick: () -> Unit,
    private val onOpenMoneyAccountClick: () -> Unit,
    private val onCloseClick: () -> Unit,
) {

    fun create(availableProducts: Set<TangemPayProduct>): TangemPayVerifiedUM.Content {
        val isMoneyAccountAvailable = TangemPayProduct.BANKING_ACCOUNT in availableProducts
        val isPaymentAccountAvailable = TangemPayProduct.PAYMENT_ACCOUNT in availableProducts
        return TangemPayVerifiedUM.Content(
            description = description(isMoneyAccountAvailable, isPaymentAccountAvailable),
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
            paymentAccount = TangemPayVerifiedUM.ProductUM(
                title = resourceReference(R.string.tangempay_verified_payment_account_title),
                subtitle = if (isPaymentAccountAvailable) {
                    resourceReference(R.string.tangempay_verified_payment_account_subtitle)
                } else {
                    resourceReference(R.string.tangempay_verified_product_not_available)
                },
                isAvailable = isPaymentAccountAvailable,
                badge = createBadge(isPaymentAccountAvailable),
            ),
            primaryButton = primaryButton(isPaymentAccountAvailable, isMoneyAccountAvailable),
            onCloseClick = onCloseClick,
        )
    }

    private fun primaryButton(
        isPaymentAccountAvailable: Boolean,
        isMoneyAccountAvailable: Boolean,
    ): TangemPayVerifiedUM.ButtonUM = TangemPayVerifiedUM.ButtonUM(
        text = when {
            isPaymentAccountAvailable -> resourceReference(R.string.tangempay_verified_choose_plan)
            isMoneyAccountAvailable -> resourceReference(R.string.tangempay_verified_open_money_account)
            else -> resourceReference(R.string.common_close)
        },
        iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24)
            .takeIf { isMoneyAccountAvailable && !isPaymentAccountAvailable },
        onClick = when {
            isPaymentAccountAvailable -> onChoosePlanClick
            isMoneyAccountAvailable -> onOpenMoneyAccountClick
            else -> onCloseClick
        },
    )

    private fun description(isMoneyAccountAvailable: Boolean, isPaymentAccountAvailable: Boolean): TextReference =
        when {
            isMoneyAccountAvailable && isPaymentAccountAvailable -> resourceReference(
                R.string.tangempay_verified_description_all,
            )
            isMoneyAccountAvailable -> resourceReference(R.string.tangempay_verified_description_money_account)
            isPaymentAccountAvailable -> resourceReference(R.string.tangempay_verified_description_payment_account)
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