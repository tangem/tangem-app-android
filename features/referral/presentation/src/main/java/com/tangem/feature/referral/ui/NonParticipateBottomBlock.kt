package com.tangem.feature.referral.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryStartIconButton
import com.tangem.core.ui.components.SmallInfoCard
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TextColorType
import com.tangem.core.ui.res.textColor
import com.tangem.feature.referral.presentation.R

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_NonParticipateBottomBlock_InLightTheme() {
    TangemTheme(isDark = false) {
        Column(Modifier.background(MaterialTheme.colors.primary)) {
            NonParticipateBottomBlock(
                purchasedWalletCount = 3,
                code = "x4JdK",
                onCopyClicked = {},
                onShareClicked = {},
                onAgreementClicked = {},
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_NonParticipateBottomBlock_InDarkTheme() {
    TangemTheme(isDark = true) {
        Column(Modifier.background(MaterialTheme.colors.primary)) {
            NonParticipateBottomBlock(
                purchasedWalletCount = 3,
                code = "x4JdK",
                onCopyClicked = {},
                onShareClicked = {},
                onAgreementClicked = {},
            )
        }
    }
}

@Composable
fun NonParticipateBottomBlock(
    purchasedWalletCount: Int,
    code: String,
    onCopyClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onAgreementClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(
                top = dimensionResource(id = R.dimen.spacing24),
                bottom = dimensionResource(id = R.dimen.spacing16),
            )
            .padding(horizontal = dimensionResource(id = R.dimen.spacing16)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing16)),
    ) {
        SmallInfoCard(
            startText = stringResource(id = R.string.referral_friends_bought_title),
            endText = String.format(
                stringResource(id = R.string.referral_wallets_purchased_count),
                purchasedWalletCount,
            ),
        )
        PersonalCodeCard(code = code)
        AdditionalButtons(onCopyClicked = onCopyClicked, onShareClicked = onShareClicked)
        AgreementText(firstPartResId = R.string.referral_tos_enroled_prefix, onClicked = onAgreementClicked)
    }
}

@Composable
private fun PersonalCodeCard(code: String) {
    Column(
        modifier = Modifier
            .shadow(
                elevation = dimensionResource(id = R.dimen.elevation2),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.radius12)),
            )
            .background(
                color = MaterialTheme.colors.secondary,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.radius12)),
            )
            .fillMaxWidth()
            .padding(vertical = dimensionResource(id = R.dimen.spacing12)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing4)),
    ) {
        Text(
            text = stringResource(id = R.string.referral_promo_code_title),
            color = MaterialTheme.colors.textColor(type = TextColorType.TERTIARY),
            maxLines = 1,
            style = MaterialTheme.typography.subtitle2,
        )
        Text(
            text = code,
            color = MaterialTheme.colors.textColor(type = TextColorType.PRIMARY1),
            maxLines = 1,
            style = MaterialTheme.typography.h2,
        )
    }
}

@Composable
private fun AdditionalButtons(onCopyClicked: () -> Unit, onShareClicked: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing16)),
    ) {
        PrimaryStartIconButton(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.common_copy),
            iconResId = R.drawable.ic_copy,
            onClicked = onCopyClicked,
        )
        PrimaryStartIconButton(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.common_share),
            iconResId = R.drawable.ic_share,
            onClicked = onShareClicked,
        )
    }
}