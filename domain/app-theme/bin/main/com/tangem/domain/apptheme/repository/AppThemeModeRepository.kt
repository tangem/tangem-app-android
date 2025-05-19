package com.tangem.domain.apptheme.repository

import com.tangem.domain.apptheme.model.AppThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Represents a repository for managing the application's theme mode settings.
 */
interface AppThemeModeRepository {

    /**
     * Retrieves the current application theme mode as a flow.
     *
     * @return A [Flow] emitting the current [AppThemeMode].
     */
    fun getAppThemeMode(): Flow<AppThemeMode>

    /**
     * Changes the application's theme mode to the specified [mode].
     *
     * @param mode The new [AppThemeMode] to be set.
     */
    suspend fun changeAppThemeMode(mode: AppThemeMode)
}