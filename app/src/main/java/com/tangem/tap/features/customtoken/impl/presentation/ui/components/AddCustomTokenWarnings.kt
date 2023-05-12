package com.tangem.tap.features.customtoken.impl.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenWarning
import com.tangem.tap.features.customtoken.impl.presentation.ui.AddCustomTokenPreviewData
import com.tangem.tap.features.details.ui.cardsettings.resolveReference
import com.tangem.wallet.R

/**
 * Add custom token warnings
 *
 * @param warnings warnings descriptions set
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun AddCustomTokenWarnings(warnings: Set<AddCustomTokenWarning>) {
    Column(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        warnings.forEach { warning ->
            key(warning) { AddCustomTokenWarning(warning) }
        }
    }
}

@Composable
private fun AddCustomTokenWarning(warning: AddCustomTokenWarning) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = TangemTheme.shapes.roundedCornersSmall2,
        backgroundColor = TangemColorPalette.Tangerine,
        contentColor = TangemColorPalette.White,
        elevation = TangemTheme.dimens.elevation4,
    ) {
        // FIXME("Incorrect typography. Replace with typography from design system")
        Column(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing16),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        ) {
            Text(
                text = stringResource(id = R.string.common_warning),
                maxLines = 1,
                style = TangemTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = warning.description.resolveReference(),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Preview
@Composable
private fun Preview_AddCustomTokenWarnings() {
    TangemTheme {
        AddCustomTokenWarnings(warnings = AddCustomTokenPreviewData.createWarnings())
    }
}