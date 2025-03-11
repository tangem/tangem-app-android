package com.tangem.data.feedback.di

import com.tangem.core.navigation.email.EmailSender
import com.tangem.data.feedback.DefaultFeedbackRepository
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.utils.version.AppVersionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
        emailSender: EmailSender,
        appVersionProvider: AppVersionProvider,
    ): FeedbackRepository {
        return DefaultFeedbackRepository(
            appLogsStore = appLogsStore,
            userWalletsListManager = userWalletsListManager,
            walletManagersStore = walletManagersStore,
            emailSender = emailSender,
            appVersionProvider = appVersionProvider,
        )
    }
}
