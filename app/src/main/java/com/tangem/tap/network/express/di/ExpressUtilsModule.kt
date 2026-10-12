package com.tangem.tap.network.express.di

import com.tangem.grow.datasource.express.ExpressUtils
import com.tangem.tap.network.express.DefaultExpressUtils
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ExpressUtilsModule {

    @Binds
    fun bindExpressUtils(impl: DefaultExpressUtils): ExpressUtils
}