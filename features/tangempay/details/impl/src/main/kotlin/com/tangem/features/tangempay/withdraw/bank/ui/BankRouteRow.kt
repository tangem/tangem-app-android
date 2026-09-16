package com.tangem.features.tangempay.withdraw.bank.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_down_20
import com.tangem.domain.pay.model.bankofframp.BankRail

/** Rail pill: rail icon, name, ETA badge and fee; a non-null [onClick] makes the row tappable and adds a chevron. */
@Composable
internal fun BankRouteRow(
    rail: BankRail,
    title: TextReference,
    eta: TextReference,
    fee: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.secondary)
            .then(
                if (onClick != null) {
                    Modifier.clickableSingle(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BankRailIcon(rail = rail, size = 20.dp)
        Text(
            modifier = Modifier.weight(1f),
            text = title.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        TangemBadge(text = eta, size = TangemBadge.Size.X6)
        Text(
            text = fee,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        if (onClick != null) {
            ChevronDownIcon()
        }
    }
}

@Composable
internal fun ChevronDownIcon(modifier: Modifier = Modifier) {
    TangemIcon(
        modifier = modifier.size(20.dp),
        tangemIconUM = TangemIconUM.Icon(
            imageVector = Icons.ic_chevron_down_20,
            tintReference = { TangemTheme.colors3.icon.secondary },
        ),
    )
}