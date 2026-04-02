package com.tangem.data.qrscanning.di

import com.tangem.data.qrscanning.parser.Bip321PaymentUriParser
import com.tangem.data.qrscanning.parser.Eip681PaymentUriParser
import com.tangem.data.qrscanning.parser.QrContentClassifierParser
import com.tangem.data.qrscanning.parser.SolanaPaymentUriParser
import com.tangem.data.qrscanning.parser.TronPaymentUriParser
import com.tangem.data.qrscanning.repository.DefaultQrScanningEventsRepository
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QrScanningDataModule {

    @Provides
    @Singleton
    fun provideQrScanningEventsRepository(): QrScanningEventsRepository {
        val blockchainDataProvider = QrContentClassifierParser.DefaultBlockchainDataProvider()
        return DefaultQrScanningEventsRepository(
            qrContentClassifierParser = QrContentClassifierParser(
                blockchainDataProvider = blockchainDataProvider,
                paymentUriParsers = setOf(
                    Eip681PaymentUriParser(blockchainDataProvider),
                    TronPaymentUriParser(blockchainDataProvider),
                    SolanaPaymentUriParser(blockchainDataProvider),
                    Bip321PaymentUriParser(blockchainDataProvider),
                ),
            ),
        )
    }
}