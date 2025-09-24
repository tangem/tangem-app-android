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

object EdCurveMockContent : MockContent {

    override val cardDto = CardDTO(
        cardId = "CB43000000000004",
        batchId = "0052",
        cardPublicKey = byteArrayOf(4, 20, -44, 84, -19, -94, 62, -88, 17, -60, -54, 115, 109, -105, 116, -4, 79, 74, 50, 55, -57, 118, -84, -17, -64, 74, 98, -104, -11, 101, 64, -85, -23, -69, 18, 96, -105, -125, -93, 87, -9, -96, -38, 32, -99, -116, 124, -34, -59, 64, -125, 96, 94, 47, -128, 61, 58, -100, 103, 84, 21, -103, 77, -91, 64),
        firmwareVersion = CardDTO.FirmwareVersion(
            major = 3,
            minor = 5,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        ),
        manufacturer = CardDTO.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1583971200),
            signature = byteArrayOf(54, 94, -56, 20, 66, -33, 103, 86, -108, 28, -101, -113, 31, 118, -89, 86, 75, 4, 21, -70, -115, 82, -98, 45, -95, 92, 45, 29, -33, 53, -16, 9, -27, 44, 44, 13, 9, -71, -85, -43, -3, 58, -82, -40, -59, -114, 7, 89, 39, -16, -60, -58, 95, -87, -54, -71, -70, -95, -59, -44, 125, -42, -66, -26),
        ),
        issuer = CardDTO.Issuer(
            name = "TANGEM SDK",
            publicKey = byteArrayOf(3, 86, -25, -61, 55, 99, 41, -33, -82, 115, -120, -33, 22, -107, 103, 3, -122, 16, 60, -110, 72, 106, -121, 100, 79, -87, -27, 18, -55, -49, 78, -110, -2),
        ),
        settings = CardDTO.Settings(
            securityDelay = 15000,
            maxWalletsCount = 1,
            isSettingAccessCodeAllowed = false,
            isSettingPasscodeAllowed = true,
            isResettingUserCodesAllowed = true,
            isLinkedTerminalEnabled = true,
            isBackupAllowed = false,
            supportedEncryptionModes = listOf(EncryptionMode.Strong, EncryptionMode.Fast, EncryptionMode.None),
            isFilesAllowed = true,
            isHDWalletAllowed = false,
            isKeysImportAllowed = false,
        ),
        userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = false),
        linkedTerminalStatus = CardDTO.LinkedTerminalStatus.None,
        isAccessCodeSet = false,
        isPasscodeSet = null,
        supportedCurves = listOf(EllipticCurve.Ed25519),
        wallets = listOf(
            CardDTO.Wallet(
                publicKey = byteArrayOf(71, -83, -58, -126, 43, 71, -27, 47, 85, -5, 120, -20, -124, -68, 14, 64, 61, 47, -95, -125, 76, -56, -98, 12, 70, 2, -18, -73, 4, -29, 62, -124),
                chainCode = null,
                curve = EllipticCurve.Ed25519,
                settings = CardWallet.Settings(isPermanent = false),
                totalSignedHashes = 0,
                remainingSignatures = 1000000,
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
        backupStatus = null,
    )

    override val scanResponse = ScanResponse(
        card = cardDto,
        productType = ProductType.Wallet,
        walletData = WalletData(
            blockchain = "XLM",
            token = null,
        ),
        secondTwinPublicKey = null,
        derivedKeys = emptyMap(),
        primaryCard = null,
    )

    override val derivationTaskResponse = DerivationTaskResponse(
        entries = emptyMap(),
    )

    override val extendedPublicKey
        get() = error("Available only for Wallet+")

    override val successResponse = SuccessResponse(cardId = "CB43000000000004")

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