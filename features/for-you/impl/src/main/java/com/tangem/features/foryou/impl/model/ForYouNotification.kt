package com.tangem.features.foryou.impl.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cloud_exclamation_20
import com.tangem.features.foryou.impl.R

@Immutable
internal sealed class ForYouNotification(val state: TangemMessageBanner.State) {

    data object UsedOutdatedData : ForYouNotification(
        state = TangemMessageBanner.State(
            title = resourceReference(R.string.warning_some_token_balances_not_updated),
            iconEnd = TangemIconUM.Icon(
                imageVector = Icons.ic_cloud_exclamation_20,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
            variant = TangemMessageBanner.Variant.Warning,
            shouldShowGlowRing = false,
        ),
    )
}