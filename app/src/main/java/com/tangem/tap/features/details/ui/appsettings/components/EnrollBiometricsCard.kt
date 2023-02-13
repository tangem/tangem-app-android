package com.tangem.tap.features.details.ui.appsettings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.SpacerW16
import com.tangem.core.ui.res.TangemTheme
import com.tangem.wallet.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun EnrollBiometricsCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing8)
            .fillMaxWidth(),
        color = TangemTheme.colors.background.primary,
        shape = TangemTheme.shapes.roundedCornersLarge,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_alert_circle_24),
                tint = TangemTheme.colors.icon.attention,
                contentDescription = null,
            )
            SpacerW16()
            Column {
                Text(
                    text = stringResource(id = R.string.app_settings_enable_biometrics_title),
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerH4()
                Text(
                    text = stringResource(id = R.string.app_settings_enable_biometrics_description),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
    }
}

// region Preview
@Composable
private fun EnrollBiometricsCardSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(TangemTheme.colors.background.secondary),
    ) {
        EnrollBiometricsCard(onClick = {})
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EnrollBiometricsCardPreview_Light() {
    TangemTheme {
        EnrollBiometricsCardSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EnrollBiometricsCardPreview_Dark() {
    TangemTheme(isDark = true) {
        EnrollBiometricsCardSample()
    }
}
// endregion Preview
