package com.tangem.tap.domain.walletregistration

import com.tangem.data.sociallogin.recovery.HotWalletSignerFactory
import com.tangem.domain.wallets.registration.WalletDeviceLinkRepository
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

    @Binds
    fun bindWalletDeviceLinkRepository(impl: DefaultWalletDeviceLinkRepository): WalletDeviceLinkRepository

    @Binds
    fun bindHotWalletSignerFactory(impl: MobileWalletRegistrationSigner): HotWalletSignerFactory
}