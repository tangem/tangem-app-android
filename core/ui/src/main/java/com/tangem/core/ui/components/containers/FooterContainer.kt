package com.tangem.core.ui.components.containers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Container for footer info below the text field
 *
 * @param modifier of component
 * @param footer text
 * @param paddingValues padding between footer and field
 * @param content field content
 */
@Composable
fun FooterContainer(
    modifier: Modifier = Modifier,
    footer: TextReference? = null,
    paddingValues: PaddingValues = PaddingValues(top = 8.dp),
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        content()
        AnimatedVisibility(visible = footer != null) {
            val footerWrapped = remember(this) { requireNotNull(footer) }
            Text(
                text = footerWrapped.resolveAnnotatedReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .padding(paddingValues),
            )
        }
    }
}