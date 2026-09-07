package com.tangem.features.jointaccount.deeplink.di

import com.tangem.features.jointaccount.deeplink.DefaultJointAccountInviteDeepLinkHandler
import com.tangem.features.jointaccount.deeplink.JointAccountInviteDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountInviteDeepLinkModule {

    @Binds
    @Singleton
    fun bindJointAccountInviteDeepLinkHandlerFactory(
        impl: DefaultJointAccountInviteDeepLinkHandler.Factory,
    ): JointAccountInviteDeepLinkHandler.Factory
}