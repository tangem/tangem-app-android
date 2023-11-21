package com.tangem.domain.settings.repositories

import kotlinx.coroutines.flow.Flow

interface AppRatingRepository {

    suspend fun setWalletWithFundsFound()

    fun isReadyToShow(): Flow<Boolean>

    suspend fun remindLater()

    suspend fun setNeverToShow()
}