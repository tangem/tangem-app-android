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

object Wallet2WithSeedPhraseMockContent : MockContent {

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
        wallets = listOf(
            CardDTO.Wallet(
                publicKey = byteArrayOf(3, 17, 59, -102, -56, 66, 10, 36, 97, -106, -92, -117, -105, -88, -2, -3, -95, -75, -54, -2, 57, 27, -90, -19, 82, 103, -12, -52, -126, -105, -120, -55, -2),
                chainCode = byteArrayOf(-34, -28, -28, -12, 80, -109, -90, -31, -74, 36, -78, -21, 39, -125, -39, -91, 63, 40, -26, 3, 66, 27, -62, -55, -97, 52, -65, -11, 26, -80, 33, -45),
                curve = EllipticCurve.Secp256k1,
                settings = CardWallet.Settings(isPermanent = false),
                totalSignedHashes = 0,
                remainingSignatures = null,
                index = 0,
                hasBackup = true,
                derivedKeys = mapOf(
                    DerivationPath("m/44'/60'/0'/0/0") to ExtendedPublicKey(
                        publicKey = byteArrayOf(2, 55, -114, -94, -73, -61, -50, 51, -115, 55, -79, 63, -96, -44, -64, -24, -36, -122, -123, -38, 81, -15, -127, -97, -42, 72, -85, -62, -98, 46, 119, 16, -55),
                        chainCode = byteArrayOf(31, 17, 71, -28, -29, 17, 72, -29, -98, 112, 31, -8, 72, -75, 4, -11, 60, -100, 9, 35, 58, -42, -38, 96, -71, -68, 24, -119, -43, -18, -122, 72),
                    ),
                    DerivationPath("m/84'/0'/0'/0/0") to ExtendedPublicKey(
                        publicKey = byteArrayOf(2, -26, 103, 96, 112, 127, -125, 0, 7, 53, 30, -12, -82, 45, 14, 107, -9, 126, 75, 104, -67, -49, 35, -12, -82, -90, 101, -101, 125, -76, 88, -54, 99),
                        chainCode = byteArrayOf(-42, 36, 97, 65, -64, -113, 76, -91, -9, 11, 89, 123, -9, -3, 21, 103, -113, -60, 48, -31, -34, 108, 111, -38, -110, -80, 109, 17, -29, 2, 45, -71),
                    ),
                    DerivationPath("m/44'/1'/0'/0/0") to ExtendedPublicKey(
                        publicKey = byteArrayOf(2, -96, -3, -83, 101, 63, -25, -125, -4, -65, -42, -56, 24, -52, 118, -11, -104, -105, 40, -59, 20, -109, -97, 29, -95, -6, -80, 2, 67, 103, -80, -22, -94),
                        chainCode = byteArrayOf(-125, 27, -91, 38, -66, 109, -92, 16, -37, 93, 107, -29, -128, -1, 115, -64, 108, -63, 17, 27, 58, 78, -2, 39, 88, -39, 44, 89, 32, -38, -16, -38),
                    ),
                ),
                extendedPublicKey = ExtendedPublicKey(
                    publicKey = byteArrayOf(-109, 42, -80, -115, -12, 44, 48, -118, -89, 10, 21, 59, 98, -110, -86, -14, 123, 105, -30, -49, -40, -4, -7, 32, 60, 67, -88, -52, -96, -10, -14, 123),
                    chainCode = byteArrayOf(-101, -10, -92, 72, 19, 121, -38, 105, -55, -82, -68, 13, -6, 17, 66, -10, -75, 58, 119, -127, 74, 68, 82, 25, 45, -110, 3, 3, 108, -97, -51, -127),
                ),
                isImported = true,
            ),
            CardDTO.Wallet(
                publicKey = byteArrayOf(-109, 42, -80, -115, -12, 44, 48, -118, -89, 10, 21, 59, 98, -110, -86, -14, 123, 105, -30, -49, -40, -4, -7, 32, 60, 67, -88, -52, -96, -10, -14, 123),
                chainCode = byteArrayOf(-101, -10, -92, 72, 19, 121, -38, 105, -55, -82, -68, 13, -6, 17, 66, -10, -75, 58, 119, -127, 74, 68, 82, 25, 45, -110, 3, 3, 108, -97, -51, -127),
                curve = EllipticCurve.Ed25519,
                settings = CardWallet.Settings(isPermanent = false),
                totalSignedHashes = 0,
                remainingSignatures = null,
                index = 1,
                hasBackup = true,
                derivedKeys = emptyMap(),
                extendedPublicKey = ExtendedPublicKey(
                    publicKey = byteArrayOf(-64, 18, -128, 121, -89, -37, -99, 44, -125, -72, -111, -79, 7, 85, 40, 67, -39, 117, 123, 11, 105, -6, -5, -79, 19, -10, -29, 20, -14, -40, 5, 90),
                    chainCode = byteArrayOf(33, -97, 53, -112, 61, 112, -24, 74, -87, -85, -124, -4, 103, -94, -97, 76, -41, -27, 118, 33, 55, 121, -17, -52, 60, 122, 27, 25, 29, -76, 78, 11),
                ),
                isImported = true,
            ),
            CardDTO.Wallet(
                publicKey = byteArrayOf(-118, -77, -109, 8, -119, 101, -91, 46, -44, 15, 52, -64, -92, 5, -102, 126, 52, 121, 63, -76, -54, 80, -82, -5, 31, -35, 105, -119, -96, -54, 38, 2, -6, 109, 117, -26, -47, -20, 108, 106, -69, 72, -37, -117, -30, -9, 104, -123),
                chainCode = null,
                curve = EllipticCurve.Bls12381G2Aug,
                settings = CardWallet.Settings(isPermanent = false),
                totalSignedHashes = 1,
                remainingSignatures = null,
                index = 2,
                hasBackup = true,
                derivedKeys = emptyMap(),
                extendedPublicKey = null,
                isImported = true,
            ),
            CardDTO.Wallet(
                publicKey = byteArrayOf(17, -78, 53, 101, 96, -110, -10, -48, -84, 88, -64, 58, -26, -13, -2, -6, -25, 23, -79, -45, 58, 77, 52, -75, -123, 121, 32, 92, 84, -74, -111, -8),
                chainCode = byteArrayOf(-110, 73, -124, -65, -1, -70, 64, -89, 95, -74, 15, 46, -110, 87, 15, -61, 120, -51, 111, 111, -45, 37, 123, -114, 65, -116, 21, 39, -77, 86, -3, -26),
                curve = EllipticCurve.Bip0340,
                settings = CardWallet.Settings(isPermanent = false),
                totalSignedHashes = 0,
                remainingSignatures = null,
                index = 3,
                hasBackup = true,
                derivedKeys = emptyMap(),
                extendedPublicKey = ExtendedPublicKey(
                    publicKey = byteArrayOf(115, 14, 11, 0, -93, 81, -103, -95, -75, -84, 18, -120, -31, 76, -83, -81, 91, 25, -75, 36, -99, -53, -25, -15, -1, -57, 14, -39, 98, -116, -63, -123),
                    chainCode = byteArrayOf(23, 5, 38, -48, 67, -42, -31, -21, 89, 11, 22, -28, 44, -19, -115, -78, 123, -27, 57, 57, -24, -86, 55, 15, 104, 114, -36, 80, 81, -108, -41, 112),
                ),
                isImported = true,
            ),
            CardDTO.Wallet(
                publicKey = byteArrayOf(-52, -92, 64, 108, -8, -100, 87, -86, -60, 18, -18, -81, 114, -84, 76, -24, 84, 52, -34, 79, -30, -66, -112, -55, -35, -119, 127, -109, 35, 18, -29, -25),
                chainCode = byteArrayOf(94, -33, -94, -63, 40, -20, -26, 71, 111, 9, 87, -36, -42, -33, 40, -53, 85, 31, -36, -29, 65, -121, 2, -23, -119, -51, 114, -17, -39, -74, 23, -97),
                curve = EllipticCurve.Ed25519Slip0010,
                settings = CardWallet.Settings(isPermanent = false),
                totalSignedHashes = 0,
                remainingSignatures = null,
                index = 4,
                hasBackup = true,
                derivedKeys = emptyMap(),
                extendedPublicKey = ExtendedPublicKey(
                    publicKey = byteArrayOf(-43, 1, -81, -47, -8, -103, -66, 42, 37, -7, 65, 54, 57, -24, 127, -89, 69, -112, 42, -46, -128, 36, -117, -28, 30, -48, 37, 52, 93, -47, 92, -47),
                    chainCode = byteArrayOf(4, -97, 81, -37, 76, -67, -87, -4, -82, -36, -45, -28, -117, -59, -62, 93, -73, 50, 65, -91, -83, 25, -95, 89, -64, -40, 113, 28, 59, 113, -99, 89),
                ),
                isImported = true,
            ),
        ),
        attestation = Attestation(
            cardKeyAttestation = Attestation.Status.Verified,
            walletKeysAttestation = Attestation.Status.Skipped,
            firmwareAttestation = Attestation.Status.Skipped,
            cardUniquenessAttestation = Attestation.Status.Skipped,
        ),
        backupStatus = CardDTO.BackupStatus.Active(1),
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