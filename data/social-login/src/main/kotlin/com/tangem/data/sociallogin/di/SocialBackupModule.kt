package com.tangem.data.sociallogin.di

import com.tangem.data.sociallogin.backup.DefaultSocialBackupRepository
import com.tangem.data.sociallogin.crypto.DefaultRecoveryKeyGenerator
import com.tangem.data.sociallogin.crypto.SocialBackupCipher
import com.tangem.data.sociallogin.store.SocialBackupStore
import com.tangem.data.sociallogin.store.UserWalletSocialBackupStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.sociallogin.crypto.RecoveryKeyGenerator
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import com.tangem.google.drive.AppDataDrive
import com.tangem.google.drive.GoogleDriveApi
import com.tangem.google.drive.GoogleDriveTokenProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SocialBackupModule {

    @Binds
    @Singleton
    fun bindRecoveryKeyGenerator(impl: DefaultRecoveryKeyGenerator): RecoveryKeyGenerator

    companion object {

        @Provides
        @Singleton
        fun provideSocialBackupStore(userWalletsListRepository: Lazy<UserWalletsListRepository>): SocialBackupStore {
            return UserWalletSocialBackupStore(userWalletsListRepositoryLazy = userWalletsListRepository)
        }

        @Provides
        @Singleton
        fun provideSocialBackupRepository(
            api: GoogleDriveApi,
            @AppDataDrive tokenProvider: GoogleDriveTokenProvider,
            store: SocialBackupStore,
            dispatchers: CoroutineDispatcherProvider,
        ): SocialBackupRepository {
            return DefaultSocialBackupRepository(
                api = api,
                tokenProvider = tokenProvider,
                store = store,
                cipher = SocialBackupCipher(),
                dispatchers = dispatchers,
            )
        }
    }
}