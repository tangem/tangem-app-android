package com.tangem.tap.domain.walletregistration

import com.tangem.domain.wallets.registration.WalletRegistrationTrigger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletRegistrationModule {

    @Binds
    fun bindWalletRegistrationTrigger(impl: DefaultWalletRegistrationTrigger): WalletRegistrationTrigger
}