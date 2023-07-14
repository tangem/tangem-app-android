package com.tangem.tap.proxy.di

import androidx.compose.ui.text.intl.Locale
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.feature.learn2earn.domain.api.Learn2earnDependencyProvider
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.proxy.DerivationManagerImpl
import com.tangem.tap.proxy.TransactionManagerImpl
import com.tangem.tap.proxy.UserWalletManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ProxyModule {

    @Provides
    @Singleton
    fun provideAppStateHolder(): AppStateHolder {
        return AppStateHolder()
    }

    @Provides
    @Singleton
    fun provideUserWalletManager(appStateHolder: AppStateHolder): UserWalletManager {
        return UserWalletManagerImpl(
            appStateHolder = appStateHolder,
        )
    }

    @Provides
    @Singleton
    fun provideTransactionManager(
        appStateHolder: AppStateHolder,
        analytics: AnalyticsEventHandler,
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): TransactionManager {
        return TransactionManagerImpl(
            appStateHolder = appStateHolder,
            analytics = analytics,
            cardSdkConfigRepository = cardSdkConfigRepository,
        )
    }

    @Provides
    @Singleton
    fun provideDerivationManager(appStateHolder: AppStateHolder): DerivationManager {
        return DerivationManagerImpl(
            appStateHolder = appStateHolder,
        )
    }

    // regions FeatureConsumers
    @Provides
    @Singleton
    fun provideLear2earnDependencies(appStateHolder: AppStateHolder): Learn2earnDependencyProvider {
        return object : Learn2earnDependencyProvider {
            override fun getUserCountryCodeProvider(): () -> String = {
                appStateHolder.mainStore?.state?.globalState?.userCountryCode ?: Locale.current.language
            }

            override fun getWebViewAuthCredentialsProvider(): () -> String? = {
                appStateHolder.mainStore?.state?.globalState?.configManager?.config?.tangemComAuthorization
            }
        }
    }
    // endregion FeatureConsumers
}
