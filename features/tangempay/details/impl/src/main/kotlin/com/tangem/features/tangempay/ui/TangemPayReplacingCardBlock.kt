package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayReplacingCardBlock(modifier: Modifier = Modifier) {
    Notification(
        modifier = modifier,
        config = NotificationConfig(
            iconResId = R.drawable.ic_update_32,
            iconTint = NotificationConfig.IconTint.Accent,
            title = resourceReference(R.string.tangempay_reissue_card_in_progress),
            subtitle = resourceReference(R.string.tangempay_reissue_card_in_progress_description),
        ),
        containerColor = TangemTheme.colors.background.primary,
    )
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        TangemPayReplacingCardBlock()
    }
}