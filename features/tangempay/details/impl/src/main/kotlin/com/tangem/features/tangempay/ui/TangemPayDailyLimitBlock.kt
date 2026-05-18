package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayDailyLimitBlockState

@Composable
internal fun TangemPayDailyLimitBlock(state: TangemPayDailyLimitBlockState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersMedium,
            )
            .padding(TangemTheme.dimens.spacing12),
    ) {
        Text(
            text = stringResourceSafe(R.string.tangempay_card_page_daily_limit_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        SpacerH4()
        CurrentLimitBlock(state)
    }
}

@Composable
private fun CurrentLimitBlock(state: TangemPayDailyLimitBlockState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = TangemTheme.colors.icon.accent.copy(alpha = 0.1f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_limit_20),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
            )
        }
        SpacerW12()
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResourceSafe(R.string.tangempay_card_page_daily_limit_current_limit),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            when (state) {
                TangemPayDailyLimitBlockState.Error,
                is TangemPayDailyLimitBlockState.Content,
                -> Text(
                    text = if (state is TangemPayDailyLimitBlockState.Content) state.limit else "—",
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TangemPayDailyLimitBlockState.Loading -> TextShimmer(
                    style = TangemTheme.typography.subtitle1,
                    text = "$50,000",
                )
            }
        }
        SpacerW8()
        if (state is TangemPayDailyLimitBlockState.Content) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                SecondaryButton(
                    modifier = Modifier,
                    text = stringResourceSafe(R.string.tangempay_card_page_daily_limit_change),
                    onClick = state.onChangeClick,
                    size = TangemButtonSize.Small,
                )
            }
        }
    }
}

@Composable
internal fun TangemPayDailyLimitErrorBlock(modifier: Modifier = Modifier) {
    Notification(
        config = NotificationConfig(
            title = resourceReference(R.string.tangempay_card_page_daily_limit_error_title),
            subtitle = resourceReference(R.string.tangempay_card_page_daily_limit_error_description),
            iconResId = R.drawable.img_attention_20,
        ),
        containerColor = TangemTheme.colors.background.action,
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun preview() = TangemThemePreview {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Content.stub())
        TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Error)
        TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Loading)
        TangemPayDailyLimitErrorBlock()
    }
}