package com.tangem.data.feedback.di

import android.content.Context
import com.tangem.data.feedback.DefaultFeedbackRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.feedback.repository.FeedbackRepository
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
        userWalletsStore: UserWalletsStore,
        walletManagersStore: WalletManagersStore,
        @ApplicationContext context: Context,
    ): FeedbackRepository {
        return DefaultFeedbackRepository(appPreferencesStore, userWalletsStore, walletManagersStore, context)
    }
}
