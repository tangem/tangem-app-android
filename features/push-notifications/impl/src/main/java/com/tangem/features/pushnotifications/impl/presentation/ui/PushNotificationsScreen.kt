package com.tangem.features.pushnotifications.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheet
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.icon
import com.tangem.core.ui.components.bottomsheets.message.infoBlock
import com.tangem.core.ui.components.bottomsheets.message.messageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.onClick
import com.tangem.core.ui.components.bottomsheets.message.primaryButton
import com.tangem.core.ui.components.bottomsheets.message.secondaryButton
import com.tangem.core.ui.components.showcase.Showcase
import com.tangem.core.ui.components.showcase.model.ShowcaseButtonModel
import com.tangem.core.ui.components.showcase.model.ShowcaseItemModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.requestPermission
import com.tangem.feature.pushnotifications.impl.R
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import kotlinx.collections.immutable.persistentListOf

/**
 * Holds the treatment-variant "Double Ask" bottom sheet state and callbacks for the onboarding soft-ask.
 */
@Immutable
internal data class PushNotificationsDoubleAskSheetState(
    val isShown: Boolean,
    val onEnableClick: () -> Unit,
    val onSkipClick: () -> Unit,
    val onDismiss: () -> Unit,
)

@Immutable
internal data class PushNotificationsUM(
    val isPushNotificationSettingsEnabled: Boolean,
    val doubleAskSheet: PushNotificationsDoubleAskSheetState,
)

@Composable
internal fun PushNotificationsScreen(
    state: PushNotificationsUM,
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

    val argumentTwoTitleRes = if (state.isPushNotificationSettingsEnabled) {
        R.string.user_push_notification_agreement_argument_two_title_v2
    } else {
        R.string.user_push_notification_agreement_argument_two_title
    }
    val argumentTwoSubtitleRes = if (state.isPushNotificationSettingsEnabled) {
        R.string.user_push_notification_agreement_argument_two_subtitle_v2
    } else {
        R.string.user_push_notification_agreement_argument_two_subtitle
    }

    Showcase(
        headerIconRes = R.drawable.ic_notification_56,
        headerText = resourceReference(R.string.user_push_notification_agreement_header),
        showcaseItems = persistentListOf(
            ShowcaseItemModel(
                iconRes = R.drawable.ic_notification_square_24,
                title = resourceReference(R.string.user_push_notification_agreement_argument_one_title),
                subTitle = resourceReference(R.string.user_push_notification_agreement_argument_one_subtitle),
            ),
            ShowcaseItemModel(
                iconRes = R.drawable.ic_stars_24,
                title = resourceReference(argumentTwoTitleRes),
                subTitle = resourceReference(argumentTwoSubtitleRes),
            ),
        ),
        primaryButton = ShowcaseButtonModel(
            buttonText = resourceReference(R.string.common_allow),
            onClick = {
                onAllowClick()
                requestPushPermission()
            },
        ),
        secondaryButton = ShowcaseButtonModel(
            buttonText = resourceReference(R.string.common_later),
            onClick = onLaterClick,
        ),
        modifier = Modifier.systemBarsPadding(),
    )

    if (state.doubleAskSheet.isShown) {
        PushNotificationsDoubleAskBottomSheet(
            onEnableClick = {
                state.doubleAskSheet.onEnableClick()
                requestPushPermission()
            },
            onSkipClick = state.doubleAskSheet.onSkipClick,
            onDismiss = state.doubleAskSheet.onDismiss,
        )
    }
}

@Composable
private fun PushNotificationsDoubleAskBottomSheet(
    onEnableClick: () -> Unit,
    onSkipClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    MessageBottomSheet(
        state = messageBottomSheetUM {
            infoBlock {
                icon(com.tangem.core.ui.R.drawable.ic_attention_default_24) {
                    type = MessageBottomSheetUM.Icon.Type.Attention
                    backgroundType = MessageBottomSheetUM.Icon.BackgroundType.Attention
                }
                title = resourceReference(R.string.push_notification_warning_sheet_title)
                body = resourceReference(R.string.push_notification_warning_sheet_description)
            }
            primaryButton {
                text = resourceReference(R.string.push_notification_warning_sheet_button_enable)
                onClick { onEnableClick() }
            }
            secondaryButton {
                text = resourceReference(R.string.common_skip)
                onClick { onSkipClick() }
            }
        },
        onDismissRequest = onDismiss,
    )
}

private fun previewState(isDoubleAskShown: Boolean) = PushNotificationsUM(
    isPushNotificationSettingsEnabled = true,
    doubleAskSheet = PushNotificationsDoubleAskSheetState(
        isShown = isDoubleAskShown,
        onEnableClick = {},
        onSkipClick = {},
        onDismiss = {},
    ),
)

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_PushNotificationsScreen() {
    TangemThemePreview {
        PushNotificationsScreen(
            state = previewState(isDoubleAskShown = false),
            onAllowClick = {},
            onLaterClick = {},
            onAllowPermission = {},
            onDenyPermission = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_PushNotificationsScreen_DoubleAsk() {
    TangemThemePreview {
        PushNotificationsScreen(
            state = previewState(isDoubleAskShown = true),
            onAllowClick = {},
            onLaterClick = {},
            onAllowPermission = {},
            onDenyPermission = {},
        )
    }
}