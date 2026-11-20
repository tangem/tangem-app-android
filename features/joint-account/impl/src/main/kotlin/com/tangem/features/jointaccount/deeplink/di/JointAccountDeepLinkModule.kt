package com.tangem.features.jointaccount.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.jointaccount.deeplink.DefaultJointAccountDeepLinkHandler
import com.tangem.features.jointaccount.deeplink.JointAccountDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(JointAccountDeepLinkHandler.DEEPLINK_KEY)
    fun bindJointAccountDeepLinkHandler(impl: DefaultJointAccountDeepLinkHandler): DeepLinkHandler
}