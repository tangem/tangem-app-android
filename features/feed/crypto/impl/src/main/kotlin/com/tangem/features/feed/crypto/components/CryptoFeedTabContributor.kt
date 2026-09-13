package com.tangem.features.feed.crypto.components

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.feed.crypto.CryptoFeedSearchTabComponent
import com.tangem.features.feed.crypto.CryptoFeedTabComponent
import com.tangem.features.feed.crypto.impl.R
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

internal class CryptoFeedTabContributor @Inject constructor(
    private val componentFactory: CryptoFeedTabComponent.Factory,
    private val searchComponentFactory: CryptoFeedSearchTabComponent.Factory,
) : FeedTabContributor {

    override val id: FeedTabId = FeedTabId.Crypto

    override val title: TextReference = resourceReference(id = R.string.feed_tab_crypto)

    override val isAvailable: Boolean = true

    override fun createTab(context: AppComponentContext): FeedTabComponent {
        return componentFactory.create(context = context, params = Unit)
    }

    override fun createSearchTab(context: AppComponentContext, query: StateFlow<String>): FeedTabComponent {
        return searchComponentFactory.create(
            context = context,
            params = CryptoFeedSearchTabComponent.Params(query = query),
        )
    }
}