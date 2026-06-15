package com.tangem.tap.domain.sdk.mocks.content

import com.tangem.common.SuccessResponse
import com.tangem.common.card.*
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.sdk.api.CreateProductWalletTaskResponse
import com.tangem.tap.domain.sdk.mocks.MockContent
import java.util.Date

// Start2Coin (S2C): issuer "Start2Coin" trips isStart2Coin → single currency, WalletConnect hidden.
object S2CMockContent : MockContent {

    override val cardDto = CardDTO(
        cardId = "1198724260000000",
        batchId = "CD04",
        cardPublicKey = byteArrayOf(2, 102, 3, -106, -14, -87, -118, 120, 10, 93, 17, 55, 26, -44, 5, 115, 88, 35, 49, -88, -69, 116, 0, -72, -27, 57, 50, -55, 80, -16, 39, -70, 119),
        firmwareVersion = CardDTO.FirmwareVersion(
            major = 4,
            minor = 52,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        ),
        manufacturer = CardDTO.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1671494400000),
            signature = byteArrayOf(),
        ),
        issuer = CardDTO.Issuer(
            name = "Start2Coin",
            publicKey = byteArrayOf(3, 86, -25, -61, 55, 99, 41, -33, -82, 115, -120, -33, 22, -107, 103, 3, -122, 16, 60, -110, 72, 106, -121, 100, 79, -87, -27, 18, -55, -49, 78, -110, -2),
        ),
        settings = CardDTO.Settings(
            securityDelay = 15000,
            maxWalletsCount = 1,
            isSettingAccessCodeAllowed = false,
            isSettingPasscodeAllowed = false,
            isResettingUserCodesAllowed = true,
            isLinkedTerminalEnabled = true,
            isBackupAllowed = false,
            supportedEncryptionModes = listOf(EncryptionMode.Strong, EncryptionMode.Fast, EncryptionMode.None),
            isFilesAllowed = false,
            isHDWalletAllowed = false,
            isKeysImportAllowed = false,
        ),
        userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = true),
        linkedTerminalStatus = CardDTO.LinkedTerminalStatus.None,
        isAccessCodeSet = false,
        isPasscodeSet = false,
        supportedCurves = listOf(EllipticCurve.Secp256k1),
        wallets = listOf(
            CardDTO.Wallet(
                publicKey = byteArrayOf(2, 106, 7, -77, -109, 39, 3, 80, 99, 31, 50, -40, -113, -81, -76, -21, 123, -60, 0, -121, -56, 126, 2, 123, 111, 80, 47, -37, 40, 119, -22, 33, 32),
                chainCode = byteArrayOf(),
                curve = EllipticCurve.Secp256k1,
                settings = CardWallet.Settings(isPermanent = true),
                totalSignedHashes = 1,
                remainingSignatures = 999999,
                index = 0,
                hasBackup = false,
                derivedKeys = emptyMap(),
                extendedPublicKey = null,
                isImported = false,
            ),
        ),
        attestation = Attestation(
            cardKeyAttestation = Attestation.Status.Verified,
            walletKeysAttestation = Attestation.Status.Skipped,
            firmwareAttestation = Attestation.Status.Skipped,
            cardUniquenessAttestation = Attestation.Status.Skipped,
        ),
        backupStatus = CardDTO.BackupStatus.NoBackup,
    )

    override val scanResponse = ScanResponse(
        card = cardDto,
        productType = ProductType.Start2Coin,
        walletData = WalletData(blockchain = "BTC", token = null),
        secondTwinPublicKey = null,
        derivedKeys = emptyMap(),
        primaryCard = null,
    )

    override val derivationTaskResponse = DerivationTaskResponse(entries = emptyMap())

    override val extendedPublicKey
        get() = error("Available only for wallet+?")

    override val successResponse = SuccessResponse(cardId = "1198724260000000")

    override val createProductWalletTaskResponse = CreateProductWalletTaskResponse(
        card = cardDto,
        derivedKeys = emptyMap(),
        primaryCard = null,
    )

    override val importWalletResponse: CreateProductWalletTaskResponse
        get() = error("Available only for Wallet 2")

    override val createFirstTwinResponse: CreateWalletResponse
        get() = error("Available only for Twin")

    override val createSecondTwinResponse: CreateWalletResponse
        get() = error("Available only for Twin")

    override val finalizeTwinResponse: ScanResponse
        get() = error("Available only for Twin")
}