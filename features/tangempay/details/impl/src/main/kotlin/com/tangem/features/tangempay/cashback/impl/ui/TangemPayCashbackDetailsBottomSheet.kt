package com.tangem.features.tangempay.cashback.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackDetailsUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayCashbackDetailsBottomSheet(state: TangemPayCashbackDetailsUM, onDismiss: () -> Unit) {
    CashbackBottomSheet(title = state.title, onDismiss = onDismiss) {
        DetailsContent(state)
    }
}

@Composable
private fun DetailsContent(state: TangemPayCashbackDetailsUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        state.rows.forEachIndexed { index, row ->
            TangemRow(
                verticalAlignment = TangemRowVerticalAlignment.Top,
                startSlot = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.ic_information_24),
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.primary,
                    )
                },
                titleSlot = { TangemRowText(text = row, role = TangemRowTextRole.Title, maxLines = Int.MAX_VALUE) },
                divider = index < state.rows.lastIndex,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Preview(showBackground = true, widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCashbackDetailsBottomSheetPreview(
    @PreviewParameter(TangemPayCashbackDetailsPreviewProvider::class) state: TangemPayCashbackDetailsUM,
) {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.secondary),
        ) {
            DetailsContent(state = state)
        }
    }
}

private class TangemPayCashbackDetailsPreviewProvider : CollectionPreviewParameterProvider<TangemPayCashbackDetailsUM>(
    listOf(
        TangemPayCashbackDetailsUM(
            title = stringReference("Cashback up to 2%"),
            rows = persistentListOf<TextReference>(
                stringReference("1% for All purchases with your Basic cards, min purchase $30, up to $100 per month"),
                stringReference("2% for All purchases with your Plus cards, min purchase $30, up to $300 per month"),
            ),
        ),
    ),
)