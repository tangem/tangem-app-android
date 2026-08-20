package com.tangem.tap.domain.walletregistration.di

import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.tap.domain.walletregistration.WalletRegistrationDataCleaner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletRegistrationDataCleanerModule {

    @Binds
    @IntoSet
    fun bindWalletRegistrationDataCleaner(impl: WalletRegistrationDataCleaner): UserWalletDataCleaner
}