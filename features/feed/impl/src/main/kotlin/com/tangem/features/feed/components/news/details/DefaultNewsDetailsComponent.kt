package com.tangem.features.feed.components.news.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableModularContentComponent

internal class DefaultNewsDetailsComponent(
    appComponentContext: AppComponentContext,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Title() {
    }

    @Composable
    override fun Content(modifier: Modifier) {
    }

    @Composable
    override fun Footer() {
    }
}