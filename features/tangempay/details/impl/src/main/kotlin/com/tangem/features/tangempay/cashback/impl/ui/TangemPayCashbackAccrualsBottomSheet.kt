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
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackAccrualsUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayCashbackAccrualsBottomSheet(state: TangemPayCashbackAccrualsUM, onDismiss: () -> Unit) {
    CashbackBottomSheet(title = state.title, onDismiss = onDismiss) {
        AccrualsContent(state)
    }
}

@Composable
private fun AccrualsContent(state: TangemPayCashbackAccrualsUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        state.infoRows.forEach { row ->
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
                titleSlot = { TangemRowText(text = row.title, role = TangemRowTextRole.Title) },
                subtitleSlot = {
                    TangemRowText(text = row.description, role = TangemRowTextRole.Subtitle, maxLines = Int.MAX_VALUE)
                },
                divider = true,
            )
        }
        state.docRows.forEachIndexed { index, row ->
            TangemRow(
                verticalAlignment = TangemRowVerticalAlignment.Center,
                startSlot = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.ic_doc_24),
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.primary,
                    )
                },
                titleSlot = { TangemRowText(text = row.title, role = TangemRowTextRole.Title) },
                endSlot = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.ic_chevron_right_24),
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.secondary,
                    )
                },
                onClick = row.onClick,
                divider = index < state.docRows.lastIndex,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Preview(showBackground = true, widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCashbackAccrualsBottomSheetPreview(
    @PreviewParameter(TangemPayCashbackAccrualsPreviewProvider::class) state: TangemPayCashbackAccrualsUM,
) {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.secondary),
        ) {
            AccrualsContent(state = state)
        }
    }
}

private class TangemPayCashbackAccrualsPreviewProvider :
    CollectionPreviewParameterProvider<TangemPayCashbackAccrualsUM>(
        listOf(
            TangemPayCashbackAccrualsUM(
                title = stringReference("Accruals"),
                infoRows = previewInfoRows(),
                docRows = persistentListOf(
                    TangemPayCashbackAccrualsUM.DocRow(
                        title = stringReference("All categories without cashback"),
                        onClick = {},
                    ),
                    TangemPayCashbackAccrualsUM.DocRow(
                        title = stringReference("Full terms of cashback program"),
                        onClick = {},
                    ),
                ),
            ),
            TangemPayCashbackAccrualsUM(
                title = stringReference("Accruals"),
                infoRows = previewInfoRows(),
                docRows = persistentListOf(),
            ),
        ),
    )

private fun previewInfoRows() = persistentListOf(
    TangemPayCashbackAccrualsUM.InfoRow(
        title = stringReference("How we calculate cashback?"),
        description = stringReference(
            "We process purchases within 5 days after the operation and count only completed transactions",
        ),
    ),
    TangemPayCashbackAccrualsUM.InfoRow(
        title = stringReference("How we pay cashback?"),
        description = stringReference("From the 2nd and the 5th of the next month"),
    ),
    TangemPayCashbackAccrualsUM.InfoRow(
        title = stringReference("Exceptions"),
        description = stringReference(
            "No cashback will be awarded for in-person/in-store purchases at EU merchants; also for " +
                "withdrawals, transfers, quasi-cash, mobile phone bills, government services and certain " +
                "other categories",
        ),
    ),
)