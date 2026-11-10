package com.tangem.features.onramp.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.common.ui.notifications.MessageBanner
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.message.TangemMessageIconPosition
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.utils.annotations.RemoveWithToggle

@Composable
@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
internal fun BuyNotSupportedBannerDs(messageUM: TangemMessageUM, modifier: Modifier = Modifier) {
    MessageBanner(
        messageUM = messageUM.copy(
            variant = TangemMessageBanner.Variant.Warning,
            iconUM = TangemIconUM.Icon(
                imageVector = Icons.ic_info_20,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
            iconPosition = TangemMessageIconPosition.Trailing,
        ),
        modifier = modifier,
    )
}