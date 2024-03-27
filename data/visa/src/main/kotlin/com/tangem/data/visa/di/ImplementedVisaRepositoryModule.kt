package com.tangem.data.visa.di

import com.tangem.domain.visa.repository.VisaRepository
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ImplementedVisaRepositoryModule {

    @BindsOptionalOf
    @ImplementedVisaRepository
    fun bindImplementedVisaRepository(): VisaRepository
}