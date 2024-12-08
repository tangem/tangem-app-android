package com.tangem.feature.referral.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.referral.presentation.R

@Composable
internal fun NonParticipateBottomBlock(onAgreementClick: () -> Unit, onParticipateClick: () -> Unit) {
    Column {
        AgreementText(
            firstPartResId = R.string.referral_tos_not_enroled_prefix,
            onClick = onAgreementClick,
        )

        PrimaryButtonIconEnd(
            text = stringResourceSafe(id = R.string.referral_button_participate),
            iconResId = R.drawable.ic_tangem_24,
            onClick = onParticipateClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = TangemTheme.dimens.spacing16),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NonParticipateBottomBlock() {
    TangemThemePreview {
        Column(Modifier.background(TangemTheme.colors.background.primary)) {
            NonParticipateBottomBlock(onAgreementClick = {}, onParticipateClick = {})
        }
    }
}