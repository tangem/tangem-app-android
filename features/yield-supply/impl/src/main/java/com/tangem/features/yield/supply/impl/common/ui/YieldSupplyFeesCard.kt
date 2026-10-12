package com.tangem.features.yield.supply.impl.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.yield.supply.impl.R

/**
 * Network fee above service fee, the order the claim sheet and its result screen both show them in.
 *
 * @param isServiceFeeVisible false while the owner cannot pay the network fee at all: what Tangem would take out of
 * the rewards is noise next to a transaction that cannot be sent.
 */
@Composable
internal fun YieldSupplyFeesCard(
    serviceFee: TextReference?,
    networkFee: TextReference?,
    modifier: Modifier = Modifier,
    networkFeeCurrency: TextReference? = null,
    isServiceFeeVisible: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TangemTheme.colors.background.action)
            .padding(vertical = 4.dp),
    ) {
        YieldSupplyFeeRow(
            title = resourceReference(R.string.common_network_fee_title),
            value = networkFee,
            badge = networkFeeCurrency,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        if (isServiceFeeVisible) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = TangemTheme.colors.stroke.primary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            YieldSupplyFeeRow(
                title = resourceReference(R.string.yield_module_claim_service_fee_title),
                value = serviceFee,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}