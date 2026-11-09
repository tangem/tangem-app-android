package com.tangem.tap.domain.walletcardsbackup

import com.tangem.domain.wallets.backup.SeenWalletCardReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletCardsBackupModule {

    @Binds
    fun bindSeenWalletCardReporter(impl: DefaultSeenWalletCardReporter): SeenWalletCardReporter
}