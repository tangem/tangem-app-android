package com.tangem.data.pay.di

import com.tangem.data.pay.DefaultKycRepository
import com.tangem.domain.pay.repository.KycRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDataModule {

    @Binds
    @Singleton
    fun bindKycRepositoryFactory(factory: DefaultKycRepository.Factory): KycRepository.Factory
}