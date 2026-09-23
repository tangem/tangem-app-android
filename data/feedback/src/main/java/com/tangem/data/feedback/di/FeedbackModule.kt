package com.tangem.data.feedback.di

import com.tangem.core.navigation.email.EmailSender
import com.tangem.data.feedback.DefaultFeedbackRepository
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.domain.sociallogin.usecase.IsSocialBackupCompletedUseCase
import com.tangem.utils.info.AppInfoProvider
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
        userWalletsListRepository: UserWalletsListRepository,
        walletManagersFacade: WalletManagersFacade,
        emailSender: EmailSender,
        appInfoProvider: AppInfoProvider,
        cloudBackupRepository: CloudBackupRepository,
        isSocialBackupCompletedUseCase: IsSocialBackupCompletedUseCase,
        socialLoginFeatureToggles: SocialLoginFeatureToggles,
    ): FeedbackRepository {
        return DefaultFeedbackRepository(
            appLogsStore = appLogsStore,
            walletManagersFacade = walletManagersFacade,
            emailSender = emailSender,
            appInfoProvider = appInfoProvider,
            userWalletsListRepository = userWalletsListRepository,
            cloudBackupRepository = cloudBackupRepository,
            isSocialBackupCompletedUseCase = isSocialBackupCompletedUseCase,
            socialLoginFeatureToggles = socialLoginFeatureToggles,
        )
    }
}