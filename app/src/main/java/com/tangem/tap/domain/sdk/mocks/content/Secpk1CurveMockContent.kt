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
        cardId = "AC07000000035437",
        batchId = "0045",
        cardPublicKey = byteArrayOf(2, 48, 4, 54, -34, -33, 22, 53, -92, 8, 72, 50, 69, 1, 105, -53, 9, -126, -23, -61, -62, -85, -24, -112, -32, 76, -88, -66, -25, -83, -70, -95, 90),
        linkingKey = byteArrayOf(
            2, 121, 98, 127, -70, 14, 5, -23, -76, 115, -30, -26, 111, 17, 110, 34, -100, -121,
            -57, -123, 74, 3, -91, 56, -20, 56, 50, -40, -101, 96, 82, 70, -91,
        ),
        existingWalletsCount = 3, isHDWalletAllowed = true,
        issuer = Card.Issuer(
            name = "TANGEM SDK",
            publicKey = byteArrayOf(2, 95, 22, -67, 29, 46, -81, -28, 99, -26, 42, 51, 90, 9, -26, -78, -69, -53, -48, 68, 82, 82, 104, -123, -53, 103, -97, -60, -46, 122, -15, -67, 34),
        ),
        manufacturer = Card.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1743759687),
            signature = byteArrayOf(44, 8, 116, 111, -57, 20, -93, 101, -104, 97, -104, -15, 58, -8, 41, -53, 95, 99, 107, 47, -29, -118, 41, 64, 22, 94, 33, -81, 114, -47, 10, -16, 97, 14, 94, -36, -82, -108, 74, -9, 35, -38, 66, 67, -116, -55, 65, -30, -58, -33, 31, 120, -45, 42, -3, -120, -74, -97, 97, -102, -13, -28, 29, -41),
        ),
        walletCurves = listOf(EllipticCurve.Secp256k1),
        firmwareVersion = FirmwareVersion(
            major = 3,
            minor = 0,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        ),
        isKeysImportAllowed = false,
        certificate = null,
    )

    override val cardDto = CardDTO(
        cardId = "AC07000000035437",
        batchId = "0045",
        cardPublicKey = byteArrayOf(2, 48, 4, 54, -34, -33, 22, 53, -92, 8, 72, 50, 69, 1, 105, -53, 9, -126, -23, -61, -62, -85, -24, -112, -32, 76, -88, -66, -25, -83, -70, -95, 90),
        firmwareVersion = CardDTO.FirmwareVersion(
            major = 3,
            minor = 0,
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
            maxWalletsCount = 20,
            isSettingAccessCodeAllowed = true,
            isSettingPasscodeAllowed = false,
            isResettingUserCodesAllowed = true,
            isLinkedTerminalEnabled = true,
            isBackupAllowed = true,
            supportedEncryptionModes = listOf(EncryptionMode.Strong, EncryptionMode.Fast, EncryptionMode.None),
            isFilesAllowed = true,
            isHDWalletAllowed = true,
            isKeysImportAllowed = true,
        ),
        userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = true),
        linkedTerminalStatus = CardDTO.LinkedTerminalStatus.None,
        isAccessCodeSet = true,
        isPasscodeSet = false,
        supportedCurves = listOf(EllipticCurve.Secp256k1),
        wallets = listOf(
            CardDTO.Wallet(
                publicKey = byteArrayOf(2, -74, 50, 45, -105, 119, 115, 87, -7, -110, 33, 10, -41, -9, -41, 1, 63, -73, 31, -77, 86, 66, -47, -31, 16, 32, 63, 122, 87, 94, 41, 68, 125),
                chainCode = byteArrayOf(74, -90, -85, -117, -116, -78, 98, 63, 83, 12, 43, -69, 43, -54, -119, -62, 80, 107, -127, 53, 73, 108, 114, -102, 81, -123, 68, 92, 65, -25, 107, 86),
                curve = EllipticCurve.Secp256k1,
                settings = CardWallet.Settings(isPermanent = false),
                totalSignedHashes = 0,
                remainingSignatures = null,
                index = 0,
                hasBackup = false,
                derivedKeys = mapOf(
                    DerivationPath("m/44'/0'/0'/0/0") to ExtendedPublicKey(
                        publicKey = byteArrayOf(3, 45, 58, -110, -52, -51, -83, -4, -45, -118, 119, 37, 123, -17, 66, -83, 61, -106, 115, 47, 121, 66, 84, -122, -57, -45, 7, -79, 70, -13, 28, -125, -52),
                        chainCode = byteArrayOf(93, 51, 52, -66, -39, -38, 34, -84, 50, 1, -127, -20, 80, -20, -30, -72, 2, 1, -78, -81, -17, 51, -52, -25, 12, 108, 50, 89, -66, 18, 65, 70),
                    ),
                ),
                extendedPublicKey = ExtendedPublicKey(
                    publicKey = byteArrayOf(2, -74, 50, 45, -105, 119, 115, 87, -7, -110, 33, 10, -41, -9, -41, 1, 63, -73, 31, -77, 86, 66, -47, -31, 16, 32, 63, 122, 87, 94, 41, 68, 125),
                    chainCode = byteArrayOf(80, 13, -8, -108, -35, 116, -92, 125, -65, -28, 85, 72, -113, 59, 83, 13, 5, -83, -102, 123, 124, -22, 94, 108, -71, 95, 65, -2, 38, 38, -108, 14),
                ),
                isImported = false,
            ),
            CardDTO.Wallet(
                publicKey = byteArrayOf(-123, -48, -77, -113, -63, -124, -18, 36, -14, -127, 104, 89, 48, 103, 69, -12, 107, -103, 64, -2, 97, 126, -78, -99, -9, -72, 94, -68, 30, 43, 38, 42),
                chainCode = byteArrayOf(104, 109, 56, -52, 125, -119, -49, -51, -46, -122, -111, 75, 51, 103, 15, 25, -101, 72, -101, 101, -128, -2, -51, 3, 74, 62, -49, -6, -95, 91, -104, -58),
                curve = EllipticCurve.Ed25519,
                settings = CardWallet.Settings(isPermanent = false),
                totalSignedHashes = 0,
                remainingSignatures = null,
                index = 1,
                hasBackup = false,
                derivedKeys = emptyMap(),
                extendedPublicKey = ExtendedPublicKey(
                    publicKey = byteArrayOf(-65, -53, -62, -12, -57, -32, -38, -9, -128, -52, -83, -61, 73, 39, 41, 15, -74, -97, 38, 52, -101, 63, 74, -56, -20, 15, 57, -127, 114, -93, -17, -109),
                    chainCode = byteArrayOf(-81, 15, 125, 28, -115, -22, 87, 81, 87, -123, -25, -74, 86, 2, 1, 110, -115, 65, -110, 63, -64, 83, -93, -97, -104, 123, 12, -26, 94, 27, 84, -6),
                ),
                isImported = false,
            ),
        ),
        attestation = Attestation(
            cardKeyAttestation = Attestation.Status.Verified,
            walletKeysAttestation = Attestation.Status.Skipped,
            firmwareAttestation = Attestation.Status.Skipped,
            cardUniquenessAttestation = Attestation.Status.Skipped,
        ),
        backupStatus = CardDTO.BackupStatus.Active(cardCount = 1),
    )

    override val scanResponse = ScanResponse(
        card = cardDto,
        productType = ProductType.Wallet,
        walletData = null,
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

    override val successResponse = SuccessResponse(cardId = "AC07000000035437")

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