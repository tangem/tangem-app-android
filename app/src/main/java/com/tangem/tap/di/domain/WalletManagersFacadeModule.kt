package com.tangem.tap.di.domain

import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.walletmanager.DefaultWalletManagersFacade
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
        assetLoader: AssetLoader,
        blockchainSDKFactory: BlockchainSDKFactory,
        dispatchers: CoroutineDispatcherProvider,
    ): WalletManagersFacade {
        return DefaultWalletManagersFacade(
            walletManagersStore = walletManagersStore,
            userWalletsStore = userWalletsStore,
            assetLoader = assetLoader,
            dispatchers = dispatchers,
            blockchainSDKFactory = blockchainSDKFactory,
        )
    }
}