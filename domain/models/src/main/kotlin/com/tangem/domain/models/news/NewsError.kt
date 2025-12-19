package com.tangem.domain.models.news

import kotlinx.serialization.Serializable

@Serializable
sealed class NewsError {
    abstract val message: String?
    abstract val code: Int?

    data class ArticleNotFound(
        override val message: String?,
        override val code: Int?,
    ) : NewsError()

    data class Unknown(
        override val message: String?,
        override val code: Int?,
    ) : NewsError()
}