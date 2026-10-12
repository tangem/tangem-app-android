package com.tangem.features.polymarket.impl.positiondetails.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_16
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Notice

@Composable
internal fun PolymarketPositionNotice(notice: Notice, modifier: Modifier = Modifier) {
    val (title, description) = when (notice) {
        Notice.Pending -> R.string.prediction_positions_pending_title to
            R.string.prediction_positions_pending_description
        Notice.Disputed -> R.string.prediction_positions_disputed_title to
            R.string.prediction_positions_disputed_description
    }
    TangemMessageBanner(
        modifier = modifier,
        title = resourceReference(title),
        description = resourceReference(description),
        variant = TangemMessageBanner.Variant.Warning,
        slotEnd = {
            Icon(
                imageVector = Icons.ic_info_16,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
                modifier = Modifier.size(16.dp),
            )
        },
    )
}