package com.tangem.data.news.repository

import com.tangem.datasource.api.news.models.response.NewsArticleDto
import com.tangem.datasource.api.news.models.response.NewsDetailsResponse
import com.tangem.datasource.api.news.models.response.NewsOriginalArticleDto
import com.tangem.datasource.api.news.models.response.NewsRelatedTokenDto
import com.tangem.domain.models.news.*

internal fun NewsDetailsResponse.toDomainDetailedArticle(isLiked: Boolean): DetailedArticle {
    return DetailedArticle(
        id = id,
        createdAt = createdAt,
        score = score.toFloat(),
        locale = language,
        isTrending = isTrending,
        categories = categories.map { ArticleCategory(id = it.id, name = it.name) },
        relatedTokens = relatedTokens.map { it.toDomainRelatedToken() },
        title = title,
        newsUrl = newsUrl,
        shortContent = shortContent,
        content = content,
        originalArticles = originalArticles.map { it.toDomainOriginalArticle() },
        isLiked = isLiked,
    )
}

internal fun NewsArticleDto.toDomainShortArticle(): ShortArticle {
    return ShortArticle(
        id = id,
        createdAt = createdAt,
        score = score.toFloat(),
        locale = language,
        categories = categories.map { ArticleCategory(id = it.id, name = it.name) },
        relatedTokens = relatedTokens.map { it.toDomainRelatedToken() },
        isTrending = isTrending,
        title = title,
        newsUrl = newsUrl,
        viewed = false,
    )
}

internal fun NewsRelatedTokenDto.toDomainRelatedToken(): RelatedToken {
    return RelatedToken(
        id = id,
        symbol = symbol,
        name = name,
    )
}

internal fun NewsOriginalArticleDto.toDomainOriginalArticle(): OriginalArticle {
    return OriginalArticle(
        id = id,
        title = title,
        source = Source(
            id = source.id,
            name = source.name,
        ),
        locale = language,
        publishedAt = publishedAt,
        url = url,
        imageUrl = imageUrl,
    )
}