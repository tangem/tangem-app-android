package com.tangem.features.tangempay.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.TextReference

@Composable
internal fun TangemPayOnboardingBLocks(modifier: Modifier = Modifier) {
    TangemPayOnboardingBlock(
        modifier = modifier,
        painterRes = R.drawable.ic_security_check_22,
        titleRef = TextReference.Res(R.string.tangempay_onboarding_security_title),
        descriptionRef = TextReference.Res(R.string.tangempay_onboarding_security_description),
    )

    SpacerH(18.dp)

    TangemPayOnboardingBlock(
        modifier = modifier,
        painterRes = R.drawable.ic_shopping_basket_22,
        titleRef = TextReference.Res(R.string.tangempay_onboarding_purchases_title),
        descriptionRef = TextReference.Res(R.string.tangempay_onboarding_purchases_description),
    )

    SpacerH(18.dp)

    TangemPayOnboardingBlock(
        modifier = modifier,
        painterRes = R.drawable.ic_credit_card_add_22,
        titleRef = TextReference.Res(R.string.tangempay_onboarding_pay_title),
        descriptionRef = TextReference.Res(R.string.tangempay_onboarding_pay_description),
    )
}