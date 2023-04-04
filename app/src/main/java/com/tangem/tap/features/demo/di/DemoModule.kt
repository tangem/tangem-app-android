package com.tangem.tap.features.demo.di

import com.tangem.datasource.demo.DemoModeDatasource
import com.tangem.tap.features.demo.DefaultDemoModeDatasource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DemoModule {

    @Binds
    @Singleton
    fun bindDemoModeHelper(demoModeHelper: DefaultDemoModeDatasource): DemoModeDatasource
}
