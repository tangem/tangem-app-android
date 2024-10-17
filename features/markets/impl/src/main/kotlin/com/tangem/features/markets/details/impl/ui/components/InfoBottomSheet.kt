package com.tangem.features.markets.details.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetTitle
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent
import com.tangem.features.markets.impl.R
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
internal fun InfoBottomSheet(config: TangemBottomSheetConfig) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    TangemBottomSheet<InfoBottomSheetContent>(
        config = config,
        addBottomInsets = false,
        title = { TangemBottomSheetTitle(title = it.title) },
        content = { content ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TangemTheme.dimens.spacing16),
            ) {
                MarkdownText(
                    markdown = content.body.resolveReference(),
                    disableLinkMovementMethod = true,
                    linkifyMask = 0,
                    syntaxHighlightColor = TangemTheme.colors.text.secondary,
                    style = TangemTheme.typography.body2.copy(
                        color = TangemTheme.colors.text.secondary,
                    ),
                )

                if (content.generatedAINotificationUM != null) {
                    AdditionalInfoNotification(
                        onClick = content.generatedAINotificationUM.onClick,
                        modifier = Modifier
                            .padding(top = TangemTheme.dimens.spacing12, bottom = TangemTheme.dimens.spacing16)
                            .fillMaxWidth(),
                    )
                }

                SpacerH(bottomBarHeight)
            }
        },
    )
}

@Composable
private fun AdditionalInfoNotification(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Notification(
        config = NotificationConfig(
            subtitle = TextReference.Res(id = R.string.information_generated_with_ai),
            iconResId = R.drawable.ic_magic_28,
            onClick = onClick,
            showArrowIcon = false,
        ),
        modifier = modifier,
        subtitleColor = TangemTheme.colors.text.primary1,
        containerColor = TangemTheme.colors.button.disabled,
        iconTint = TangemTheme.colors.icon.accent,
    )
}