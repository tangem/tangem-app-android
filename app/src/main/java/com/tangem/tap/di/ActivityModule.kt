package com.tangem.tap.di

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.tangem.TangemSdk
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.sdk.extensions.initWithBiometrics
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.scanCard.repository.DefaultScanCardRepository
import com.tangem.tap.userTokensRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal object ActivityModule {

    @Provides
    @ActivityScoped
    fun provideTangemSdk(@ActivityContext context: Context): TangemSdk {
        return TangemSdk.initWithBiometrics(context as FragmentActivity, TangemSdkManager.config)
    }

    @Provides
    @ActivityScoped
    fun provideTangemSdkManager(@ActivityContext context: Context, tangemSdk: TangemSdk): TangemSdkManager {
        return TangemSdkManager(tangemSdk, context)
    }

    @Provides
    @ActivityScoped
    fun provideScanCardUseCase(tangemSdk: TangemSdk, tangemSdkManager: TangemSdkManager): ScanCardUseCase {
        return ScanCardUseCase(
            tangemSdk = tangemSdk,
            scanCardRepository = DefaultScanCardRepository(userTokensRepository, tangemSdkManager),
        )
    }
}