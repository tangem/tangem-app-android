package com.tangem.spend.datasource.di

import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.Timeouts
import com.tangem.core.remote.build
import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.moshi.NetworkMoshiConfigurer
import com.tangem.spend.datasource.config.TangemPay
import com.tangem.spend.datasource.config.TangemPayEnvironmentConfig
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.TangemPayAuthApi
import com.tangem.spend.datasource.pay.addTangemPayEnumFallbackAdapters
import com.tangem.spend.datasource.pay.models.response.RawJsonAdapter
import com.tangem.spend.datasource.visa.VisaApi
import com.tangem.spend.datasource.visa.entity.PaymentAccountStatusValueDM
import com.tangem.spend.datasource.visa.entity.MoneyAccountStatusValueDM
import com.tangem.utils.info.AppInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import dev.onenowy.moshipolymorphicadapter.NamePolymorphicAdapterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SpendDatasourceModule {

    private const val TIMEOUT_60_SECONDS = 60L

    @Provides
    @IntoMap
    @StringKey(TangemPay.Bff.KEY)
    fun provideTangemPayBffConfig(
        environmentConfig: TangemPayEnvironmentConfig,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return TangemPay.Bff(environmentConfig, appInfoProvider)
    }

    @Provides
    @IntoMap
    @StringKey(TangemPay.Auth.KEY)
    fun provideTangemPayAuthConfig(
        environmentConfig: TangemPayEnvironmentConfig,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return TangemPay.Auth(environmentConfig, appInfoProvider)
    }

    @Provides
    @Singleton
    fun provideTangemPayApi(factory: RetrofitFactory): TangemPayApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemPay.Bff.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
                timeouts = Timeouts(
                    callTimeoutSeconds = TIMEOUT_60_SECONDS,
                    connectTimeoutSeconds = TIMEOUT_60_SECONDS,
                    readTimeoutSeconds = TIMEOUT_60_SECONDS,
                ),
            ),
        )
    }

    @Provides
    @Singleton
    fun provideVisaApi(factory: RetrofitFactory): VisaApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemPay.Bff.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
                timeouts = Timeouts(
                    callTimeoutSeconds = TIMEOUT_60_SECONDS,
                    connectTimeoutSeconds = TIMEOUT_60_SECONDS,
                    readTimeoutSeconds = TIMEOUT_60_SECONDS,
                ),
            ),
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayAuthApi(factory: RetrofitFactory): TangemPayAuthApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemPay.Auth.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @IntoSet
    fun provideVisaMoshiConfigurer(): NetworkMoshiConfigurer {
        return NetworkMoshiConfigurer { builder ->
            builder
                .addTangemPayEnumFallbackAdapters()
                .add(RawJsonAdapter())
                .add(
                    NamePolymorphicAdapterFactory.of(PaymentAccountStatusValueDM::class.java)
                        .withSubtype(PaymentAccountStatusValueDM.Empty::class.java, "empty")
                        .withSubtype(PaymentAccountStatusValueDM.NotCreated::class.java, "not_created")
                        .withSubtype(PaymentAccountStatusValueDM.UnderReview::class.java, "kyc_status")
                        .withSubtype(PaymentAccountStatusValueDM.IssuingCard::class.java, "issuing_card")
                        .withSubtype(PaymentAccountStatusValueDM.ActiveAccount::class.java, "active_account")
                        .withSubtype(PaymentAccountStatusValueDM.DeactivatedAccount::class.java, "deactivated_account")
                        .withSubtype(PaymentAccountStatusValueDM.CardIssueFailed::class.java, "card_issue_failed"),
                )
                .add(
                    NamePolymorphicAdapterFactory.of(MoneyAccountStatusValueDM::class.java)
                        .withSubtype(MoneyAccountStatusValueDM.Empty::class.java, "empty")
                        .withSubtype(MoneyAccountStatusValueDM.NotCreated::class.java, "not_created")
                        .withSubtype(MoneyAccountStatusValueDM.UnderReview::class.java, "kyc_status")
                        .withSubtype(MoneyAccountStatusValueDM.Issuing::class.java, "issuing")
                        .withSubtype(
                            MoneyAccountStatusValueDM.VerificationDeclined::class.java,
                            "verification_declined",
                        )
                        .withSubtype(MoneyAccountStatusValueDM.Activating::class.java, "activating")
                        .withSubtype(
                            MoneyAccountStatusValueDM.CountryNotSupported::class.java,
                            "country_not_supported",
                        )
                        .withSubtype(MoneyAccountStatusValueDM.ActiveAccount::class.java, "active_account")
                        .withSubtype(MoneyAccountStatusValueDM.Deactivated::class.java, "deactivated"),
                )
        }
    }
}