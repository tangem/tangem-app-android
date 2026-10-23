package com.tangem.features.feed.entry.components

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

@Stable
interface FeedEntryComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<FeedEntryRoute, FeedEntryComponent>
}