package com.tangem.tap.domain.userWalletList.di

import com.tangem.domain.common.wallets.UserWalletDataCleaner
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
internal interface UserWalletDataCleanerModule {

    @Multibinds
    fun userWalletDataCleaners(): Set<UserWalletDataCleaner>
}