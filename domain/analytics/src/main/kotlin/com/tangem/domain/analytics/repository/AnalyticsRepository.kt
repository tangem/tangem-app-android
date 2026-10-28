package com.tangem.domain.analytics.repository

interface AnalyticsRepository {

    suspend fun checkIsEventSent(eventId: String): Boolean

    suspend fun setIsEventSent(eventId: String)
}