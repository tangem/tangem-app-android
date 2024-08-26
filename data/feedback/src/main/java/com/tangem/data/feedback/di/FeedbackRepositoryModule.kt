package com.tangem.data.feedback.di

import android.content.Context
import com.tangem.data.feedback.DefaultFeedbackRepository
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
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
        appLogsStore: AppLogsStore,
        userWalletsListManager: UserWalletsListManager,
        walletManagersStore: WalletManagersStore,
        @ApplicationContext context: Context,
    ): FeedbackRepository {
        return DefaultFeedbackRepository(
            appLogsStore = appLogsStore,
            userWalletsListManager = userWalletsListManager,
            walletManagersStore = walletManagersStore,
            context = context,
        )
    }
}