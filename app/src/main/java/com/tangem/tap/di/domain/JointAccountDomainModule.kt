package com.tangem.tap.di.domain

import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.repository.JointAccountSupportedNetworksRepository
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.CreateJointAccountUseCase
import com.tangem.domain.jointaccount.usecase.GetJointAccountSupportedNetworksUseCase
import com.tangem.tap.domain.jointaccount.DefaultJointAccountSigner
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

    @Provides
    @Singleton
    fun provideJointAccountSigner(signer: DefaultJointAccountSigner): JointAccountSigner = signer

    @Provides
    @Singleton
    fun provideCreateJointAccountUseCase(
        repository: JointAccountRepository,
        signer: JointAccountSigner,
        fetcher: SingleJointAccountListFetcher,
        supplier: SingleJointAccountListSupplier,
    ): CreateJointAccountUseCase {
        return CreateJointAccountUseCase(
            repository = repository,
            signer = signer,
            fetcher = fetcher,
            supplier = supplier,
        )
    }
}