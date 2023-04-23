package com.tangem.tap.features.customtoken.impl.presentation.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.details.ui.cardsettings.resolveReference

/**
 * Add custom token toolbar
 *
 * @param title             title
 * @param onBackButtonClick lambda be invoked when BackButton is been pressed
 *
 * @author Andrew Khokhlov on 19/04/2023
 */
@Composable
internal fun AddCustomTokenToolbar(title: TextReference, onBackButtonClick: () -> Unit) {
    TopAppBar(backgroundColor = TangemTheme.colors.background.secondary) {
        IconButton(onClick = onBackButtonClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.secondary,
            )
        }

        Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing26))

        Text(
            text = title.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            style = TangemTheme.typography.h3,
        )
    }
}

@Preview
@Composable
internal fun Preview_AddCustomTokenToolbar() {
    TangemTheme {
        AddCustomTokenToolbar(title = TextReference.Res(R.string.add_custom_token_title), onBackButtonClick = {})
    }
}
