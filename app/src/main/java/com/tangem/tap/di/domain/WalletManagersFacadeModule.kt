package com.tangem.tap.di.domain

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.di.SdkMoshi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.walletmanager.DefaultWalletManagersFacade
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletManagersFacadeModule {

    @Provides
    @Singleton
    fun provideWalletManagersFacade(
        @ApplicationContext context: Context,
        walletManagersStore: WalletManagersStore,
        userWalletsStore: UserWalletsStore,
        configManager: ConfigManager,
        assetReader: AssetReader,
        @SdkMoshi moshi: Moshi,
    ): WalletManagersFacade {
        return DefaultWalletManagersFacade(
            context = context,
            walletManagersStore = walletManagersStore,
            userWalletsStore = userWalletsStore,
            configManager = configManager,
            assetReader = assetReader,
            moshi = moshi,
        )
    }
}
