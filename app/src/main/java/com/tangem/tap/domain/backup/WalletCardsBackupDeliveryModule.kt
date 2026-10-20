package com.tangem.tap.domain.backup

import com.tangem.domain.wallets.backup.WalletCardsBackupDeliveryScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletCardsBackupDeliveryModule {

    @Binds
    fun bindWalletCardsBackupDeliveryScheduler(
        impl: DefaultWalletCardsBackupDeliveryScheduler,
    ): WalletCardsBackupDeliveryScheduler
}