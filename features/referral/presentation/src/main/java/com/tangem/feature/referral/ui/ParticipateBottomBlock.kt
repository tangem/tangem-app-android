package com.tangem.feature.referral.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryEndIconButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.referral.presentation.R

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_ParticipateBottomBlock_InLightTheme() {
    TangemTheme(isDark = false) {
        Column(Modifier.background(MaterialTheme.colors.primary)) {
            ParticipateBottomBlock(onAgreementClicked = {}, onParticipateClicked = {})
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_ParticipateBottomBlock_InDarkTheme() {
    TangemTheme(isDark = true) {
        Column(Modifier.background(MaterialTheme.colors.primary)) {
            ParticipateBottomBlock(onAgreementClicked = {}, onParticipateClicked = {})
        }
    }
}

@Composable
fun ParticipateBottomBlock(onAgreementClicked: () -> Unit, onParticipateClicked: () -> Unit) {
    Column {
        AgreementText(
            firstPartResId = R.string.referral_tou_not_enroled_prefix,
            onClicked = onAgreementClicked,
        )
        PrimaryEndIconButton(
            modifier = Modifier.padding(all = dimensionResource(id = R.dimen.spacing16)),
            text = stringResource(id = R.string.referral_button_participate),
            iconResId = R.drawable.ic_tangem,
            onClicked = onParticipateClicked,
        )
    }
}