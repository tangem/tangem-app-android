package com.tangem.tap.di.domain

import com.squareup.moshi.Moshi
import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.di.SdkMoshi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.walletmanager.DefaultWalletManagersFacade
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.onboarding.data.MnemonicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletManagersFacadeModule {

    @Provides
    @Singleton
    fun provideWalletManagersFacade(
        walletManagersStore: WalletManagersStore,
        userWalletsStore: UserWalletsStore,
        configManager: ConfigManager,
        mnemonicRepository: MnemonicRepository,
        assetReader: AssetReader,
        @SdkMoshi moshi: Moshi,
    ): WalletManagersFacade {
        return DefaultWalletManagersFacade(
            walletManagersStore = walletManagersStore,
            userWalletsStore = userWalletsStore,
            configManager = configManager,
            assetReader = assetReader,
            moshi = moshi,
            mnemonic = mnemonicRepository.generateDefaultMnemonic(),
        )
    }
}