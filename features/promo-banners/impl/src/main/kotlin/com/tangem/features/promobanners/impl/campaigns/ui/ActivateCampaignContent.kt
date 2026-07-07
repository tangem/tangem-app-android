package com.tangem.features.promobanners.impl.campaigns.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.entity.ActivateCampaignUM

@Composable
internal fun ActivateCampaignContent(
    state: ActivateCampaignUM,
    onSelectTokenClick: () -> Unit,
    onEnrollClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            // TODO([REDACTED_TASK_KEY]): replace placeholder with the real campaign illustration.
            painter = painterResource(R.drawable.ill_businessman_3d),
            contentDescription = null,
            modifier = Modifier
                .size(TangemTheme.dimens.size96)
                .clip(CircleShape),
        )
        SpacerH16()
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH8()
        Text(
            text = state.description.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH12()
        Text(
            // TODO([REDACTED_TASK_KEY]): localize
            text = "Learn more",
            style = TangemTheme.typography.button,
            color = TangemTheme.colors.text.accent,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onLearnMoreClick),
        )

        val selectedToken = state.selectedToken
        if (selectedToken != null) {
            SpacerH24()
            Text(
                text = "Select cashback account", // TODO([REDACTED_TASK_KEY]): localize
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH12()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
                    .background(TangemTheme.colors.background.primary)
                    .padding(vertical = TangemTheme.dimens.spacing12),
            ) {
                TokenItem(state = selectedToken, isBalanceHidden = false)
            }
        }

        SpacerH24()

        Footer(
            campaignName = state.campaignName,
            hasSelectedToken = selectedToken != null,
            onSelectTokenClick = onSelectTokenClick,
            onEnrollClick = onEnrollClick,
        )
    }
}

@Composable
private fun Footer(
    campaignName: String,
    hasSelectedToken: Boolean,
    onSelectTokenClick: () -> Unit,
    onEnrollClick: () -> Unit,
) {
    if (hasSelectedToken) {
        Text(
            text = "I agree with $campaignName Terms", // TODO([REDACTED_TASK_KEY]): localize + clickable terms
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH12()
    }

    PrimaryButton(
        modifier = Modifier.fillMaxWidth(),
        text = if (hasSelectedToken) {
            "Enroll" // TODO([REDACTED_TASK_KEY]): localize
        } else {
            "Select token" // TODO([REDACTED_TASK_KEY]): localize
        },
        onClick = if (hasSelectedToken) onEnrollClick else onSelectTokenClick,
    )
    SpacerH16()
}