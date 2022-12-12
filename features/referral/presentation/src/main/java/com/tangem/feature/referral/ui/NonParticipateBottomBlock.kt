package com.tangem.feature.referral.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryEndIconButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.referral.presentation.R

@Composable
internal fun NonParticipateBottomBlock(onAgreementClicked: () -> Unit, onParticipateClicked: () -> Unit) {
    Column {
        AgreementText(
            firstPartResId = R.string.referral_tos_not_enroled_prefix,
            onClicked = onAgreementClicked,
        )
        PrimaryEndIconButton(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing16),
            text = stringResource(id = R.string.referral_button_participate),
            iconResId = R.drawable.ic_tangem_24,
            onClicked = onParticipateClicked,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_NonParticipateBottomBlock_InLightTheme() {
    TangemTheme(isDark = false) {
        Column(Modifier.background(MaterialTheme.colors.primary)) {
            NonParticipateBottomBlock(onAgreementClicked = {}, onParticipateClicked = {})
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_NonParticipateBottomBlock_InDarkTheme() {
    TangemTheme(isDark = true) {
        Column(Modifier.background(MaterialTheme.colors.primary)) {
            NonParticipateBottomBlock(onAgreementClicked = {}, onParticipateClicked = {})
        }
    }
}
