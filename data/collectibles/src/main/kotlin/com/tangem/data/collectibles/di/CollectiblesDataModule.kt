package com.tangem.data.collectibles.di

import com.tangem.data.collectibles.DefaultCollectiblesRepository
import com.tangem.data.collectibles.account.DefaultCollectiblesAccountRepository
import com.tangem.data.collectibles.account.cleaner.CollectiblesUserWalletDataCleaner
import com.tangem.data.collectibles.account.derivation.DefaultCollectiblesOwnerDeriver
import com.tangem.data.collectibles.vault.DefaultGachaVaultRepository
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CollectiblesDataModule {

    @Binds
    @Singleton
    fun bindCollectiblesOwnerDeriver(impl: DefaultCollectiblesOwnerDeriver): CollectiblesOwnerDeriver

    @Binds
    @Singleton
    fun bindCollectiblesRepository(impl: DefaultCollectiblesRepository): CollectiblesRepository

    @Binds
    @Singleton
    fun bindCollectiblesAccountRepository(impl: DefaultCollectiblesAccountRepository): CollectiblesAccountRepository

    @Binds
    @IntoSet
    fun bindCollectiblesUserWalletDataCleaner(impl: CollectiblesUserWalletDataCleaner): UserWalletDataCleaner

    @Binds
    @Singleton
    fun bindGachaVaultRepository(impl: DefaultGachaVaultRepository): GachaVaultRepository
}