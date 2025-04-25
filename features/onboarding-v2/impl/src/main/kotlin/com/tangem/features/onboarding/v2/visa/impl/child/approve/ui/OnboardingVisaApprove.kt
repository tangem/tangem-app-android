package com.tangem.features.onboarding.v2.visa.impl.child.approve.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.visa.impl.child.approve.ui.state.OnboardingVisaApproveUM

@Composable
internal fun OnboardingVisaApprove(state: OnboardingVisaApproveUM, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 56.dp, bottom = 32.dp)
                .padding(horizontal = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_tangem_card_grey),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
            )

            SpacerH(86.dp)

            Text(
                text = stringResourceSafe(R.string.visa_onboarding_tangem_approve_title),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )

            SpacerH(12.dp)

            Text(
                text = stringResourceSafe(R.string.visa_onboarding_tangem_approve_description),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }

        NavigationPrimaryButton(
            modifier = Modifier
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            primaryButton = NavigationButton(
                textReference = resourceReference(R.string.common_approve),
                iconRes = R.drawable.ic_tangem_24,
                isIconVisible = true,
                showProgress = state.approveButtonLoading,
                onClick = state.onApproveClick,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingVisaApprove(state = OnboardingVisaApproveUM())
    }
}