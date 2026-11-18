package com.tangem.features.jointaccount.deeplink.di

import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkKey
import com.tangem.features.jointaccount.deeplink.JointAccountInviteDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountInviteDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(JointAccountInviteDeepLinkHandler.DEEPLINK_KEY)
    fun bindJointAccountInviteDeepLinkHandler(impl: JointAccountInviteDeepLinkHandler): DeepLinkHandler
}