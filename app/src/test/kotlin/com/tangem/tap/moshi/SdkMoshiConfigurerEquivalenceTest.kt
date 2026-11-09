package com.tangem.tap.moshi

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.core.local.moshi.SdkMoshiConfigurer
import com.tangem.datasource.common.di.MoshiModule
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import org.junit.jupiter.api.Test

/**
 * The SDK-Moshi adapters are split into two @IntoSet [SdkMoshiConfigurer]s (card SDK from tangem-sdk-api,
 * ScanResponse from onboarding). The @IntoSet Set is unordered, so the two must not conflict on any type —
 * otherwise ScanResponse (which @SdkMoshi persists to prefs) would serialize differently depending on the
 * Set order. Asserting both orders produce identical JSON proves they don't conflict.
 */
internal class SdkMoshiConfigurerEquivalenceTest {

    private val scanResponse: ScanResponse = MockScanResponseFactory.create(
        cardConfig = GenericCardConfig(maxWalletCount = 2),
        derivedKeys = emptyMap(),
    ).let { it.copy(card = it.card.copy(backupStatus = CardDTO.BackupStatus.Active(cardCount = 1))) }

    @Test
    fun `GIVEN configurers in either order WHEN provideSdkMoshi THEN ScanResponse json is identical`() {
        // Arrange
        val cardSdkFirst = MoshiModule()
            .provideSdkMoshi(linkedSetOf(TestSdkMoshiConfigurers.cardSdk, TestSdkMoshiConfigurers.scanResponse))
        val scanResponseFirst = MoshiModule()
            .provideSdkMoshi(linkedSetOf(TestSdkMoshiConfigurers.scanResponse, TestSdkMoshiConfigurers.cardSdk))

        // Act
        val cardSdkFirstJson = cardSdkFirst.adapter(ScanResponse::class.java).toJson(scanResponse)
        val scanResponseFirstJson = scanResponseFirst.adapter(ScanResponse::class.java).toJson(scanResponse)

        // Assert
        assertThat(scanResponseFirstJson).isEqualTo(cardSdkFirstJson)
    }
}