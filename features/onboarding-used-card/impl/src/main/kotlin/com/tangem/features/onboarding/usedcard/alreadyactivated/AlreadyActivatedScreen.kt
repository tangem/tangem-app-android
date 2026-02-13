package com.tangem.features.onboarding.usedcard.alreadyactivated

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.icons.HighlightedIcon
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.usedcard.impl.R

@Composable
internal fun AlreadyActivatedScreen(
    onThisIsMyWalletClick: () -> Unit,
    onNewCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        HighlightedIcon(
            icon = R.drawable.ic_heading_32,
            iconTint = TangemTheme.colors.icon.accent,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResourceSafe(R.string.onboarding_used_card_welcome_back_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResourceSafe(R.string.onboarding_used_card_welcome_back_description),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                start = 20.dp,
                top = 16.dp,
                end = 20.dp,
            ),
        )

        WarningBlock(
            modifier = Modifier.padding(24.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = stringResourceSafe(R.string.this_is_my_wallet_title),
            onClick = onThisIsMyWalletClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        SecondaryButton(
            text = stringResourceSafe(R.string.onboarding_used_card_new_card_button),
            onClick = onNewCardClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun WarningBlock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors.icon.attention.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_alert_triangle_20),
            contentDescription = null,
            tint = TangemTheme.colors.icon.attention,
            modifier = Modifier.size(20.dp),
        )

        Text(
            text = stringResourceSafe(R.string.onboarding_used_card_warning),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAlreadyActivatedScreen() {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(color = TangemTheme.colors.background.primary),
        ) {
            AlreadyActivatedScreen(
                onThisIsMyWalletClick = {},
                onNewCardClick = {},
            )
        }
    }
}