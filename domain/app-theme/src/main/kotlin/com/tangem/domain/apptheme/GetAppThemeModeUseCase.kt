package com.tangem.domain.apptheme

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.apptheme.error.AppThemeModeError
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Use case responsible for retrieving the current application theme mode.
 *
 * @property appThemeModeRepository The repository providing access to theme mode settings.
 */
class GetAppThemeModeUseCase(
    private val appThemeModeRepository: AppThemeModeRepository,
) {

    /**
     * Invokes the use case to retrieve the current application theme mode.
     *
     * @return A [Flow] emitting an [Either] instance. The right side contains the retrieved
     * [AppThemeMode], and the left side contains any [AppThemeModeError] that occurred during the process.
     */
    operator fun invoke(): Flow<Either<AppThemeModeError, AppThemeMode>> {
        return appThemeModeRepository.getAppThemeMode()
            .map<AppThemeMode, Either<AppThemeModeError, AppThemeMode>> { it.right() }
            .catch { emit(AppThemeModeError.DataError(it).left()) }
    }
}