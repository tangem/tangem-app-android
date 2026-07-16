package com.tangem.features.feed.ui.earn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun BestOpportunitiesEmptyFiltered(onClearFilterClick: () -> Unit, modifier: Modifier = Modifier) {
    BestOpportunitiesEmptyFilteredV2(onClearFilterClick, modifier)
}

@Composable
private fun BestOpportunitiesEmptyFilteredV2(onClearFilterClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens2.x4)
            .background(
                color = TangemTheme.colors2.surface.level3,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            )
            .padding(vertical = TangemTheme.dimens2.x8, horizontal = TangemTheme.dimens2.x3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.earn_no_results),
            style = TangemTheme.typography2.subheadlineMedium14,
            color = TangemTheme.colors2.text.neutral.secondary,
        )
        SpacerH(TangemTheme.dimens2.x2)
        TangemButton(
            buttonUM = TangemButtonUM(
                text = resourceReference(R.string.earn_clear_filter),
                onClick = onClearFilterClick,
                type = TangemButtonType.Secondary,
                size = TangemButtonSize.X8,
                shape = TangemButtonShape.Rounded,
            ),
        )
    }
}