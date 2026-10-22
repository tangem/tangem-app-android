package com.tangem.tap.di.domain

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkHandlerRegistry
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.qrscanning.QrDeepLinkMatcher
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import com.tangem.domain.qrscanning.usecases.EmitQrScannedEventUseCase
import com.tangem.domain.qrscanning.usecases.ExtractQrRecipientAddressUseCase
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.qrscanning.usecases.ParseQrCodeUseCase
import com.tangem.domain.qrscanning.usecases.ResolveQrSendTargetsUseCase
import com.tangem.tap.common.deeplink.DefaultQrDeepLinkMatcher
import com.tangem.tap.routing.utils.DeepLinkFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QrScanningDomainModule {

    @Provides
    @Singleton
    fun provideListenToQrScanUseCase(repository: QrScanningEventsRepository): ListenToQrScanningUseCase {
        return ListenToQrScanningUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideEmitQrScannedEventUseCase(repository: QrScanningEventsRepository): EmitQrScannedEventUseCase {
        return EmitQrScannedEventUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideParseQrCodeUseCase(repository: QrScanningEventsRepository): ParseQrCodeUseCase {
        return ParseQrCodeUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideExtractQrRecipientAddressUseCase(
        repository: QrScanningEventsRepository,
    ): ExtractQrRecipientAddressUseCase {
        return ExtractQrRecipientAddressUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideResolveQrSendTargetsUseCase(
        multiAccountListSupplier: MultiAccountListSupplier,
        qrScanningEventsRepository: QrScanningEventsRepository,
        userWalletsListRepository: UserWalletsListRepository,
        networksRepository: NetworksRepository,
    ): ResolveQrSendTargetsUseCase {
        return ResolveQrSendTargetsUseCase(
            multiAccountListSupplier = multiAccountListSupplier,
            qrScanningEventsRepository = qrScanningEventsRepository,
            userWalletsListRepository = userWalletsListRepository,
            networksRepository = networksRepository,
        )
    }

    @Provides
    @Singleton
    fun provideQrDeepLinkMatcher(
        deepLinkHandlers: Map<String, @JvmSuppressWildcards DeepLinkHandler>,
        deepLinkFeatureToggles: DeepLinkFeatureToggles,
    ): QrDeepLinkMatcher {
        return DefaultQrDeepLinkMatcher(
            deepLinkHandlerRegistry = DeepLinkHandlerRegistry(deepLinkHandlers),
            deepLinkFeatureToggles = deepLinkFeatureToggles,
        )
    }
}