package com.tangem.tap.domain.sdk.mocks.content

import com.tangem.common.SuccessResponse
import com.tangem.common.card.*
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.sdk.api.CreateProductWalletTaskResponse
import com.tangem.tap.domain.sdk.mocks.MockContent
import java.util.Date

object Secpk1CurveMockContent : MockContent {

    private val primaryCard = PrimaryCard(
        cardId = "CB37000000000002",
        batchId = "0045",
        cardPublicKey = byteArrayOf(4, 9, -112, -60, 29, -62, 102, -43, -38, -63, 56, 87, -76, 74, 91, -68, 127, -98, -56, 15, 12, -52, 37, -99, -104, -104, -7, -111, 6, 86, -90, 44, 87, 18, 73, 126, -104, 9, 83, -17, -11, -80, 115, 86, -90, -97, 119, -66, 114, 48, -95, -40, 10, 64, 121, -40, -15, 92, -64, 31, -21, 126, -31, 0, -40),
        linkingKey = byteArrayOf(
            2, 121, 98, 127, -70, 14, 5, -23, -76, 115, -30, -26, 111, 17, 110, 34, -100, -121,
            -57, -123, 74, 3, -91, 56, -20, 56, 50, -40, -101, 96, 82, 70, -91,
        ),
        existingWalletsCount = 3,
        isHDWalletAllowed = false,
        issuer = Card.Issuer(
            name = "TANGEM SDK",
            publicKey = byteArrayOf(4, 95, 22, -67, 29, 46, -81, -28, 99, -26, 42, 51, 90, 9, -26, -78, -69, -53, -48, 68, 82, 82, 104, -123, -53, 103, -97, -60, -46, 122, -15, -67, 34, -11, 83, -57, -34, -17, -75, 79, -45, -44, -13, 97, -47, 78, 109, -61, -15, 27, 125, 78, -95, -125, 37, 10, 96, 114, 14, -67, -7, -31, 16, -51, 38),
        ),
        manufacturer = Card.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1743759687),
            signature = byteArrayOf(18, -101, -119, 75, 82, 4, 71, -45, 4, 74, -109, 68, 100, 48, 49, 82, 35, 13, 35, -43, 14, -123, -15, -13, -94, 122, -8, 12, -73, -52, 48, 16, 112, 41, 17, 52, -51, 38, 123, 50, 97, -32, -12, 76, -102, 29, -39, 126, 55, 65, -5, -1, -32, -28, -10, -33, 77, 105, -24, 93, -30, 73, -77, 82),
        ),
        walletCurves = listOf(EllipticCurve.Secp256k1),
        firmwareVersion = FirmwareVersion(
            major = 3,
            minor = 5,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        ),
        isKeysImportAllowed = false,
        certificate = null,
    )

    override val cardDto = CardDTO(
        cardId = "CB37000000000002",
        batchId = "0045",
        cardPublicKey = byteArrayOf(4, 9, -112, -60, 29, -62, 102, -43, -38, -63, 56, 87, -76, 74, 91, -68, 127, -98, -56, 15, 12, -52, 37, -99, -104, -104, -7, -111, 6, 86, -90, 44, 87, 18, 73, 126, -104, 9, 83, -17, -11, -80, 115, 86, -90, -97, 119, -66, 114, 48, -95, -40, 10, 64, 121, -40, -15, 92, -64, 31, -21, 126, -31, 0, -40),
        firmwareVersion = CardDTO.FirmwareVersion(
            major = 3,
            minor = 5,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        ),
        manufacturer = CardDTO.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1649635200000),
            signature = byteArrayOf(-20, 98, -101, 94, -23, 73, 122, -21, 74, 76, 79, -55, -102, -62, -30, 44, -38, 118, 75, 121, -36, 118, -62, 60, -38, -63, 33, -14, -98, -69, 112, 22, 48, -43, 47, 65, -61, -56, 38, -94, -45, 44, 95, -22, 6, -31, -40, -25, 42, 38, 94, -99, 71, 98, -33, 27, -102, 30, 52, -7, 51, -27, 84, 66),
        ),
        issuer = CardDTO.Issuer(
            name = "TANGEM SDk",
            publicKey = byteArrayOf(3, 86, -25, -61, 55, 99, 41, -33, -82, 115, -120, -33, 22, -107, 103, 3, -122, 16, 60, -110, 72, 106, -121, 100, 79, -87, -27, 18, -55, -49, 78, -110, -2),
        ),
        settings = CardDTO.Settings(
            securityDelay = 15000,
            maxWalletsCount = 1,
            isSettingAccessCodeAllowed = false,
            isSettingPasscodeAllowed = true,
            isResettingUserCodesAllowed = true,
            isLinkedTerminalEnabled = true,
            isBackupAllowed = true,
            supportedEncryptionModes = listOf(EncryptionMode.Strong, EncryptionMode.Fast, EncryptionMode.None),
            isFilesAllowed = true,
            isHDWalletAllowed = false,
            isKeysImportAllowed = false,
        ),
        userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = true),
        linkedTerminalStatus = CardDTO.LinkedTerminalStatus.None,
        isAccessCodeSet = true,
        isPasscodeSet = false,
        supportedCurves = listOf(EllipticCurve.Secp256k1),
        wallets = listOf(
            CardDTO.Wallet(
                publicKey = byteArrayOf(4, -45, -28, -111, 109, -9, -113, 17, 107, -45, 43, -55, -126, 103, -70, -13, 79, -103, -59, -69, 83, -47, -37, -49, -18, -65, 55, 6, 121, 17, 54, 32, 105, 2, 25, -101, -118, 91, 109, 49, 99, -42, -113, 105, 21, 109, 66, -7, -57, 55, 25, -37, 63, 16, 22, -118, 121, -127, -13, 47, -89, -4, -38, -65, -27),
                chainCode = null,
                curve = EllipticCurve.Secp256k1,
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
            blockchain = "BTC",
            token = null,
        ),
        secondTwinPublicKey = null,
        derivedKeys = emptyMap(),
        primaryCard = null,
    )

    override val derivationTaskResponse = DerivationTaskResponse(
        entries = mapOf(
            ByteArrayKey(
                byteArrayOf(2, -74, 50, 45, -105, 119, 115, 87, -7, -110, 33, 10, -41, -9, -41, 1, 63, -73, 31, -77, 86, 66, -47, -31, 16, 32, 63, 122, 87, 94, 41, 68, 125),
            )
                to
                ExtendedPublicKeysMap(
                    mapOf(
                        DerivationPath("m/44'/0'/0'/0/0") to ExtendedPublicKey( // btc
                            publicKey = byteArrayOf(3, 45, 58, -110, -52, -51, -83, -4, -45, -118, 119, 37, 123, -17, 66, -83, 61, -106, 115, 47, 121, 66, 84, -122, -57, -45, 7, -79, 70, -13, 28, -125, -52),
                            chainCode = byteArrayOf(93, 51, 52, -66, -39, -38, 34, -84, 50, 1, -127, -20, 80, -20, -30, -72, 2, 1, -78, -81, -17, 51, -52, -25, 12, 108, 50, 89, -66, 18, 65, 70),
                            depth = 0,
                            parentFingerprint = byteArrayOf(0, 0, 0, 0),
                            childNumber = 0,
                        ),
                    ),
                ),
        ),
    )

    override val extendedPublicKey = ExtendedPublicKey(
        publicKey = byteArrayOf(3, 2, 95, 53, 40, -87, -60, 11, -8, -47, 41, 37, 100, 15, -69, 1, -122, 127, -20, -81, -32, -20, -24, 5, -28, 113, 106, -90, -59, -30, -27, -110, -110),
        chainCode = byteArrayOf(-95, -87, -95, -25, 27, 96, -57, -92, -69, -106, -45, 10, 85, 4, -92, -68, 49, -24, -28, -50, -49, -77, -20, 118, -50, -27, 104, -93, 115, -50, -46, -34),
        depth = 0,
        parentFingerprint = byteArrayOf(0, 0, 0, 0),
        childNumber = 0,
    )

    override val successResponse = SuccessResponse(cardId = "CB37000000000002")

    override val createProductWalletTaskResponse = CreateProductWalletTaskResponse(
        card = cardDto,
        derivedKeys = mapOf(
            ByteArrayKey(
                byteArrayOf(2, -74, 50, 45, -105, 119, 115, 87, -7, -110, 33, 10, -41, -9, -41, 1, 63, -73, 31, -77, 86, 66, -47, -31, 16, 32, 63, 122, 87, 94, 41, 68, 125),
            )
                to
                ExtendedPublicKeysMap(
                    mapOf(
                        DerivationPath("m/44'/0'/0'/0/0") to ExtendedPublicKey( // btc
                            publicKey = byteArrayOf(3, 45, 58, -110, -52, -51, -83, -4, -45, -118, 119, 37, 123, -17, 66, -83, 61, -106, 115, 47, 121, 66, 84, -122, -57, -45, 7, -79, 70, -13, 28, -125, -52),
                            chainCode = byteArrayOf(93, 51, 52, -66, -39, -38, 34, -84, 50, 1, -127, -20, 80, -20, -30, -72, 2, 1, -78, -81, -17, 51, -52, -25, 12, 108, 50, 89, -66, 18, 65, 70),
                            depth = 0,
                            parentFingerprint = byteArrayOf(0, 0, 0, 0),
                            childNumber = 0,
                        ),
                    ),
                ),
        ),
        primaryCard = primaryCard,
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