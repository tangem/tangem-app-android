package com.tangem.managetokens.presentation.managetokens.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButtonIconEndTwoLines
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.managetokens.state.previewdata.DerivationNotificationStatePreviewData

@Composable
internal fun DerivationNotification(config: NotificationConfig, modifier: Modifier = Modifier) {
    BaseContainer(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing16),
            verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing16),
        ) {
            MainContent(
                iconResId = config.iconResId,
                iconTint = TangemTheme.colors.icon.accent,
                title = config.title,
                subtitle = config.subtitle,
            )
            val buttonConfig = config.buttonsState
            if (buttonConfig is NotificationConfig.ButtonsState.PrimaryButtonConfig) {
                PrimaryButtonIconEndTwoLines(
                    text = buttonConfig.text.resolveReference(),
                    iconResId = buttonConfig.iconResId ?: R.drawable.ic_tangem_24,
                    onClick = buttonConfig.onClick,
                    modifier = Modifier
                        .fillMaxWidth(),
                    additionalText = buttonConfig.additionalText?.resolveReference(),
                )
            }
        }
    }
}

@Composable
private fun BaseContainer(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Card(
        modifier = modifier
            .defaultMinSize(minHeight = TangemTheme.dimens.size62)
            .fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = TangemTheme.dimens.radius16,
            topEnd = TangemTheme.dimens.radius16,
        ),
        elevation = TangemTheme.dimens.elevation12,
        backgroundColor = TangemTheme.colors.background.action,
    ) {
        Box(content = content)
    }
}

@Composable
private fun MainContent(iconResId: Int, iconTint: Color, title: TextReference, subtitle: TextReference) {
    Row {
        NotificationIcon(iconResId = iconResId, iconTint = iconTint)
        SpacerW(width = TangemTheme.dimens.spacing10)
        TextsBlock(title = title, subtitle = subtitle)
    }
}

@Composable
private fun RowScope.NotificationIcon(iconResId: Int, iconTint: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .align(alignment = Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(TangemTheme.dimens.size36)
                .background(
                    color = iconTint.copy(alpha = 0.12f),
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .size(TangemTheme.dimens.size16)
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = CircleShape,
                ),
        )
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = iconTint,
        )
    }
}

@Composable
private fun TextsBlock(title: TextReference, subtitle: TextReference) {
    Column(verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing2)) {
        Text(
            text = title.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.button,
        )

        Text(
            text = subtitle.resolveReference(),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption2,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ManageTokensScreen() {
    TangemThemePreview {
        DerivationNotification(DerivationNotificationStatePreviewData.state.config)
    }
}
