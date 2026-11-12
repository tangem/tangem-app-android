package com.tangem.store.features.cardsettings.impl.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.domain.card.configs.CardConfig
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.card.configs.MultiWalletCardConfig
import com.tangem.domain.card.configs.Wallet2CardConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class CardSettingsInteractorTest {

    private val interactor = CardSettingsInteractor()

    @BeforeEach
    fun clearInteractor() {
        interactor.clear()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RequireScanResponse {

        @Test
        fun `GIVEN initialized interactor WHEN requireScanResponse THEN returns scan response`() {
            // Arrange
            val scanResponse = createScanResponse()
            interactor.initialize(scanResponse)

            // Act
            val actual = interactor.requireScanResponse()

            // Assert
            assertThat(actual).isEqualTo(scanResponse)
        }

        @Test
        fun `GIVEN cleared interactor WHEN requireScanResponse THEN throws`() {
            // Arrange
            interactor.initialize(createScanResponse())
            interactor.clear()

            // Act
            val actual = runCatching { interactor.requireScanResponse() }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(actual).hasMessageThat().isEqualTo("Scan response is null")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetSecurityOption {

        @ParameterizedTest
        @ProvideTestModels
        fun getSecurityOption(model: GetSecurityOptionModel) {
            // Arrange
            val card = createScanResponse(
                isAccessCodeSet = model.isAccessCodeSet,
                isPasscodeSet = model.isPasscodeSet,
            ).card

            // Act
            val actual = interactor.getSecurityOption(card)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            GetSecurityOptionModel(isAccessCodeSet = true, isPasscodeSet = true, expected = SecurityOption.AccessCode),
            GetSecurityOptionModel(isAccessCodeSet = false, isPasscodeSet = true, expected = SecurityOption.PassCode),
            GetSecurityOptionModel(isAccessCodeSet = false, isPasscodeSet = false, expected = SecurityOption.LongTap),
            GetSecurityOptionModel(isAccessCodeSet = false, isPasscodeSet = null, expected = SecurityOption.LongTap),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAllowedSecurityOptions {

        @ParameterizedTest
        @ProvideTestModels
        fun getAllowedSecurityOptions(model: GetAllowedSecurityOptionsModel) {
            // Act
            val actual = interactor.getAllowedSecurityOptions(model.scanResponse)

            // Assert
            assertThat(actual).containsExactlyElementsIn(model.expected)
        }

        private fun provideTestModels() = listOf(
            GetAllowedSecurityOptionsModel(
                name = "Start2Coin with access code",
                scanResponse = createScanResponse(issuerName = START_2_COIN_ISSUER, isAccessCodeSet = true),
                expected = setOf(SecurityOption.LongTap),
            ),
            GetAllowedSecurityOptionsModel(
                name = "Note with access code",
                scanResponse = createScanResponse(productType = ProductType.Note, isAccessCodeSet = true),
                expected = setOf(SecurityOption.LongTap),
            ),
            GetAllowedSecurityOptionsModel(
                name = "Backup allowed with access code",
                scanResponse = createScanResponse(cardConfig = MultiWalletCardConfig, isAccessCodeSet = true),
                expected = setOf(SecurityOption.AccessCode),
            ),
            GetAllowedSecurityOptionsModel(
                name = "Twins with long tap",
                scanResponse = createScanResponse(productType = ProductType.Twins),
                expected = setOf(SecurityOption.LongTap, SecurityOption.PassCode),
            ),
            GetAllowedSecurityOptionsModel(
                name = "Twins with access code",
                scanResponse = createScanResponse(productType = ProductType.Twins, isAccessCodeSet = true),
                expected = setOf(SecurityOption.LongTap, SecurityOption.AccessCode, SecurityOption.PassCode),
            ),
            GetAllowedSecurityOptionsModel(
                name = "Single wallet with access code",
                scanResponse = createScanResponse(isAccessCodeSet = true),
                expected = setOf(SecurityOption.LongTap, SecurityOption.AccessCode),
            ),
            GetAllowedSecurityOptionsModel(
                name = "Single wallet with long tap",
                scanResponse = createScanResponse(),
                expected = setOf(SecurityOption.LongTap),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AccessCodeRecovery {

        @ParameterizedTest
        @ProvideTestModels
        fun accessCodeRecovery(model: AccessCodeRecoveryModel) {
            // Arrange
            val scanResponse = createScanResponse(
                cardConfig = model.cardConfig,
                userSettings = model.userSettings,
            )

            // Act
            val isAllowed = interactor.isAccessCodeRecoveryAllowed(scanResponse)
            val isEnabled = interactor.isAccessCodeRecoveryEnabled(scanResponse)

            // Assert
            assertThat(isAllowed).isEqualTo(model.isAllowed)
            assertThat(isEnabled).isEqualTo(model.isEnabled)
        }

        private fun provideTestModels() = listOf(
            AccessCodeRecoveryModel(
                cardConfig = Wallet2CardConfig,
                userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = true),
                isAllowed = true,
                isEnabled = true,
            ),
            AccessCodeRecoveryModel(
                cardConfig = Wallet2CardConfig,
                userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = false),
                isAllowed = true,
                isEnabled = false,
            ),
            AccessCodeRecoveryModel(
                cardConfig = Wallet2CardConfig,
                userSettings = null,
                isAllowed = true,
                isEnabled = false,
            ),
            AccessCodeRecoveryModel(
                cardConfig = MultiWalletCardConfig,
                userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = true),
                isAllowed = false,
                isEnabled = false,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UpdateSecurityOption {

        @ParameterizedTest
        @ProvideTestModels
        fun updateSecurityOption(model: UpdateSecurityOptionModel) {
            // Arrange
            val scanResponse = createScanResponse(productType = ProductType.Twins, isAccessCodeSet = true)
            interactor.initialize(scanResponse)

            // Act
            interactor.updateSecurityOption(model.option)

            // Assert
            val expected = scanResponse.copy(
                card = scanResponse.card.copy(
                    isAccessCodeSet = model.isAccessCodeSet,
                    isPasscodeSet = model.isPasscodeSet,
                ),
            )
            assertThat(interactor.scannedScanResponse.value).isEqualTo(expected)
        }

        @Test
        fun `GIVEN not initialized interactor WHEN updateSecurityOption THEN throws`() {
            // Act
            val actual = runCatching { interactor.updateSecurityOption(SecurityOption.LongTap) }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(IllegalStateException::class.java)
            assertThat(actual).hasMessageThat().isEqualTo("Scan response is null")
        }

        private fun provideTestModels() = listOf(
            UpdateSecurityOptionModel(SecurityOption.AccessCode, isAccessCodeSet = true, isPasscodeSet = false),
            UpdateSecurityOptionModel(SecurityOption.PassCode, isAccessCodeSet = false, isPasscodeSet = true),
            UpdateSecurityOptionModel(SecurityOption.LongTap, isAccessCodeSet = false, isPasscodeSet = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UpdateAccessCodeRecovery {

        @Test
        fun `GIVEN recovery disabled WHEN updateAccessCodeRecovery true THEN recovery enabled in scan response`() {
            // Arrange
            val scanResponse = createScanResponse(
                cardConfig = Wallet2CardConfig,
                userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = false),
            )
            interactor.initialize(scanResponse)

            // Act
            interactor.updateAccessCodeRecovery(isEnabled = true)

            // Assert
            val expected = scanResponse.copy(
                card = scanResponse.card.copy(userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = true)),
            )
            assertThat(interactor.scannedScanResponse.value).isEqualTo(expected)
        }

        @Test
        fun `GIVEN card without user settings WHEN updateAccessCodeRecovery THEN scan response unchanged`() {
            // Arrange
            val scanResponse = createScanResponse(cardConfig = Wallet2CardConfig, userSettings = null)
            interactor.initialize(scanResponse)

            // Act
            interactor.updateAccessCodeRecovery(isEnabled = true)

            // Assert
            assertThat(interactor.scannedScanResponse.value).isEqualTo(scanResponse)
        }

        @Test
        fun `GIVEN not initialized interactor WHEN updateAccessCodeRecovery THEN throws`() {
            // Act
            val actual = runCatching { interactor.updateAccessCodeRecovery(isEnabled = true) }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(IllegalStateException::class.java)
            assertThat(actual).hasMessageThat().isEqualTo("Scan response is null")
        }
    }

    data class GetSecurityOptionModel(
        val isAccessCodeSet: Boolean,
        val isPasscodeSet: Boolean?,
        val expected: SecurityOption,
    )

    data class GetAllowedSecurityOptionsModel(
        val name: String,
        val scanResponse: ScanResponse,
        val expected: Set<SecurityOption>,
    ) {
        override fun toString(): String = name
    }

    data class AccessCodeRecoveryModel(
        val cardConfig: CardConfig,
        val userSettings: CardDTO.UserSettings?,
        val isAllowed: Boolean,
        val isEnabled: Boolean,
    )

    data class UpdateSecurityOptionModel(
        val option: SecurityOption,
        val isAccessCodeSet: Boolean,
        val isPasscodeSet: Boolean,
    )

    private companion object {
        const val START_2_COIN_ISSUER = "start2coin"

        fun createScanResponse(
            cardConfig: CardConfig = GenericCardConfig(maxWalletCount = 1),
            productType: ProductType = ProductType.Wallet,
            issuerName: String = "Tangem",
            isAccessCodeSet: Boolean = false,
            isPasscodeSet: Boolean? = false,
            userSettings: CardDTO.UserSettings? = null,
        ): ScanResponse {
            val scanResponse = MockScanResponseFactory.create(cardConfig = cardConfig, derivedKeys = emptyMap())

            return scanResponse.copy(
                productType = productType,
                card = scanResponse.card.copy(
                    issuer = scanResponse.card.issuer.copy(name = issuerName),
                    isAccessCodeSet = isAccessCodeSet,
                    isPasscodeSet = isPasscodeSet,
                    userSettings = userSettings,
                ),
            )
        }
    }
}