package com.tangem.datasource.di

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.Express
import com.tangem.datasource.api.common.config.TangemTech
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal object ApiConfigsModule {

    @Provides
    @IntoSet
    fun provideExpressConfig(): ApiConfig = Express()

    @Provides
    @IntoSet
    fun provideTangemTechConfig(): ApiConfig = TangemTech()
}