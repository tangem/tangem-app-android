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

object EdCurveMockContent : MockContent {

    private val primaryCard = PrimaryCard(
        cardId = "CB43000000000004",
        batchId = "0052",
        cardPublicKey = byteArrayOf(4, 20, -44, 84, -19, -94, 62, -88, 17, -60, -54, 115, 109, -105, 116, -4, 79, 74, 50, 55, -57, 118, -84, -17, -64, 74, 98, -104, -11, 101, 64, -85, -23, -69, 18, 96, -105, -125, -93, 87, -9, -96, -38, 32, -99, -116, 124, -34, -59, 64, -125, 96, 94, 47, -128, 61, 58, -100, 103, 84, 21, -103, 77, -91, 64),
        linkingKey = byteArrayOf(
            2, 121, 98, 127, -70, 14, 5, -23, -76, 115, -30, -26, 111, 17, 110, 34, -100, -121,
            -57, -123, 74, 3, -91, 56, -20, 56, 50, -40, -101, 96, 82, 70, -91,
        ),
        existingWalletsCount = 3, isHDWalletAllowed = true,
        issuer = Card.Issuer(
            name = "TANGEM SDK",
            publicKey = byteArrayOf(3, 86, -25, -61, 55, 99, 41, -33, -82, 115, -120, -33, 22, -107, 103, 3, -122, 16, 60, -110, 72, 106, -121, 100, 79, -87, -27, 18, -55, -49, 78, -110, -2),
        ),
        manufacturer = Card.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1583971200),
            signature = byteArrayOf(54, 94, -56, 20, 66, -33, 103, 86, -108, 28, -101, -113, 31, 118, -89, 86, 75, 4, 21, -70, -115, 82, -98, 45, -95, 92, 45, 29, -33, 53, -16, 9, -27, 44, 44, 13, 9, -71, -85, -43, -3, 58, -82, -40, -59, -114, 7, 89, 39, -16, -60, -58, 95, -87, -54, -71, -70, -95, -59, -44, 125, -42, -66, -26),
        ),
        walletCurves = listOf(EllipticCurve.Ed25519),
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
        cardId = "CB43000000000004",
        batchId = "0052",
        cardPublicKey = byteArrayOf(2, -120, -3, -32, -122, -127, -120, -104, 59, 72, 76, 114, 94, 75, -37, -55, 55, 99, 66, 123, 85, -87, 80, 106, 105, -116, 87, -83, -12, 70, 108, -68, -39),
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
            isSettingPasscodeAllowed = false,
            isResettingUserCodesAllowed = true,
            isLinkedTerminalEnabled = true,
            isBackupAllowed = false,
            supportedEncryptionModes = listOf(EncryptionMode.Strong, EncryptionMode.Fast, EncryptionMode.None),
            isFilesAllowed = true,
            isHDWalletAllowed = true,
            isKeysImportAllowed = false,
        ),
        userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = false),
        linkedTerminalStatus = CardDTO.LinkedTerminalStatus.None,
        isAccessCodeSet = false,
        isPasscodeSet = false,
        supportedCurves = listOf(EllipticCurve.Ed25519),
        wallets = listOf(
            CardDTO.Wallet(
                publicKey = byteArrayOf(71, -83, -58, -126, 43, 71, -27, 47, 85, -5, 120, -20, -124, -68, 14, 64, 61, 47, -95, -125, 76, -56, -98, 12, 70, 2, -18, -73, 4, -29, 62, -124),
                chainCode = byteArrayOf(80, 13, -8, -108, -35, 116, -92, 125, -65, -28, 85, 72, -113, 59, 83, 13, 5, -83, -102, 123, 124, -22, 94, 108, -71, 95, 65, -2, 38, 38, -108, 14),
                curve = EllipticCurve.Ed25519,
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
                    DerivationPath("m/44'/60'/0'/0/0") to ExtendedPublicKey(
                        publicKey = byteArrayOf(2, 34, 6, 119, -106, 5, -119, 111, -22, 8, 23, -108, -72, -56, 6, 77, -17, -61, -101, -85, 16, 28, 18, 3, -3, -89, -81, -108, 48, -7, -86, -82, -67),
                        chainCode = byteArrayOf(-75, 55, 107, -106, -37, -81, -15, 72, -102, 94, 55, -39, 9, -112, 1, 90, -50, 103, 53, 120, -92, -36, -85, -39, -65, 1, 88, 46, 92, 104, -13, -109),
                    ),
                    DerivationPath("m/44'/1'/0'/0/0") to ExtendedPublicKey(
                        publicKey = byteArrayOf(2, -40, 115, -15, 80, 104, 100, -29, 80, 29, 112, -35, -54, 64, -98, 45, 115, 108, 93, 113, -71, 89, 17, 72, 98, 21, 126, 82, -50, 50, -12, -87, -62),
                        chainCode = byteArrayOf(-27, 56, 57, 54, 27, -40, 65, 109, 119, -54, -94, 88, -29, -115, -45, -112, -31, 57, 53, 56, 118, 87, 34, -16, -64, -45, 105, 77, 91, -106, -92, 41),
                    ),
                    DerivationPath("m/44'/195'/0'/0/0") to ExtendedPublicKey(
                        publicKey = byteArrayOf(2, -40, 115, -15, 80, 104, 100, -29, 80, 29, 112, -35, -54, 64, -98, 45, 115, 108, 93, 113, -71, 89, 17, 72, 98, 21, 126, 82, -50, 50, -12, -87, -62),
                        chainCode = byteArrayOf(-27, 56, 57, 54, 27, -40, 65, 109, 119, -54, -94, 88, -29, -115, -45, -112, -31, 57, 53, 56, 118, 87, 34, -16, -64, -45, 105, 77, 91, -106, -92, 41),
                    ),
                ),
                extendedPublicKey = ExtendedPublicKey(
                    publicKey = byteArrayOf(2, -109, 28, -27, -124, -58, -97, -61, 43, -84, 90, -9, -5, 4, 90, 17, 112, -125, -108, 44, 19, -79, -60, -23, 34, -20, -20, 61, 84, 113, 120, -90, -5),
                    chainCode = byteArrayOf(80, 13, -8, -108, -35, 116, -92, 125, -65, -28, 85, 72, -113, 59, 83, 13, 5, -83, -102, 123, 124, -22, 94, 108, -71, 95, 65, -2, 38, 38, -108, 14),
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
        backupStatus = CardDTO.BackupStatus.NoBackup,
    )

    override val scanResponse = ScanResponse(
        card = cardDto,
        productType = ProductType.Wallet,
        walletData = WalletData(
            blockchain = "ETH",
            token = WalletData.Token(
                name = "Ethereum",
                symbol = "ETH",
                contractAddress = "0x",
                decimals = 8,
            ),
        ),
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
                            DerivationPath("m/84'/0'/0'/0/0") to ExtendedPublicKey( // btc
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
                            DerivationPath("m/44'/195'/0'/0/0") to ExtendedPublicKey(
                                publicKey = byteArrayOf(2, -40, 115, -15, 80, 104, 100, -29, 80, 29, 112, -35, -54, 64, -98, 45, 115, 108, 93, 113, -71, 89, 17, 72, 98, 21, 126, 82, -50, 50, -12, -87, -62),
                                chainCode = byteArrayOf(-27, 56, 57, 54, 27, -40, 65, 109, 119, -54, -94, 88, -29, -115, -45, -112, -31, 57, 53, 56, 118, 87, 34, -16, -64, -45, 105, 77, 91, -106, -92, 41),
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

    override val successResponse = SuccessResponse(cardId = "CB43000000000004")

    override val createProductWalletTaskResponse = CreateProductWalletTaskResponse(
        card = cardDto,
        derivedKeys = mapOf(
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