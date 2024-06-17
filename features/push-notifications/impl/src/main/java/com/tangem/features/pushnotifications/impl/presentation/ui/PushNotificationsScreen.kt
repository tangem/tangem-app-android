package com.tangem.features.pushnotifications.impl.presentation.ui

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.showcase.Showcase
import com.tangem.core.ui.components.showcase.model.ShowcaseButtonModel
import com.tangem.core.ui.components.showcase.model.ShowcaseItemModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.requestPushPermission
import com.tangem.feature.pushnotifications.impl.R
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PushNotificationsScreen(
    onShowAllow: () -> Unit,
    onAllow: () -> Unit,
    onLater: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val isClicked = remember { mutableStateOf(false) }
    val requestPushPermission = requestPushPermission(
        isFirstTimeAsking = true,
        isClicked = isClicked,
        onAllow = onAllow,
        onDeny = onLater,
        onOpenSettings = onOpenSettings,
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
        ),
        primaryButton = ShowcaseButtonModel(
            buttonText = resourceReference(R.string.common_allow),
            onClick = {
                isClicked.value = true
                onShowAllow()
                requestPushPermission()
            },
        ),
        secondaryButton = ShowcaseButtonModel(
            buttonText = resourceReference(R.string.common_later),
            onClick = onLater,
        ),
        modifier = Modifier.systemBarsPadding(),
    )
}