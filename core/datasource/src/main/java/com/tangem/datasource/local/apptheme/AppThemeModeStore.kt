package com.tangem.datasource.local.apptheme

import com.tangem.domain.apptheme.model.AppThemeMode
import kotlinx.coroutines.flow.Flow

interface AppThemeModeStore {

    fun get(): Flow<AppThemeMode>

    suspend fun store(item: AppThemeMode)

    suspend fun isEmpty(): Boolean
}