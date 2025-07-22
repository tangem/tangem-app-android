package com.tangem.features.send.v2.subcomponents.destination.model.transformers

import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationRecipientListUM
import kotlinx.collections.immutable.toPersistentList

internal const val WALLET_DEFAULT_COUNT = 1
internal const val RECENT_DEFAULT_COUNT = 3
internal const val WALLET_KEY_TAG = "wallet"
internal const val RECENT_KEY_TAG = "recent"

internal fun loadingListState(tag: String, count: Int) = buildList {
    repeat(count) {
        add(
            DestinationRecipientListUM(
                id = "$tag$it",
                isLoading = true,
            ),
        )
    }
}.toPersistentList()

internal fun emptyListState(tag: String, count: Int) = buildList {
    repeat(count) {
        add(
            DestinationRecipientListUM(
                id = "$tag$it",
                isLoading = false,
                isVisible = false,
            ),
        )
    }
}.toPersistentList()