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
    onAllowClick: () -> Unit,
    onLaterClick: () -> Unit,
    onAllowPermission: () -> Unit,
    onDenyPermission: () -> Unit,
    showNotificationsInfo: Boolean,
) {
    val requestPushPermission = requestPermission(
        onAllow = onAllowPermission,
        onDeny = onDenyPermission,
        permission = PUSH_PERMISSION,
    )

    Showcase(
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
        ).let { baseItems ->
            if (showNotificationsInfo) {
                baseItems.add(
                    ShowcaseItemModel(
                        R.drawable.ic_notifications_24,
                        resourceReference(R.string.user_push_notification_agreement_argument_three),
                    ),
                )
            } else {
                baseItems
            }
        },
        primaryButton = ShowcaseButtonModel(
            buttonText = resourceReference(R.string.common_allow),
            onClick = {
                onAllowClick()
                requestPushPermission()
            },
        ),
        secondaryButton = ShowcaseButtonModel(
            buttonText = resourceReference(R.string.common_later),
            onClick = {
                onLaterClick()
            },
        ),
        modifier = Modifier.systemBarsPadding(),
    )
}