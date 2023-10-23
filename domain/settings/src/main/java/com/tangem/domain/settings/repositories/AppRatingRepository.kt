package com.tangem.domain.settings.repositories

import kotlinx.coroutines.flow.Flow

interface AppRatingRepository {

    // FIXME: We must to initialize all stores before calling [isReadyToShow], otherwise flow will not emit data
    suspend fun initialize()

    suspend fun setWalletWithFundsFound()

    fun isReadyToShow(): Flow<Boolean>

    suspend fun remindLater()

    suspend fun setNeverToShow()
}