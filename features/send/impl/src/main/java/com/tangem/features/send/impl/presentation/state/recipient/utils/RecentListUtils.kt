package com.tangem.features.send.impl.presentation.state.recipient.utils

import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import kotlinx.collections.immutable.toPersistentList

internal const val WALLET_DEFAULT_COUNT = 1
internal const val RECENT_DEFAULT_COUNT = 3
internal const val WALLET_KEY_TAG = "wallet"
internal const val RECENT_KEY_TAG = "recent"

internal fun loadingListState(tag: String, count: Int) = buildList {
    repeat(count) {
        add(
            SendRecipientListContent(
                id = "$tag$it",
                isLoading = true,
            ),
        )
    }
}.toPersistentList()

internal fun emptyListState(tag: String, count: Int) = buildList {
    repeat(count) {
        add(
            SendRecipientListContent(
                id = "$tag$it",
                isLoading = false,
                isVisible = false,
            ),
        )
    }
}.toPersistentList()