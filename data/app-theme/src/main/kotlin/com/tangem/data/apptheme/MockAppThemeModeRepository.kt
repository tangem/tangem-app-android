package com.tangem.data.apptheme

import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class MockAppThemeModeRepository : AppThemeModeRepository {

    private val appThemeModeFlow = MutableStateFlow(AppThemeMode.DEFAULT)

    override fun getAppThemeMode(): Flow<AppThemeMode> {
        return appThemeModeFlow
    }

    override suspend fun changeAppThemeMode(mode: AppThemeMode) {
        appThemeModeFlow.value = mode
    }
}