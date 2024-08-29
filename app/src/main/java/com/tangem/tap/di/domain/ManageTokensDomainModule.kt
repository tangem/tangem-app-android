package com.tangem.tap.di.domain

import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.managetokens.SaveManagedTokensUseCase
import com.tangem.domain.managetokens.GetManagedTokensUseCase
import com.tangem.domain.managetokens.*
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.managetokens.repository.ManageTokensRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ManageTokensDomainModule {

    @Provides
    @Singleton
    fun provideGetManageTokensUseCase(manageTokensRepository: ManageTokensRepository): GetManagedTokensUseCase {
        return GetManagedTokensUseCase(manageTokensRepository)
    }

    @Provides
    @Singleton
    fun provideValidateTokenFormatUseCase(customTokensRepository: CustomTokensRepository): ValidateTokenFormUseCase {
        return ValidateTokenFormUseCase(customTokensRepository)
    }

    @Provides
    @Singleton
    fun provideCreateCurrencyUseCase(customTokensRepository: CustomTokensRepository): CreateCurrencyUseCase {
        return CreateCurrencyUseCase(customTokensRepository)
    }

    @Provides
    @Singleton
    fun provideFindTokenUseCase(customTokensRepository: CustomTokensRepository): FindTokenUseCase {
        return FindTokenUseCase(customTokensRepository)
    }

    @Provides
    @Singleton
    fun provideCheckIsCurrencyNotAddedUseCase(
        customTokensRepository: CustomTokensRepository,
    ): CheckIsCurrencyNotAddedUseCase {
        return CheckIsCurrencyNotAddedUseCase(customTokensRepository)
    }

    @Provides
    @Singleton
    fun provideRemoveCustomManagedCryptoCurrencyUseCase(
        customTokensRepository: CustomTokensRepository,
    ): RemoveCustomManagedCryptoCurrencyUseCase {
        return RemoveCustomManagedCryptoCurrencyUseCase(customTokensRepository)
    }

    @Provides
    @Singleton
    fun provideSaveManagedTokensUseCase(
        manageTokensRepository: ManageTokensRepository,
        derivationsRepository: DerivationsRepository,
    ): SaveManagedTokensUseCase {
        return SaveManagedTokensUseCase(
            manageTokensRepository = manageTokensRepository,
            derivationsRepository = derivationsRepository,
        )
    }
}
