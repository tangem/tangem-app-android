package com.tangem.datasource.local.cache.model

import org.joda.time.Duration
import org.joda.time.LocalDateTime

data class CacheKey(
    val id: String,
    val updatedAt: LocalDateTime,
    val expiresIn: Duration,
)