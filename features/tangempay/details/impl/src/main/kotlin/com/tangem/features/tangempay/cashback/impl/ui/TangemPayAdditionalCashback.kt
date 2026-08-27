package com.tangem.features.tangempay.cashback.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayAdditionalCashbackUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayAdditionalCashback(state: TangemPayAdditionalCashbackUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(16.dp),
        ) {
            Text(
                text = resourceReference(R.string.tangempay_cashback_additional_title).resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.items.forEach { item ->
                AdditionalCashbackCard(item = item)
            }
        }
    }
}

@Composable
private fun AdditionalCashbackCard(item: TangemPayAdditionalCashbackUM.Item, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CashbackBadge(badge = item.badge)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.name.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            Text(
                text = item.description.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }
    }
}

@Composable
private fun CashbackBadge(badge: TangemPayAdditionalCashbackUM.Badge, modifier: Modifier = Modifier) {
    when (badge) {
        TangemPayAdditionalCashbackUM.Badge.Permanent -> TangemBadge(
            text = resourceReference(R.string.tangempay_cashback_additional_permanent),
            modifier = modifier,
            status = TangemBadge.Status.Neutral,
            size = TangemBadge.Size.X6,
        )
        is TangemPayAdditionalCashbackUM.Badge.Until -> TangemBadge(
            text = badge.text,
            modifier = modifier,
            status = TangemBadge.Status.Info,
            size = TangemBadge.Size.X6,
            iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_clock_24),
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Preview(showBackground = true, widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayAdditionalCashbackPreview(
    @PreviewParameter(TangemPayAdditionalCashbackPreviewProvider::class) state: TangemPayAdditionalCashbackUM,
) {
    TangemThemePreviewRedesign {
        TangemPayAdditionalCashback(
            state = state,
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}

private class TangemPayAdditionalCashbackPreviewProvider :
    CollectionPreviewParameterProvider<TangemPayAdditionalCashbackUM>(
        listOf(
            TangemPayAdditionalCashbackUM(
                items = persistentListOf(
                    TangemPayAdditionalCashbackUM.Item(
                        id = "1",
                        name = stringReference("Groceries increase"),
                        description = stringReference("+1% cashback for groceries stores"),
                        badge = TangemPayAdditionalCashbackUM.Badge.Permanent,
                    ),
                    TangemPayAdditionalCashbackUM.Item(
                        id = "2",
                        name = stringReference("Groceries increase"),
                        description = stringReference("+1% cashback for groceries stores. Max \$10/month"),
                        badge = TangemPayAdditionalCashbackUM.Badge.Until(stringReference("Until 09.26.2026")),
                    ),
                    TangemPayAdditionalCashbackUM.Item(
                        id = "3",
                        name = stringReference("Cashback increase"),
                        description = stringReference("+2% cashback for groceries stores. Max \$10/month"),
                        badge = TangemPayAdditionalCashbackUM.Badge.Until(stringReference("Until 09.26.2026")),
                    ),
                ),
            ),
        ),
    )