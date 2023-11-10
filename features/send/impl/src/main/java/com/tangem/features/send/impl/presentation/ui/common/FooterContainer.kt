package com.tangem.features.send.impl.presentation.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.res.TangemTheme

/**
 * Container for footer info below the text field
 *
 * @param modifier of component
 * @param footer text
 * @param footerTopPadding padding between footer and field
 * @param content field content
 */
@Composable
internal fun FooterContainer(
    modifier: Modifier = Modifier,
    footer: String? = null,
    footerTopPadding: Dp = TangemTheme.dimens.spacing8,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        content()
        footer?.let {
            Text(
                text = it,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .padding(top = footerTopPadding),
            )
        }
    }
}