package com.tangem.features.tangempay.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.tangempay.deeplink.DefaultTangemPayMainDeepLinkHandler
import com.tangem.features.tangempay.deeplink.DefaultTangemPayOnboardDeepLinkHandler
import com.tangem.features.tangempay.deeplink.TangemPayAccountDeepLinkHandler
import com.tangem.features.tangempay.deeplink.TangemPayMainDeepLinkHandler
import com.tangem.features.tangempay.deeplink.TangemPayOnboardDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDeeplinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(TangemPayAccountDeepLinkHandler.DEEPLINK_KEY)
    fun bindTangemPayAccountDeepLinkHandler(impl: TangemPayAccountDeepLinkHandler): DeepLinkHandler

    @Binds
    fun bindTangemPayOnboardDeepLinkHandler(
        impl: DefaultTangemPayOnboardDeepLinkHandler,
    ): TangemPayOnboardDeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(TangemPayOnboardDeepLinkHandler.TANGEM_DEEPLINK_KEY)
    fun bindTangemPayOnboardTangemDeepLinkHandler(impl: DefaultTangemPayOnboardDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(TangemPayOnboardDeepLinkHandler.HTTPS_DEEPLINK_KEY)
    fun bindTangemPayOnboardHttpsDeepLinkHandler(impl: DefaultTangemPayOnboardDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(TangemPayMainDeepLinkHandler.TANGEM_DEEPLINK_KEY)
    fun bindTangemPayMainDeepLinkHandler(impl: DefaultTangemPayMainDeepLinkHandler): DeepLinkHandler

    @Binds
    @IntoMap
    @DeepLinkKey(TangemPayMainDeepLinkHandler.HTTPS_DEEPLINK_KEY)
    fun bindTangemPayMainHttpsDeepLinkHandler(impl: DefaultTangemPayMainDeepLinkHandler): DeepLinkHandler
}