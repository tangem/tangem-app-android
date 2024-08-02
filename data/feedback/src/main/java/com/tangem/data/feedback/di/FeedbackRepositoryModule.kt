package com.tangem.data.feedback.di

import android.content.Context
import com.tangem.data.feedback.DefaultFeedbackRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FeedbackRepositoryModule {

    @Provides
    @Singleton
    fun provideFeedbackRepository(
        appPreferencesStore: AppPreferencesStore,
        userWalletsListManager: UserWalletsListManager,
        walletManagersStore: WalletManagersStore,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): FeedbackRepository {
        return DefaultFeedbackRepository(
            appPreferencesStore = appPreferencesStore,
            userWalletsListManager = userWalletsListManager,
            walletManagersStore = walletManagersStore,
            context = context,
            dispatchers = dispatchers,
        )
    }
}
