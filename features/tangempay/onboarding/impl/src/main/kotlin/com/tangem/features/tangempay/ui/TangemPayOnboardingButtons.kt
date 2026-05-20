package com.tangem.features.tangempay.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe

@Composable
internal fun TangemPayOnboardingButtons(
    onGetCardClick: () -> Unit,
    isLoading: Boolean,
    onTermsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(R.string.tangem_pay_terms_fees_limits),
            onClick = onTermsClick,
        )
        NavigationPrimaryButton(
            primaryButton = NavigationButton(
                textReference = resourceReference(R.string.tangempay_onboarding_get_card_button_text),
                iconRes = R.drawable.ic_tangem_24,
                isIconVisible = true,
                shouldShowProgress = isLoading,
                onClick = onGetCardClick,
            ),
        )
    }
}