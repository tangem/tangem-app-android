package com.tangem.features.feed.ui.earn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun BestOpportunitiesEmpty(modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        BestOpportunitiesEmptyV2(modifier)
    } else {
        BestOpportunitiesEmptyV1(modifier)
    }
}

@Composable
private fun BestOpportunitiesEmptyV1(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = TangemTheme.colors.background.action,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .padding(vertical = 32.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .fillMaxWidth(),
            painter = painterResource(R.drawable.ic_empty_64),
            contentDescription = null,
            tint = Color.Unspecified,
        )
        SpacerH(TangemTheme.dimens2.x6)
        Text(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens2.x8),
            text = stringResourceSafe(R.string.earn_empty),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BestOpportunitiesEmptyV2(modifier: Modifier = Modifier) {
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
        Icon(
            modifier = Modifier
                .fillMaxWidth(),
            painter = painterResource(R.drawable.ic_empty_64),
            contentDescription = null,
            tint = TangemTheme.colors2.graphic.neutral.quaternary,
        )
        SpacerH(TangemTheme.dimens2.x5)
        Text(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x8),
            text = stringResourceSafe(R.string.earn_empty),
            style = TangemTheme.typography2.bodyRegular14,
            color = TangemTheme.colors2.text.neutral.secondary,
            textAlign = TextAlign.Center,
        )
    }
}