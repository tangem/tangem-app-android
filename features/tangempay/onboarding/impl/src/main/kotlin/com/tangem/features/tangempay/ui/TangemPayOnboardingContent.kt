package com.tangem.features.tangempay.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.onboarding.impl.R

@Composable
internal fun TangemPayOnboardingContent(state: TangemPayOnboardingScreenState, modifier: Modifier = Modifier) {
    if (state.fullScreenLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier,
                color = TangemTheme.colors.icon.primary1,
            )
        }
    } else {
        Column(
            modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_tangem_pay_onboarding),
                    contentDescription = null,
                    modifier = Modifier
                        .height(250.dp)
                        .fillMaxWidth(),
                )

                Text(
                    text = stringResourceSafe(R.string.tangempay_onboarding_title),
                    style = TangemTheme.typography.h2,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                )

                SpacerH(32.dp)

                TangemPayOnboardingBLocks()
            }

            NavigationPrimaryButton(
                modifier = Modifier
                    .imePadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                primaryButton = NavigationButton(
                    textReference = resourceReference(R.string.tangempay_onboarding_get_card_button_text),
                    iconRes = R.drawable.ic_tangem_24,
                    isIconVisible = true,
                    showProgress = state.buttonLoading,
                    onClick = {},
                ),
            )
        }
    }
}