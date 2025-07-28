package com.tangem.core.ui.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Stable
fun interface ComposableContentComponent {

    @Composable
    fun Content(modifier: Modifier)
}

fun getEmptyComposableContentComponent() = EmptyComposableContentComponent

object EmptyComposableContentComponent : ComposableContentComponent {
    @Composable
    override fun Content(modifier: Modifier) {
        /* no-op */
    }
}