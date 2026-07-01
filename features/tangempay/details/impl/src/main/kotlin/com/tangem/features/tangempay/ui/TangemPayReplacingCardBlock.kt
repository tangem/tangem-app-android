package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.LocalVisaRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayReplacingCardBlock(
    modifier: Modifier = Modifier,
    title: TextReference? = resourceReference(R.string.tangempay_reissue_card_in_progress),
    subtitle: TextReference = resourceReference(R.string.tangempay_reissue_card_in_progress_description),
) {
    if (LocalVisaRedesignEnabled.current) {
        return
    } else {
        Block(title = title, subtitle = subtitle, modifier = modifier)
    }
}

@Composable
private fun Block(
    modifier: Modifier = Modifier,
    title: TextReference? = resourceReference(R.string.tangempay_reissue_card_in_progress),
    subtitle: TextReference = resourceReference(R.string.tangempay_reissue_card_in_progress_description),
) {
    Notification(
        modifier = modifier,
        config = NotificationConfig(
            iconResId = R.drawable.ic_update_32,
            iconTint = NotificationConfig.IconTint.Accent,
            title = title,
            subtitle = subtitle,
        ),
        containerColor = TangemTheme.colors.background.primary,
    )
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    Column {
        TangemThemePreview {
            TangemPayReplacingCardBlock()
        }
        SpacerH24()
        CompositionLocalProvider(LocalVisaRedesignEnabled provides true) {
            TangemThemePreviewRedesign {
                TangemPayReplacingCardBlock()
            }
        }
    }
}