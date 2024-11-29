package com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R

@Composable
internal fun MultiWalletAccessCodeIntro(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_access_code_intro_title),
            style = TangemTheme.typography.h2,
        )
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_access_code),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 46.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            DescriptionItem(
                title = stringResource(R.string.onboarding_access_code_feature_1_title),
                body = stringResource(R.string.onboarding_access_code_feature_1_description),
                iconRes = R.drawable.ic_feature_1,
            )
            DescriptionItem(
                title = stringResource(R.string.onboarding_access_code_feature_2_title),
                body = stringResource(R.string.onboarding_access_code_feature_2_description),
                iconRes = R.drawable.ic_feature_2,
            )
            DescriptionItem(
                title = stringResource(R.string.onboarding_access_code_feature_3_title),
                body = stringResource(R.string.onboarding_access_code_feature_3_description),
                iconRes = R.drawable.ic_feature_3,
            )
        }
    }
}

@Composable
private fun DescriptionItem(title: String, body: String, @DrawableRes iconRes: Int, modifier: Modifier = Modifier) {
    Row(modifier) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            tint = TangemTheme.colors.icon.primary1,
            contentDescription = null,
        )
        SpacerW(20.dp)
        Column {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle1,
            )
            SpacerH(3.dp)
            Text(
                text = body,
                style = TangemTheme.typography.body2,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        MultiWalletAccessCodeIntro()
    }
}