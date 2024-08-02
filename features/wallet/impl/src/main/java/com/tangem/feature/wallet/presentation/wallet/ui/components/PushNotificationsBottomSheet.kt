package com.tangem.feature.wallet.presentation.wallet.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.showcase.ShowcaseContent
import com.tangem.core.ui.components.showcase.model.ShowcaseItemModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.requestPushPermission
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.PushNotificationsBottomSheetConfig
import com.tangem.features.pushnotifications.api.utils.getPushPermissionOrNull
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PushNotificationsBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<PushNotificationsBottomSheetConfig>(config = config) {
        PushNotificationsSheetContent(content = it, onDismiss = config.onDismissRequest)
    }
}

@Composable
private fun PushNotificationsSheetContent(content: PushNotificationsBottomSheetConfig, onDismiss: () -> Unit) {
    val isClicked = remember { mutableStateOf(false) }
    val requestPushPermission = requestPushPermission(
        pushPermission = getPushPermissionOrNull(),
        isFirstTimeAsking = content.isFirstTimeRequested,
        isClicked = isClicked,
        onAllow = {
            content.onAllow()
            onDismiss()
        },
        onDeny = {
            content.onDeny()
            onDismiss()
        },
        onOpenSettings = content.openSettings,
    )

    Column(modifier = Modifier.background(TangemTheme.colors.background.primary)) {
        ShowcaseContent(
            headerIconRes = R.drawable.ic_notifications_unread_24,
            headerText = resourceReference(R.string.user_push_notification_agreement_header),
            showcaseItems = persistentListOf(
                ShowcaseItemModel(
                    iconRes = R.drawable.ic_rocket_launch_24,
                    text = resourceReference(R.string.user_push_notification_agreement_argument_one),
                ),
                ShowcaseItemModel(
                    iconRes = R.drawable.ic_storefront_24,
                    text = resourceReference(R.string.user_push_notification_agreement_argument_two),
                ),
            ),
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing40),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            modifier = Modifier.padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                top = TangemTheme.dimens.spacing40,
                bottom = TangemTheme.dimens.spacing16,
            ),
        ) {
            SecondaryButton(
                text = if (content.wasInitiallyAsk) {
                    stringResource(R.string.common_later)
                } else {
                    stringResource(R.string.common_cancel)
                },
                onClick = {
                    content.onRequestLater()
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(R.string.common_allow),
                onClick = {
                    isClicked.value = true
                    content.onRequest()
                    requestPushPermission()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PushNotificationsSheetContent_Preview() {
    TangemThemePreview {
        PushNotificationsSheetContent(
            PushNotificationsBottomSheetConfig(
                isFirstTimeRequested = false,
                wasInitiallyAsk = false,
                onRequest = {},
                onRequestLater = {},
                onAllow = {},
                onDeny = {},
                openSettings = {},
            ),
            onDismiss = {},
        )
    }
}
// endregion