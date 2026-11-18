package com.tangem.features.onramp.alloffers.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.features.onramp.alloffers.entity.OnrampRegionRestrictionNotification
import com.tangem.utils.annotations.RemoveWithToggle

@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
@Composable
internal fun RegionRestrictionBannerDs(notification: NotificationUM, modifier: Modifier = Modifier) {
    val config = notification.config
    TangemThemeRedesign {
        TangemMessageBanner(
            title = config.title ?: config.subtitle,
            description = config.subtitle.takeIf { config.title != null },
            variant = TangemMessageBanner.Variant.Warning,
            slotEnd = {
                TangemIcon(
                    tangemIconUM = TangemIconUM.Icon(
                        imageVector = Icons.ic_info_20,
                        tintReference = { TangemTheme.colors3.icon.primary },
                    ),
                )
            },
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegionRestrictionBannerDsPreview() {
    TangemThemePreviewRedesign {
        RegionRestrictionBannerDs(
            notification = OnrampRegionRestrictionNotification,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}