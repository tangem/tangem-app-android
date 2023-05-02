package com.tangem.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

/**
 * Closable notification with custom icon
 * Child of parent component
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=1045-807&t=6CVvYDJe0sB7wBKE-0">Figma component</a>
 *
 * Use to show banner with custom icon and possibility to close
 * i.e. Feedback notification
 *
 * @param title notification title
 * @param icon drawable res on icon
 * @param iconColor icon color
 * @param onClick callback on click
 * @param onCloseClick callback on close icon click
 */
@Composable
fun ClosableNotification(
    title: String,
    @DrawableRes icon: Int,
    iconColor: Color,
    onClick: (() -> Unit),
    onCloseClick: (() -> Unit),
) {
    NotificationCardTemplate(onClick) {
        Icon(
            modifier = Modifier
                .size(TangemTheme.dimens.size20)
                .align(Alignment.CenterStart),
            painter = painterResource(id = icon),
            tint = iconColor,
            contentDescription = null,
        )
        Text(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing28)
                .align(Alignment.CenterStart),
            text = title,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle2,
        )
        Icon(
            modifier = Modifier
                .size(TangemTheme.dimens.size20)
                .align(Alignment.CenterEnd)
                .clickable(onClick = onCloseClick),
            painter = painterResource(id = R.drawable.ic_close_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

/**
 * Notification component from Design system
 * There are few states for this component, but only one parent, see link below
 *
 * Use this for Notification with title, subtitle, clickable or not
 *
 * @param title notification title
 * @param subtitle notification subtitle
 * @param onClick click on notification, if its null then no chevron icon
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=1045-807&t=6CVvYDJe0sB7wBKE-0">Figma component</a>
 */
@Composable
fun WarningNotification(title: String, subtitle: String?, onClick: (() -> Unit)?) {
    NotificationCardTemplate(onClick) {
        Image(
            modifier = Modifier
                .size(TangemTheme.dimens.size20)
                .align(Alignment.CenterStart),
            painter = painterResource(id = R.drawable.img_attention_20),
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing28)
                .align(Alignment.CenterStart),
        ) {
            Text(
                text = title,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle2,
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
        if (onClick != null) {
            Icon(
                modifier = Modifier
                    .size(TangemTheme.dimens.size20)
                    .align(Alignment.CenterEnd),
                painter = painterResource(id = R.drawable.ic_chevron_right_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NotificationCardTemplate(onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    Surface(
        color = TangemTheme.colors.button.secondary,
        shape = RoundedCornerShape(TangemTheme.dimens.radius18),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Box(
            Modifier
                .padding(
                    horizontal = TangemTheme.dimens.spacing12,
                    vertical = TangemTheme.dimens.spacing8,
                )
                .wrapContentSize(),
        ) {
            content()
        }
    }
}

// region Preview

@Composable
private fun WarningNotificationPreview() {
    Column(modifier = Modifier.fillMaxWidth()) {
        WarningNotification(
            title = "Your wallet hasn’t been backed up",
            subtitle = "Lorem ipsum dolor sit amet, consectetur " +
                "adipiscing elit, sed do eiusmod tempor incididunt ut labore et...",
            onClick = {},
        )
        SpacerH32()
        WarningNotification(
            title = "Your wallet hasn’t been backed up",
            subtitle = null,
            onClick = {},
        )
        SpacerH32()
        WarningNotification(
            title = "Your wallet hasn’t been backed up",
            subtitle = "Lorem ipsum dolor sit amet, consectetur " +
                "adipiscing elit, sed do eiusmod tempor incididunt ut labore et...",
            onClick = null,
        )
        SpacerH32()
        WarningNotification(
            title = "Your wallet hasn’t been backed up",
            subtitle = null,
            onClick = null,
        )
        SpacerH32()
        ClosableNotification(
            title = "Like tangem app?",
            icon = R.drawable.ic_star_24,
            iconColor = TangemTheme.colors.icon.attention,
            onClick = {},
            onCloseClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_WarningNotification_InLightTheme() {
    TangemTheme(isDark = false) {
        WarningNotificationPreview()
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_WarningNotification_InDarkTheme() {
    TangemTheme(isDark = true) {
        WarningNotificationPreview()
    }
}

// endregion Preview