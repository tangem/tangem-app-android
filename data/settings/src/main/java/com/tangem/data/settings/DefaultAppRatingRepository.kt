package com.tangem.data.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.domain.settings.repositories.AppRatingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

internal class DefaultAppRatingRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : AppRatingRepository {

    override suspend fun setWalletWithFundsFound() {
        appPreferencesStore.edit { mutablePreferences ->
            val foundDate = mutablePreferences[PreferencesKeys.FUNDS_FOUND_DATE_KEY]
            if (foundDate != null && foundDate != FUNDS_FOUND_DATE_UNDEFINED) return@edit

            mutablePreferences[PreferencesKeys.FUNDS_FOUND_DATE_KEY] = Calendar.getInstance().timeInMillis

            val appLaunchCount = mutablePreferences[PreferencesKeys.APP_LAUNCH_COUNT_KEY] ?: DEFAULT_APP_LAUNCH_COUNT
            mutablePreferences[PreferencesKeys.SHOW_RATING_DIALOG_AT_LAUNCH_COUNT_KEY] =
                appLaunchCount + FIRST_SHOWING_COUNT
        }
    }

    override fun isReadyToShow(): Flow<Boolean> {
        // TODO: [REDACTED_JIRA]
        return combine(
            appPreferencesStore.get(key = PreferencesKeys.USER_WAS_INTERACT_WITH_RATING_KEY, default = false),
            appPreferencesStore.get(
                key = PreferencesKeys.SHOW_RATING_DIALOG_AT_LAUNCH_COUNT_KEY,
                default = FIRST_SHOWING_COUNT,
            ),
            appPreferencesStore.get(key = PreferencesKeys.APP_LAUNCH_COUNT_KEY, default = DEFAULT_APP_LAUNCH_COUNT),
            appPreferencesStore.get(key = PreferencesKeys.FUNDS_FOUND_DATE_KEY, default = FUNDS_FOUND_DATE_UNDEFINED),
        ) { isInteracting, ratingShowingCount, appLaunchCount, fundsFoundDate ->
            if (!isInteracting) {
                val diff = Calendar.getInstance().timeInMillis - fundsFoundDate
                val diffInDays = diff / DAY_IN_MILLIS
                val isFundsFound = fundsFoundDate != FUNDS_FOUND_DATE_UNDEFINED

                appLaunchCount >= ratingShowingCount && diffInDays >= FIRST_SHOWING_COUNT && isFundsFound
            } else {
                appLaunchCount >= ratingShowingCount
            }
        }
    }

    override suspend fun remindLater() {
        appPreferencesStore.edit { mutablePreferences ->
            mutablePreferences[PreferencesKeys.APP_LAUNCH_COUNT_KEY]?.let {
                mutablePreferences.updateNextShowing(it + DEFER_SHOWING_COUNT)
            }
        }
    }

    override suspend fun setNeverToShow() {
        appPreferencesStore.edit { mutablePreferences ->
            mutablePreferences.updateNextShowing(at = Int.MAX_VALUE)
        }
    }

    private fun MutablePreferences.updateNextShowing(at: Int) {
        this[PreferencesKeys.SHOW_RATING_DIALOG_AT_LAUNCH_COUNT_KEY] = at
        this[PreferencesKeys.USER_WAS_INTERACT_WITH_RATING_KEY] = true
    }

    private companion object {
        const val FUNDS_FOUND_DATE_UNDEFINED = -1L
        const val DEFAULT_APP_LAUNCH_COUNT = 1
        const val FIRST_SHOWING_COUNT = 3
        const val DEFER_SHOWING_COUNT = 20
        const val DAY_IN_MILLIS = 1000 * 60 * 60 * 24
    }
}