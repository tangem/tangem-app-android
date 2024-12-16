package com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onboarding.v2.impl.R

@Composable
fun Wallet1ChooseOption(onSkipClick: () -> Unit, onBackupClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        // val pagerState = rememberPagerState(pageCount = { 10 }) // TODO [REDACTED_TASK_KEY]

        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, bottom = 24.dp)
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.onboarding_create_wallet_header),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )

            Text(
                text = stringResource(R.string.onboarding_create_wallet_body),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        PrimaryButtonIconEnd(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth(),
            iconResId = R.drawable.ic_tangem_24,
            text = stringResource(R.string.onboarding_button_backup_now),
            onClick = onBackupClick,
        )

        SecondaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.onboarding_button_skip_backup),
            onClick = onSkipClick,
        )
    }
}