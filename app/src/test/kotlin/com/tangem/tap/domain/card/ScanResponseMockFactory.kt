package com.tangem.tap.domain.card

import com.tangem.common.card.CardWallet
import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.common.configs.*
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.KeyWalletPublicKey
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import java.util.Date

/**
[REDACTED_AUTHOR]
 */
object ScanResponseMockFactory {

    private val genericFirmwareVersion = CardDTO.FirmwareVersion(
        major = 3,
        minor = 29,
        patch = 0,
        type = FirmwareVersion.FirmwareType.Release,
    )

    private val walletFirmwareVersion = CardDTO.FirmwareVersion(
        major = 4,
        minor = 0,
        patch = 0,
        type = FirmwareVersion.FirmwareType.Release,
    )

    private val wallet2FirmwareVersion = CardDTO.FirmwareVersion(
        major = 6,
        minor = 33,
        patch = 0,
        type = FirmwareVersion.FirmwareType.Release,
    )

    fun create(cardConfig: CardConfig, derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap>): ScanResponse {
        return ScanResponse(
            card = CardDTO(
                cardId = "NEVER-MIND",
                batchId = "NEVER-MIND",
                cardPublicKey = ByteArray(0),
                firmwareVersion = when (cardConfig) {
                    is GenericCardConfig -> genericFirmwareVersion
                    MultiWalletCardConfig -> walletFirmwareVersion
                    Wallet2CardConfig -> wallet2FirmwareVersion
                    EdSingleCurrencyCardConfig -> genericFirmwareVersion
                },
                manufacturer = CardDTO.Manufacturer(name = "NEVER-MIND", manufactureDate = Date(), signature = null),
                issuer = CardDTO.Issuer(name = "NEVER-MIND", publicKey = ByteArray(0)),
                settings = CardDTO.Settings(
                    securityDelay = 0,
                    maxWalletsCount = 0,
                    isSettingAccessCodeAllowed = false,
                    isSettingPasscodeAllowed = false,
                    isResettingUserCodesAllowed = false,
                    isLinkedTerminalEnabled = false,
                    isBackupAllowed = cardConfig is MultiWalletCardConfig,
                    supportedEncryptionModes = listOf(),
                    isFilesAllowed = true,
                    isHDWalletAllowed = cardConfig is MultiWalletCardConfig,
                    isKeysImportAllowed = true,
                ),
                userSettings = null,
                linkedTerminalStatus = CardDTO.LinkedTerminalStatus.Current,
                isAccessCodeSet = false,
                isPasscodeSet = null,
                supportedCurves = listOf(),
                wallets = cardConfig.mandatoryCurves.map {
                    CardDTO.Wallet(
                        CardWallet(
                            publicKey = it.name.toByteArray(), // IMPORTANT: public key must equal to curve name
                            chainCode = null,
                            curve = it,
                            settings = createSettings(),
                            totalSignedHashes = null,
                            remainingSignatures = null,
                            index = 0,
                            isImported = true,
                            hasBackup = true,
                            derivedKeys = mapOf(),
                        ),
                    )
                },
                attestation = Attestation(
                    cardKeyAttestation = Attestation.Status.Skipped,
                    walletKeysAttestation = Attestation.Status.Skipped,
                    firmwareAttestation = Attestation.Status.Skipped,
                    cardUniquenessAttestation = Attestation.Status.Skipped,
                ),
                backupStatus = null,
            ),
            productType = ProductType.Wallet2,
            walletData = null,
            derivedKeys = derivedKeys,
        )
    }

    private fun createSettings(): CardWallet.Settings {
        val constructor = CardWallet.Settings::class.java.declaredConstructors[0]
        constructor.isAccessible = true
        return constructor.newInstance(false) as CardWallet.Settings
    }
}