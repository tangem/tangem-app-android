package com.tangem.tap.domain.sdk.mocks.content

import com.tangem.common.SuccessResponse
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.EncryptionMode
import com.tangem.common.card.FirmwareVersion
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

object Wallet2NoBackupNoWalletsMockContent : MockContent {

    private val primaryCard = PrimaryCard(
        cardId = "AF05888888880018",
        batchId = "AF05",
        cardPublicKey = byteArrayOf(3, 23, -112, -57, 109, 60, -82, -36, 45, -14, -34, -12, 10, -89, 14, 37, 38, 36, -102, 37, 93, 90, 69, -113, -117, 120, -29, 12, -125, 43, -40, -31, 5),
        linkingKey = byteArrayOf( //
            2, 121, 98, 127, -70, 14, 5, -23, -76, 115, -30, -26, 111, 17, 110, 34, -100, -121,
            -57, -123, 74, 3, -91, 56, -20, 56, 50, -40, -101, 96, 82, 70, -91,
        ),
        existingWalletsCount = 5, isHDWalletAllowed = true,
        issuer = Card.Issuer(
            name = "TANGEM SDK",
            publicKey = byteArrayOf(2, 95, 22, -67, 29, 46, -81, -28, 99, -26, 42, 51, 90, 9, -26, -78, -69, -53, -48, 68, 82, 82, 104, -123, -53, 103, -97, -60, -46, 122, -15, -67, 34),
        ),
        manufacturer = Card.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1743759687),
            signature = byteArrayOf(),
        ),
        walletCurves = listOf(
            EllipticCurve.Secp256k1,
            EllipticCurve.Ed25519,
            EllipticCurve.Bls12381G2Aug,
            EllipticCurve.Secp256r1,
            EllipticCurve.Ed25519Slip0010,
            EllipticCurve.Bls12381G2,
            EllipticCurve.Bls12381G2Pop,
            EllipticCurve.Bip0340,
        ),
        firmwareVersion = FirmwareVersion(
            major = 6,
            minor = 33,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        ),
        isKeysImportAllowed = false,
        certificate = null,
    )

    override val cardDto = CardDTO(
        cardId = "AF05888888880018",
        batchId = "AF05",
        cardPublicKey = byteArrayOf(3, 23, -112, -57, 109, 60, -82, -36, 45, -14, -34, -12, 10, -89, 14, 37, 38, 36, -102, 37, 93, 90, 69, -113, -117, 120, -29, 12, -125, 43, -40, -31, 5),
        firmwareVersion = CardDTO.FirmwareVersion(
            major = 6,
            minor = 33,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        ),
        manufacturer = CardDTO.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1698094800000),
            signature = byteArrayOf(51, -12, 14, -56, 7, -39, 5, 63, 59, 24, 102, 99, -126, 124, -127, -108, -118, 71, -19, -71, 4, -47, -121, -46, 49, -51, -31, 100, -56, -15, 96, -37, 25, 82, 94, -88, 48, 98, -105, -97, -40, 41, 27, 116, 65, 26, 78, -85, 66, -94, -92, 15, 50, -2, 7, -69, 41, 56, -75, 59, 86, 68, -38, -3),
        ),
        issuer = CardDTO.Issuer(
            name = "TANGEM SDK",
            publicKey = byteArrayOf(2, 95, 22, -67, 29, 46, -81, -28, 99, -26, 42, 51, 90, 9, -26, -78, -69, -53, -48, 68, 82, 82, 104, -123, -53, 103, -97, -60, -46, 122, -15, -67, 34),
        ),
        settings = CardDTO.Settings(
            securityDelay = 15000,
            maxWalletsCount = 20,
            isSettingAccessCodeAllowed = true,
            isSettingPasscodeAllowed = true,
            isResettingUserCodesAllowed = false,
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
        supportedCurves = listOf(
            EllipticCurve.Secp256k1,
            EllipticCurve.Ed25519,
            EllipticCurve.Bls12381G2Aug,
            EllipticCurve.Secp256r1,
            EllipticCurve.Ed25519Slip0010,
            EllipticCurve.Bls12381G2,
            EllipticCurve.Bls12381G2Pop,
            EllipticCurve.Bip0340,
        ),
        wallets = emptyList(),
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
        productType = ProductType.Wallet2,
        walletData = null,
        secondTwinPublicKey = null,
        derivedKeys = emptyMap(),
        primaryCard = null,
    )

    override val derivationTaskResponse = DerivationTaskResponse(
        entries = mapOf(
            ByteArrayKey(
                byteArrayOf(2, -109, 28, -27, -124, -58, -97, -61, 43, -84, 90, -9, -5, 4, 90, 17, 112, -125, -108, 44, 19, -79, -60, -23, 34, -20, -20, 61, 84, 113, 120, -90, -5),
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
                        DerivationPath("m/44'/60'/0'/0/0") to ExtendedPublicKey( // eth
                            publicKey = byteArrayOf(2, 34, 6, 119, -106, 5, -119, 111, -22, 8, 23, -108, -72, -56, 6, 77, -17, -61, -101, -85, 16, 28, 18, 3, -3, -89, -81, -108, 48, -7, -86, -82, -67),
                            chainCode = byteArrayOf(-75, 55, 107, -106, -37, -81, -15, 72, -102, 94, 55, -39, 9, -112, 1, 90, -50, 103, 53, 120, -92, -36, -85, -39, -65, 1, 88, 46, 92, 104, -13, -109),
                            depth = 0,
                            parentFingerprint = byteArrayOf(0, 0, 0, 0),
                            childNumber = 0,
                        ),
                        DerivationPath("m/44'/145'/0'/0/0") to ExtendedPublicKey( // bch
                            publicKey = byteArrayOf(2, 38, -6, 92, -37, -91, -59, -108, -18, -119, -55, 41, 38, -33, 44, 59, 24, -79, -14, -38, -10, -123, 106, 56, 39, 8, 112, 29, -41, 99, 70, -104, -121),
                            chainCode = byteArrayOf(105, -21, -61, -50, 68, -89, 119, 53, -96, -40, 119, 77, -122, 121, 16, 40, -50, -48, -105, -101, -74, -7, -94, -59, -90, 96, 59, 99, 43, -91, 115, -29),
                            depth = 0,
                            parentFingerprint = byteArrayOf(0, 0, 0, 0),
                            childNumber = 0,
                        ),
                        DerivationPath("m/44'/3'/0'/0/0") to ExtendedPublicKey( // doge
                            publicKey = byteArrayOf(3, -25, -24, -97, -124, 24, -89, 44, 75, 123, 92, -86, -73, -93, 25, -90, -89, -95, 88, 3, 107, 37, -1, -85, -32, -57, -123, -41, 108, -9, -96, 77, -124),
                            chainCode = byteArrayOf(119, 3, 41, 112, 71, 54, 72, 30, 39, 25, 25, -104, 92, 46, -109, 63, 93, 67, 43, -102, -87, 39, -95, 106, 45, 67, 109, -29, -35, 10, -107, 104),
                            depth = 0,
                            parentFingerprint = byteArrayOf(0, 0, 0, 0),
                            childNumber = 0,
                        ),
                    ),
                ),
        ),
    )

    override val extendedPublicKey = ExtendedPublicKey(
        publicKey = byteArrayOf(2, -93, -36, -105, 121, -52, -30, -43, -67, -7, -31, -26, -35, 25, 99, 25, -20, 118, -20, -125, -89, 12, -101, -86, 74, -91, 23, -24, 93, -86, 20, -53, -8),
        chainCode = byteArrayOf(32, 60, 63, -96, 97, 58, 121, 108, 75, 59, 63, -113, 60, -49, 47, 33, 15, -65, -69, 45, -7, -26, 65, -5, -91, -55, -42, -102, -127, -104, -111, 96),
        depth = 0,
        parentFingerprint = byteArrayOf(0, 0, 0, 0),
        childNumber = 0,
    )

    override val successResponse = SuccessResponse(cardId = "AF05888888880018")

    override val createProductWalletTaskResponse = CreateProductWalletTaskResponse(
        card = cardDto,
        derivedKeys = mapOf(
            ByteArrayKey(
                byteArrayOf(3, 23, -112, -57, 109, 60, -82, -36, 45, -14, -34, -12, 10, -89, 14, 37, 38, 36, -102, 37, 93, 90, 69, -113, -117, 120, -29, 12, -125, 43, -40, -31, 5),
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
                        DerivationPath("m/44'/60'/0'/0/0") to ExtendedPublicKey( // eth
                            publicKey = byteArrayOf(2, 34, 6, 119, -106, 5, -119, 111, -22, 8, 23, -108, -72, -56, 6, 77, -17, -61, -101, -85, 16, 28, 18, 3, -3, -89, -81, -108, 48, -7, -86, -82, -67),
                            chainCode = byteArrayOf(-75, 55, 107, -106, -37, -81, -15, 72, -102, 94, 55, -39, 9, -112, 1, 90, -50, 103, 53, 120, -92, -36, -85, -39, -65, 1, 88, 46, 92, 104, -13, -109),
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
        get() = TODO("Not yet implemented")

    override val createFirstTwinResponse: CreateWalletResponse
        get() = error("Available only for Twin")

    override val createSecondTwinResponse: CreateWalletResponse
        get() = error("Available only for Twin")

    override val finalizeTwinResponse: ScanResponse
        get() = error("Available only for Twin")
}