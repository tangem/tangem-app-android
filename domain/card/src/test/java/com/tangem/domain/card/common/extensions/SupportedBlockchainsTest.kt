package com.tangem.domain.card.common.extensions

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.operations.attestation.Attestation
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.util.Date

/**
 * Tests for the firmware-based blockchain filter in [supportedBlockchains].
 *
 * Regression: Robinhood and Igra were unavailable on cards without HD wallets.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SupportedBlockchainsTest {

    private val excludedBlockchains = ExcludedBlockchains()

    @ParameterizedTest
    @ProvideTestModels
    fun supportedBlockchains(model: TestModel) {
        // Arrange
        val card = createCard(firmwareVersion = model.firmwareVersion)
        val cardTypesResolver = createCardTypesResolver(isTangemWallet = model.isTangemWallet)

        // Act
        val actual = card.supportedBlockchains(
            cardTypesResolver = cardTypesResolver,
            excludedBlockchains = excludedBlockchains,
        )

        // Assert
        assertThat(actual).containsAtLeastElementsIn(model.expectedContains)
        assertThat(actual).containsNoneIn(model.expectedNotContains)
    }

    @Test
    fun `GIVEN card without HD wallets WHEN supportedTokens THEN Robinhood and Igra are available`() {
        // Arrange
        val card = createCard(firmwareVersion = MULTI_WHITE_FIRMWARE)

        // Act
        val actual = card.supportedTokens(
            cardTypesResolver = createCardTypesResolver(isTangemWallet = true),
            excludedBlockchains = excludedBlockchains,
        )

        // Assert
        assertThat(actual).containsAtLeast(Blockchain.Robinhood, Blockchain.Igra)
    }

    private fun provideTestModels() = listOf(
        TestModel(
            firmwareVersion = WALLET_V3_FIRMWARE,
            isTangemWallet = false,
            expectedContains = listOf(Blockchain.Ethereum, Blockchain.Robinhood, Blockchain.Igra),
            expectedNotContains = listOf(Blockchain.Quai, Blockchain.Adi, Blockchain.SeiEvm),
        ),
        // 4.12 Multi White: cardId FF79000000000000, batch CB79, firmware 4.12d
        TestModel(
            firmwareVersion = MULTI_WHITE_FIRMWARE,
            isTangemWallet = true,
            expectedContains = listOf(Blockchain.Ethereum, Blockchain.Robinhood, Blockchain.Igra),
            expectedNotContains = listOf(Blockchain.Quai, Blockchain.Adi, Blockchain.SeiEvm),
        ),
        // Wallet 2.0: HD wallets are available, nothing is filtered out by firmware
        TestModel(
            firmwareVersion = WALLET_2_FIRMWARE,
            isTangemWallet = true,
            expectedContains = listOf(
                Blockchain.Ethereum,
                Blockchain.Robinhood,
                Blockchain.Igra,
                Blockchain.Quai,
                Blockchain.Adi,
                Blockchain.SeiEvm,
            ),
            expectedNotContains = listOf(Blockchain.RobinhoodTestnet, Blockchain.IgraTestnet),
        ),
    )

    internal data class TestModel(
        val firmwareVersion: CardDTO.FirmwareVersion,
        val isTangemWallet: Boolean,
        val expectedContains: List<Blockchain>,
        val expectedNotContains: List<Blockchain>,
    )

    private fun createCardTypesResolver(isTangemWallet: Boolean): CardTypesResolver = mockk {
        every { isWallet2() } returns false
        every { isTangemWallet() } returns isTangemWallet
    }

    private fun createCard(
        firmwareVersion: CardDTO.FirmwareVersion,
        curves: List<EllipticCurve> = listOf(EllipticCurve.Secp256k1),
    ): CardDTO = CardDTO(
        cardId = "CB37000000000002",
        batchId = "0045",
        cardPublicKey = ByteArray(0),
        firmwareVersion = firmwareVersion,
        manufacturer = CardDTO.Manufacturer(name = "TANGEM", manufactureDate = Date(), signature = null),
        issuer = CardDTO.Issuer(name = "TANGEM", publicKey = ByteArray(0)),
        settings = CardDTO.Settings(
            securityDelay = 0,
            maxWalletsCount = 0,
            isSettingAccessCodeAllowed = false,
            isSettingPasscodeAllowed = false,
            isResettingUserCodesAllowed = false,
            isLinkedTerminalEnabled = false,
            isBackupAllowed = false,
            supportedEncryptionModes = emptyList(),
            isFilesAllowed = false,
            isHDWalletAllowed = false,
            isKeysImportAllowed = false,
        ),
        userSettings = null,
        linkedTerminalStatus = CardDTO.LinkedTerminalStatus.Current,
        isAccessCodeSet = false,
        isPasscodeSet = null,
        supportedCurves = curves,
        wallets = curves.map { curve ->
            CardDTO.Wallet(
                publicKey = ByteArray(0),
                chainCode = null,
                curve = curve,
                settings = createWalletSettings(),
                totalSignedHashes = null,
                remainingSignatures = null,
                index = 0,
                hasBackup = false,
                derivedKeys = emptyMap(),
                extendedPublicKey = null,
            )
        },
        attestation = Attestation(
            cardKeyAttestation = Attestation.Status.Skipped,
            walletKeysAttestation = Attestation.Status.Skipped,
            firmwareAttestation = Attestation.Status.Skipped,
            cardUniquenessAttestation = Attestation.Status.Skipped,
        ),
        backupStatus = null,
    )

    private fun createWalletSettings(): CardWallet.Settings {
        val constructor = CardWallet.Settings::class.java.declaredConstructors[0]
        constructor.isAccessible = true

        return requireNotNull(constructor.newInstance(false) as? CardWallet.Settings)
    }

    private companion object {

        val WALLET_V3_FIRMWARE = CardDTO.FirmwareVersion(
            major = 3,
            minor = 5,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        )

        val MULTI_WHITE_FIRMWARE = CardDTO.FirmwareVersion(
            major = 4,
            minor = 12,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        )

        val WALLET_2_FIRMWARE = CardDTO.FirmwareVersion(
            major = 6,
            minor = 33,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        )
    }
}