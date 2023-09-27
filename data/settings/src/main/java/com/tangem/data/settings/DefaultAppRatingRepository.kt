package com.tangem.data.settings

import com.tangem.datasource.local.settings.AppLaunchCountStore
import com.tangem.datasource.local.settings.AppRatingShowingCountStore
import com.tangem.datasource.local.settings.FundsFoundDateInMillisStore
import com.tangem.datasource.local.settings.UserInteractingStatusStore
import com.tangem.domain.settings.repositories.AppRatingRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.Calendar

internal class DefaultAppRatingRepository(
    private val fundsFoundDateInMillisStore: FundsFoundDateInMillisStore,
    private val appLaunchCountStore: AppLaunchCountStore,
    private val appRatingShowingCountStore: AppRatingShowingCountStore,
    private val userInteractingStatusStore: UserInteractingStatusStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : AppRatingRepository {

    override suspend fun initialize() {
        fundsFoundDateInMillisStore.getSyncOrNull()
            ?: fundsFoundDateInMillisStore.store(item = FUNDS_FOUND_DATE_UNDEFINED)

        appLaunchCountStore.getSyncOrNull()
            ?: appLaunchCountStore.store(item = DEFAULT_APP_LAUNCH_COUNT)

        appRatingShowingCountStore.getSyncOrNull()
            ?: appRatingShowingCountStore.store(item = FIRST_SHOWING_COUNT)

        userInteractingStatusStore.getSyncOrNull()
            ?: userInteractingStatusStore.store(item = false)
    }

    override suspend fun setWalletWithFundsFound() {
        withContext(dispatchers.io) {
            val foundDate = fundsFoundDateInMillisStore.getSyncOrNull()
            if (foundDate != null && foundDate != FUNDS_FOUND_DATE_UNDEFINED) return@withContext

            fundsFoundDateInMillisStore.store(item = Calendar.getInstance().timeInMillis)

            val appLaunchCount = appLaunchCountStore.getSyncOrNull().let { count ->
                if (count == null) {
                    appLaunchCountStore.store(item = DEFAULT_APP_LAUNCH_COUNT)
                    DEFAULT_APP_LAUNCH_COUNT
                } else {
                    count
                }
            }

            appRatingShowingCountStore.store(item = appLaunchCount + FIRST_SHOWING_COUNT)

            userInteractingStatusStore.getSyncOrNull()
                ?: userInteractingStatusStore.store(item = false)
        }
    }

    override fun isReadyToShow(): Flow<Boolean> {
        // TODO: [REDACTED_JIRA]
        return combine(
            userInteractingStatusStore.get(),
            appRatingShowingCountStore.get(),
            appLaunchCountStore.get(),
            fundsFoundDateInMillisStore.get(),
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
        appLaunchCountStore.getSyncOrNull()?.let {
            updateNextShowing(at = it + DEFER_SHOWING_COUNT)
        }
    }

    override suspend fun setNeverToShow() {
        updateNextShowing(at = Int.MAX_VALUE)
    }

    private suspend fun updateNextShowing(at: Int) {
        withContext(dispatchers.io) {
            appRatingShowingCountStore.store(item = at)
            userInteractingStatusStore.store(item = true)
        }
    }

    private companion object {
        const val FUNDS_FOUND_DATE_UNDEFINED = -1L
        const val DEFAULT_APP_LAUNCH_COUNT = 1
        const val FIRST_SHOWING_COUNT = 3
        const val DEFER_SHOWING_COUNT = 20
        const val DAY_IN_MILLIS = 1000 * 60 * 60 * 24
    }
}