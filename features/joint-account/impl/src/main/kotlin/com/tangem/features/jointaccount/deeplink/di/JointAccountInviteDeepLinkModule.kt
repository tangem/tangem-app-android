package com.tangem.features.jointaccount.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.features.jointaccount.deeplink.DefaultJointAccountInviteDeepLinkHandler
import com.tangem.features.jointaccount.deeplink.JointAccountInviteDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountInviteDeepLinkModule {

    @Binds
    @Singleton
    fun bindJointAccountInviteDeepLinkHandlerFactory(
        impl: DefaultJointAccountInviteDeepLinkHandler.Factory,
    ): JointAccountInviteDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultJointAccountInviteDeepLinkHandler.DEEPLINK_KEY)
        fun provideJointAccountInviteDeepLinkHandler(
            factory: JointAccountInviteDeepLinkHandler.Factory,
            jointAccountFeatureToggles: JointAccountFeatureToggles,
        ): DeepLinkHandler = DeepLinkHandler(
            isEnabled = { jointAccountFeatureToggles.isJointAccountEnabled },
            handle = { request -> factory.create(coroutineScope = request.scope, deeplinkUri = request.uri) },
        )
    }
}