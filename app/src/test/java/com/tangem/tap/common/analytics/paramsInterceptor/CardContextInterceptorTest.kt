package com.tangem.tap.common.analytics.paramsInterceptor

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CardContextInterceptorTest {

    private val walletsRepository: WalletsRepository = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletsRepository)
        coEvery { walletsRepository.isWalletWithRing(any()) } returns false
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN product type WHEN intercept THEN product type param matches the spec`(model: ProductTypeModel) {
        // Arrange
        val params = mutableMapOf<String, String>()

        // Act
        CardContextInterceptor(createScanResponse(model.productType), walletsRepository).intercept(params)

        // Assert
        assertThat(params["Product Type"]).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN wallet linked with a ring WHEN intercept THEN product type is Ring`() {
        // Arrange
        coEvery { walletsRepository.isWalletWithRing(any()) } returns true
        val params = mutableMapOf<String, String>()

        // Act
        CardContextInterceptor(createScanResponse(ProductType.Wallet2), walletsRepository).intercept(params)

        // Assert
        assertThat(params["Product Type"]).isEqualTo("Ring")
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN import wallet request WHEN canBeAppliedTo THEN applied only when the selected wallet is related`(
        isSelectedWalletUnrelated: Boolean,
    ) {
        // Arrange
        val interceptor = CardContextInterceptor(createScanResponse(ProductType.Wallet2), walletsRepository)
        val event = OnboardingAnalyticsEvent.Backup.ImportWalletRequest(
            cloudBackup = AnalyticsParam.CloudBackupAvailability.Available,
            isSelectedWalletUnrelated = isSelectedWalletUnrelated,
        )

        // Act
        val actual = interceptor.canBeAppliedTo(event)

        // Assert
        assertThat(actual).isEqualTo(!isSelectedWalletUnrelated)
    }

    private fun createScanResponse(productType: ProductType): ScanResponse {
        return MockScanResponseFactory.create(
            cardConfig = GenericCardConfig(maxWalletCount = 2),
            derivedKeys = emptyMap(),
        ).copy(productType = productType)
    }

    internal data class ProductTypeModel(val productType: ProductType, val expected: String)

    private fun provideTestModels() = listOf(
        ProductTypeModel(productType = ProductType.Note, expected = "Note"),
        ProductTypeModel(productType = ProductType.Twins, expected = "Twins"),
        ProductTypeModel(productType = ProductType.Wallet, expected = "Wallet"),
        ProductTypeModel(productType = ProductType.Start2Coin, expected = "Start2Coin"),
        ProductTypeModel(productType = ProductType.Wallet2, expected = "Wallet 2.0"),
        ProductTypeModel(productType = ProductType.Ring, expected = "Ring"),
    )
}