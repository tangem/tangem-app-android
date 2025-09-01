package com.tangem.data.feedback.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.navigation.email.EmailSender
import com.tangem.data.feedback.DefaultFeedbackFeatureToggles
import com.tangem.data.feedback.DefaultFeedbackRepository
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.feedback.repository.FeedbackFeatureToggles
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.utils.version.AppVersionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FeedbackModule {

    @Provides
    @Singleton
    fun provideFeedbackRepository(
        appLogsStore: AppLogsStore,
        userWalletsListManager: UserWalletsListManager,
        walletManagersStore: WalletManagersStore,
        emailSender: EmailSender,
        appVersionProvider: AppVersionProvider,
        getSelectedWalletUseCase: GetSelectedWalletUseCase,
    ): FeedbackRepository {
        return DefaultFeedbackRepository(
            appLogsStore = appLogsStore,
            userWalletsListManager = userWalletsListManager,
            walletManagersStore = walletManagersStore,
            emailSender = emailSender,
            appVersionProvider = appVersionProvider,
            getSelectedWalletUseCase = getSelectedWalletUseCase,
        )
    }

    @Provides
    @Singleton
    fun provideFeedbackFeatureToggles(featureTogglesManager: FeatureTogglesManager): FeedbackFeatureToggles {
        return DefaultFeedbackFeatureToggles(featureTogglesManager)
    }
}