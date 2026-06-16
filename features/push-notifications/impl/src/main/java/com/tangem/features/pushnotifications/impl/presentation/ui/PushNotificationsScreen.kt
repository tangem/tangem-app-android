package com.tangem.features.pushnotifications.impl.presentation.ui

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.showcase.Showcase
import com.tangem.core.ui.components.showcase.model.ShowcaseButtonModel
import com.tangem.core.ui.components.showcase.model.ShowcaseItemModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.requestPermission
import com.tangem.feature.pushnotifications.impl.R
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PushNotificationsScreen(
    isPushNotificationSettingsEnabled: Boolean,
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

    val argumentTwoTitleRes = if (isPushNotificationSettingsEnabled) {
        R.string.user_push_notification_agreement_argument_two_title_v2
    } else {
        R.string.user_push_notification_agreement_argument_two_title
    }
    val argumentTwoSubtitleRes = if (isPushNotificationSettingsEnabled) {
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
}