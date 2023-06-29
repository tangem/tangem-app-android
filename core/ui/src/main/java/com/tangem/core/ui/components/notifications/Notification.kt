package com.tangem.core.ui.components.notifications

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH2
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme

/**
 * Notification component from Design system.
 * Use this for Notification with title, subtitle, clickable or not.
 *
 * @param state    component state
 * @param modifier modifier
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=1045-807&t=6CVvYDJe0sB7wBKE-0"
 * >Figma component</a>
 */
@Composable
fun Notification(state: NotificationState, modifier: Modifier = Modifier) {
    Surface(
        onClick = if (state is NotificationState.Action) {
            state.onClick
        } else {
            {}
        },
        modifier = modifier,
        enabled = when (state) {
            is NotificationState.Simple -> false
            is NotificationState.Action -> true
        },
        shape = RoundedCornerShape(TangemTheme.dimens.radius18),
        color = TangemTheme.colors.button.secondary,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing12, vertical = TangemTheme.dimens.spacing8),
        ) {
            NotificationIcon(
                iconResId = state.iconResId,
                iconTint = state.tint,
                modifier = Modifier
                    .size(size = TangemTheme.dimens.size20)
                    .align(alignment = Alignment.CenterStart),
            )

            NotificationInfoBlock(
                title = state.title,
                subtitle = state.subtitle,
                modifier = Modifier.align(alignment = Alignment.CenterStart),
            )

            if (state is NotificationState.Action) {
                Icon(
                    modifier = Modifier
                        .size(size = TangemTheme.dimens.size20)
                        .align(alignment = Alignment.CenterEnd),
                    painter = painterResource(id = R.drawable.ic_chevron_right_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.informative,
                )
            }
        }
    }
}

@Composable
private fun NotificationIcon(@DrawableRes iconResId: Int, iconTint: Color?, modifier: Modifier = Modifier) {
    if (iconTint != null) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = modifier,
            tint = iconTint,
        )
    } else {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = modifier,
        )
    }
}

@Composable
private fun NotificationInfoBlock(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = TangemTheme.dimens.spacing30)) {
        Text(
            text = title,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.body2,
        )

        if (!subtitle.isNullOrEmpty()) {
            SpacerH2()
            Text(
                text = subtitle,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption,
            )
        }
    }
}

@Preview
@Composable
private fun Preview_WarningNotification_Light(
    @PreviewParameter(NotificationStateProvider::class)
    state: NotificationState,
) {
    TangemTheme(isDark = false) {
        Notification(state)
    }
}

@Preview
@Composable
private fun Preview_WarningNotification_Dark(
    @PreviewParameter(NotificationStateProvider::class)
    state: NotificationState,
) {
    TangemTheme(isDark = true) {
        Notification(state)
    }
}

private class NotificationStateProvider : CollectionPreviewParameterProvider<NotificationState>(
    collection = listOf(
        NotificationState.Simple(
            title = "Your wallet hasn’t been backed up",
            subtitle = "Lorem ipsum dolor sit amet, consectetur " +
                "adipiscing elit, sed do eiusmod tempor incididunt ut labore et...",
            iconResId = R.drawable.img_attention_20,
        ),
        NotificationState.Simple(
            title = "Your wallet hasn’t been backed up",
            subtitle = null,
            iconResId = R.drawable.ic_alert_circle_24,
            tint = TangemColorPalette.Amaranth,
        ),
        NotificationState.Action(
            title = "Your wallet hasn’t been backed up",
            subtitle = "Lorem ipsum dolor sit amet, consectetur " +
                "adipiscing elit, sed do eiusmod tempor incididunt ut labore et...",
            iconResId = R.drawable.img_attention_20,
            onClick = {},
        ),
        NotificationState.Action(
            title = "Your wallet hasn’t been backed up",
            subtitle = null,
            iconResId = R.drawable.ic_alert_circle_24,
            tint = TangemColorPalette.Amaranth,
            onClick = {},
        ),
    ),
)
