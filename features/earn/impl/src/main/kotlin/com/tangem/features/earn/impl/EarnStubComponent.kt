package com.tangem.features.earn.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme

/** Placeholder of an Earn screen that is not built yet: Info (A-2.6) and the flow (A-3.2) replace it. */
internal class EarnStubComponent(private val title: String) : ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(TangemTheme.colors3.bg.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
            )
        }
    }
}