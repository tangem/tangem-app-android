package com.tangem.features.collectibles.impl.machinedetails.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_16
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM

@Composable
internal fun CollectiblesPackDetailsFooter(
    state: CollectiblesMachineDetailsUM.FooterUM,
    modifier: Modifier = Modifier,
) {
    TangemFooter(
        modifier = modifier,
        extraTop = { FooterMeta(state = state) },
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = state.buyButtonText,
            onClick = state.onBuyClick,
        )
    }
}

@Composable
private fun FooterMeta(state: CollectiblesMachineDetailsUM.FooterUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TurboMode(state = state)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResourceSafe(R.string.collectibles_machine_expected_value),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.End,
            )
            Text(
                text = state.expectedValue.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun TurboMode(state: CollectiblesMachineDetailsUM.FooterUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.clickableSingle(onClick = state.onTurboInfoClick),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Turbo mode",
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            TangemIcon(
                tangemIconUM = TangemIconUM.Icon(
                    imageVector = Icons.ic_info_16,
                    tintReference = { TangemTheme.colors3.icon.secondary },
                ),
                modifier = Modifier.size(16.dp),
            )
        }
        CollectiblesTurboSwitch(
            isChecked = state.isTurboEnabled,
            onCheckedChange = state.onTurboCheckedChange,
        )
    }
}