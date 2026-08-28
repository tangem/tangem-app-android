package com.tangem.features.jointaccount.deeplink.di

import com.tangem.features.jointaccount.deeplink.DefaultJointAccountDeepLinkHandler
import com.tangem.features.jointaccount.deeplink.JointAccountDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountDeepLinkModule {

    @Binds
    @Singleton
    fun bindJointAccountDeepLinkHandlerFactory(
        impl: DefaultJointAccountDeepLinkHandler.Factory,
    ): JointAccountDeepLinkHandler.Factory
}