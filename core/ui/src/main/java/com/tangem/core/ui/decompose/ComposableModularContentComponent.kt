package com.tangem.core.ui.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Stable
interface ComposableModularContentComponent {

    @Composable
    fun Title()

    @Composable
    fun Content(modifier: Modifier)

    @Composable
    fun Footer()
}

fun getEmptyComposableModularContentComponent() = EmptyComposableModularContentComponent

object EmptyComposableModularContentComponent : ComposableModularContentComponent {
    @Composable
    override fun Title() {
        /* no-op */
    }

    @Composable
    override fun Content(modifier: Modifier) {
        /* no-op */
    }

    @Composable
    override fun Footer() {
        /* no-op */
    }
}