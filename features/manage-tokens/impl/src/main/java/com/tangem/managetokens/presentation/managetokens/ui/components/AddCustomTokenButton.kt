package com.tangem.managetokens.presentation.managetokens.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.managetokens.impl.R

@Composable
internal fun AddCustomTokenButton(onButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = TangemTheme.dimens.size68)
            .fillMaxWidth()
            .clickable { onButtonClick() }
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        Box(
            modifier = Modifier
                .size(TangemTheme.dimens.size36)
                .background(color = TangemTheme.colors.button.secondary, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_plus_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
        SpacerW(width = TangemTheme.dimens.spacing12)
        Text(
            text = stringResource(id = R.string.add_custom_token_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddCustomTokenButton_Preview() {
    TangemThemePreview {
        AddCustomTokenButton(onButtonClick = { })
    }
}
