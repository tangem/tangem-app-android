package com.tangem.tap.di.domain

import com.tangem.domain.jointaccount.repository.JointAccountSupportedNetworksRepository
import com.tangem.domain.jointaccount.usecase.GetJointAccountSupportedNetworksUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object JointAccountDomainModule {

    @Provides
    @Singleton
    fun provideGetJointAccountSupportedNetworksUseCase(
        repository: JointAccountSupportedNetworksRepository,
    ): GetJointAccountSupportedNetworksUseCase {
        return GetJointAccountSupportedNetworksUseCase(repository)
    }
}