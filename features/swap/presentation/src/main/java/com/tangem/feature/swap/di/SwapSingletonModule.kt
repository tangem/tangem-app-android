package com.tangem.feature.swap.di

import com.tangem.feature.swap.di.impl.DefaultAmountFormatter
import com.tangem.feature.swap.domain.models.ui.AmountFormatter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class SwapSingletonModule {

    @Provides
    fun provideAmountFormatter(): AmountFormatter {
        return DefaultAmountFormatter()
    }
}