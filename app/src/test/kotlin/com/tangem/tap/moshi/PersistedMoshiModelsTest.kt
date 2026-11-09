package com.tangem.tap.moshi

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.moshi.PersistedModelGuard
import com.tangem.datasource.common.di.MoshiModule
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.tap.domain.userWalletList.di.UserWalletsListRepositoryModule
import com.tangem.tap.domain.userWalletList.model.UserWalletEncryptionKey
import com.tangem.tap.domain.userWalletList.model.UserWalletPublicInformation
import com.tangem.tap.domain.userWalletList.model.UserWalletSensitiveInformation
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/** Fails CI on a field without codegen in today's persisted models; new ones are left to [PersistedModelGuard] */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersistedMoshiModelsTest {

    private val guard = PersistedModelGuard(isStrict = true)

    @ParameterizedTest
    @MethodSource("provideUserWalletsListModels")
    fun `GIVEN user wallets list model WHEN its adapter is created THEN no class is read by reflection`(
        model: Class<*>,
    ) {
        // Arrange
        val moshi = UserWalletsListRepositoryModule.buildMoshi(persistedModelGuard = guard)

        // Act
        val error = runCatching { moshi.adapter(model) }.exceptionOrNull()

        // Assert
        assertThat(error).isNull()
    }

    @ParameterizedTest
    @MethodSource("providePreferencesModels")
    fun `GIVEN preferences model WHEN its adapter is created THEN no class is read by reflection`(
        modelName: String,
    ) {
        // Arrange
        val moshi = MoshiModule()
            .provideSdkMoshi(setOf(TestSdkMoshiConfigurers.cardSdk, TestSdkMoshiConfigurers.scanResponse))
            .newBuilder()
            .add(guard)
            .build()

        // Act
        val error = runCatching { moshi.adapter(Class.forName(modelName)) }.exceptionOrNull()

        // Assert
        assertThat(error).isNull()
    }

    private fun provideUserWalletsListModels(): List<Class<*>> = listOf(
        UserWalletPublicInformation::class.java,
        UserWalletSensitiveInformation::class.java,
        UserWalletEncryptionKey::class.java,
        UserWalletId::class.java,
    )

    // By name, since some of them are internal to their modules
    private fun providePreferencesModels(): List<String> = listOf(
        "com.tangem.datasource.api.tangemTech.models.UserTokensResponse",
        "com.tangem.datasource.api.tangemTech.models.ApplicationVersionsResponse",
        "com.tangem.datasource.api.tangemTech.models.CurrenciesResponse\$Currency",
        "com.tangem.data.appsflyer.store.ConversionDataDTO",
        "com.tangem.data.card.model.UsedCardInfo",
        "com.tangem.domain.models.scan.ScanResponse",
        "com.tangem.grow.datasource.onramp.models.response.model.OnrampCountryDTO",
        "com.tangem.data.onramp.models.OnrampTransactionDTO",
        "com.tangem.data.onramp.models.OnrampTerminalTransactionDTO",
        "com.tangem.domain.staking.model.UnsubmittedTransactionMetadata",
        "com.tangem.data.swap.models.SwapTransactionListDTO",
        "com.tangem.data.swap.models.SwapStatusDTO",
        "com.tangem.data.swap.models.LastSwappedCryptoCurrencyDTO",
        "com.tangem.data.wallets.DefaultWalletsPromoRepository\$ReferralWalletsBindingData",
        "com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModelInner",
        "com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel",
        "com.tangem.feature.swap.domain.models.domain.SavedLastSwappedCryptoCurrency",
        "com.tangem.data.balancehiding.model.BalanceHidingSettingsDTO",
    )
}