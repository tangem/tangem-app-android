package com.tangem.feature.tokendetails.di

import com.tangem.common.routing.AppRouter
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.feature.tokendetails.presentation.router.DefaultTokenDetailsRouter
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal object TokenDetailsRouterModule {

    @Provides
    @ActivityScoped
    fun provideTokenDetailsRouter(
        appRouter: AppRouter,
        urlOpener: UrlOpener,
        shareManager: ShareManager,
    ): TokenDetailsRouter {
        return DefaultTokenDetailsRouter(appRouter, urlOpener, shareManager)
    }
}
