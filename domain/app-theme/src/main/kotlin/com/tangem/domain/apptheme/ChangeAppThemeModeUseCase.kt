package com.tangem.domain.apptheme

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.apptheme.error.AppThemeModeError
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.apptheme.repository.AppThemeModeRepository

/**
 * Use case responsible for changing the application theme mode.
 *
 * @property appThemeModeRepository The repository providing access to theme mode settings.
 */
class ChangeAppThemeModeUseCase(
    private val appThemeModeRepository: AppThemeModeRepository,
) {

    /**
     * Changes the application theme mode.
     *
     * @param mode The new [AppThemeMode] to set.
     * @return An [Either] instance. The right side contains a [Unit] value indicating success,
     * and the left side contains any [AppThemeModeError] that occurred during the process.
     */
    suspend operator fun invoke(mode: AppThemeMode): Either<AppThemeModeError, Unit> = either {
        catch({ appThemeModeRepository.changeAppThemeMode(mode) }) {
            raise(AppThemeModeError.DataError(it))
        }
    }
}