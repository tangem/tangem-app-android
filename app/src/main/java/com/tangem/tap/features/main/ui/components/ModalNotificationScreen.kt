package com.tangem.tap.features.main.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.main.model.ActionConfig
import com.tangem.tap.features.main.model.ModalNotification
import com.tangem.wallet.R

@Composable
internal fun ModalNotificationContent(notification: ModalNotification, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing40),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Hand()
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size48),
            painter = painterResource(notification.iconResId),
            tint = TangemTheme.colors.icon.primary1,
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = notification.title.resolveReference(),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            Text(
                text = notification.message.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            modifier = Modifier
                .padding(bottom = TangemTheme.dimens.spacing16)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = notification.primaryAction.text.resolveReference(),
                onClick = notification.primaryAction.onClick,
            )
            when (val secondaryAction = notification.secondaryAction) {
                null -> Unit
                else -> SecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = secondaryAction.text.resolveReference(),
                    onClick = secondaryAction.onClick,
                )
            }
        }
    }
}

// region Preview
@Preview(widthDp = 360, heightDp = 404)
@Preview(widthDp = 360, heightDp = 404, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ModalNotificationContentPreview(
    @PreviewParameter(GlobalNotificationProvider::class) param: ModalNotification,
) {
    TangemThemePreview {
        ModalNotificationContent(
            notification = param,
            modifier = Modifier.background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.bottomSheet,
            ),
        )
    }
}

private class GlobalNotificationProvider : CollectionPreviewParameterProvider<ModalNotification>(
    collection = listOf(
        ModalNotification(
            iconResId = R.drawable.ic_eye_off_outline_24,
            title = resourceReference(R.string.balance_hidden_title),
            message = resourceReference(R.string.balance_hidden_description),
            primaryAction = ActionConfig(
                text = resourceReference(R.string.balance_hidden_got_it_button),
                onClick = {},
            ),
            secondaryAction = ActionConfig(
                text = resourceReference(R.string.balance_hidden_do_not_show_button),
                onClick = {},
            ),
        ),
    ),
)
// endregion Preview