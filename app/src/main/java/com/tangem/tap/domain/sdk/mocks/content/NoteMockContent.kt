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

object NoteMockContent : MockContent {

    override val cardDto = CardDTO(
        cardId = "AB04000000010905",
        batchId = "AB04",
        cardPublicKey = byteArrayOf(2, 102, 3, -106, -14, -87, -118, 120, 10, 93, 17, 55, 26, -44, 5, 115, 88, 35, 49, -88, -69, 116, 0, -72, -27, 57, 50, -55, 80, -16, 39, -70, 119),
        firmwareVersion = CardDTO.FirmwareVersion(
            major = 4,
            minor = 39,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        ),
        manufacturer = CardDTO.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1649635200000),
            signature = byteArrayOf(-2, -45, 100, -79, 7, -120, 49, 74, 126, -3, -75, 54, -36, -1, 19, -83, 47, -82, 52, -43, -119, 75, 58, 97, 50, 37, 103, -6, -2, 28, 120, 103, -38, -95, 7, -126, -3, -23, -22, -30, -24, -126, 3, 70, 7, -20, -6, -49, 55, -93, 26, 57, -71, 12, -20, 41, -105, -75, 82, 116, 12, -75, -22, 55),
        ),
        issuer = CardDTO.Issuer(
            name = "TANGEM AG",
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
        userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = false),
        linkedTerminalStatus = CardDTO.LinkedTerminalStatus.None,
        isAccessCodeSet = false,
        isPasscodeSet = false,
        supportedCurves = listOf(
            EllipticCurve.Secp256k1,
            EllipticCurve.Ed25519,
            EllipticCurve.Secp256r1,
        ),
        wallets = listOf(
            CardDTO.Wallet(
                publicKey = byteArrayOf(2, -27, -117, 23, 68, -3, 21, -109, 18, -67, -107, -42, -44, -16, -127, -53, 46, -109, -46, -51, 89, 119, 79, 111, 78, 62, -125, 72, 109, 8, 45, 59, 117),
                chainCode = byteArrayOf(),
                curve = EllipticCurve.Secp256k1,
                settings = CardWallet.Settings(isPermanent = false),
                totalSignedHashes = 3,
                remainingSignatures = null,
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
        productType = ProductType.Note,
        walletData = WalletData(blockchain = "DOGE", token = null),
        secondTwinPublicKey = null,
        derivedKeys = emptyMap(),
        primaryCard = null,
    )

    override val derivationTaskResponse = DerivationTaskResponse(
        entries = emptyMap(),
    )

    override val extendedPublicKey
        get() = error("Available only for wallet+?")

    override val successResponse = SuccessResponse(cardId = "AB04000000010905")

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