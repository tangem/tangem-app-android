package com.tangem.features.feed.ui.v2.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/** One entry of the feed home's tab row. */
@Immutable
internal data class FeedV2TabUM(
    val id: String,
    val title: TextReference,
)