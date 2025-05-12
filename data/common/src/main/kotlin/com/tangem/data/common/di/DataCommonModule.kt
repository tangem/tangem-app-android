package com.tangem.data.common.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.DefaultCardCryptoCurrencyFactory
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.demo.DemoConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataCommonModule {

    @Provides
    @Singleton
    fun provideCardCryptoCurrencyFactory(
        excludedBlockchains: ExcludedBlockchains,
        userWalletsStore: UserWalletsStore,
        userTokensResponseStore: UserTokensResponseStore,
    ): CardCryptoCurrencyFactory {
        return DefaultCardCryptoCurrencyFactory(
            demoConfig = DemoConfig(),
            excludedBlockchains = excludedBlockchains,
            userWalletsStore = userWalletsStore,
            userTokensResponseStore = userTokensResponseStore,
        )
    }
}