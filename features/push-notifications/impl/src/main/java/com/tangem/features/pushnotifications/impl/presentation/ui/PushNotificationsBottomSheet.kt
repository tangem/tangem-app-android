package com.tangem.features.pushnotifications.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH28
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.showcase.ShowcaseButtons
import com.tangem.core.ui.components.showcase.ShowcaseContent
import com.tangem.core.ui.components.showcase.model.ShowcaseItemModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.requestPermission
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PushNotificationsBottomSheet(config: TangemBottomSheetConfig, content: @Composable () -> Unit) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent>(
        config = config,
        title = {
            TangemModalBottomSheetTitle(
                endIconRes = R.drawable.ic_close_24,
                onEndClick = config.onDismissRequest,
            )
        },
    ) {
        content()
    }
}

@Composable
internal fun PushNotificationsContent(
    onAllowClick: () -> Unit,
    onLaterClick: () -> Unit,
    onAllowPermission: () -> Unit,
    onDenyPermission: () -> Unit,
) {
    val requestPushPermission = requestPermission(
        onAllow = onAllowPermission,
        onDeny = onDenyPermission,
        permission = PUSH_PERMISSION,
    )

    Column(modifier = Modifier.background(TangemTheme.colors.background.primary)) {
        ShowcaseContent(
            headerIconRes = R.drawable.ic_notifications_unread_24,
            headerText = resourceReference(R.string.user_push_notification_agreement_header),
            showcaseItems = persistentListOf(
                ShowcaseItemModel(
                    R.drawable.ic_rocket_launch_24,
                    resourceReference(R.string.user_push_notification_agreement_argument_one),
                ),
                ShowcaseItemModel(
                    R.drawable.ic_storefront_24,
                    resourceReference(R.string.user_push_notification_agreement_argument_two),
                ),
                ShowcaseItemModel(
                    R.drawable.ic_notifications_24,
                    resourceReference(R.string.user_push_notification_agreement_argument_three),
                ),
            ),
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing40),
        )
        SpacerH28()
        ShowcaseButtons(
            primaryButtonText = resourceReference(R.string.common_allow),
            onPrimaryClick = {
                onAllowClick()
                requestPushPermission()
            },
            secondaryButtonText = resourceReference(R.string.common_later),
            onSecondaryClick = {
                onLaterClick()
            },
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_PushNotificationsBottomSheet() {
    TangemThemePreview {
        PushNotificationsBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
        ) {
            PushNotificationsContent(
                onAllowClick = {},
                onLaterClick = {},
                onAllowPermission = {},
                onDenyPermission = {},
            )
        }
    }
}