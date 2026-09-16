package com.tangem.features.feed.components.v2

import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultFeedEntryController @Inject constructor() : FeedEntryController {

    override val pendingEntry: StateFlow<FeedEntryRequest?>
        field = MutableStateFlow<FeedEntryRequest?>(null)

    override fun request(request: FeedEntryRequest) {
        pendingEntry.value = request
    }

    override fun consumeEntry() {
        pendingEntry.value = null
    }
}