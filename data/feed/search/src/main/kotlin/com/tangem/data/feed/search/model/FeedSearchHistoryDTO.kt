package com.tangem.data.feed.search.model

import kotlinx.serialization.Serializable

@Serializable
internal data class FeedSearchHistoryDTO(
    val queries: List<QueryDTO> = emptyList(),
)

@Serializable
internal data class QueryDTO(val text: String, val timestamp: Long)