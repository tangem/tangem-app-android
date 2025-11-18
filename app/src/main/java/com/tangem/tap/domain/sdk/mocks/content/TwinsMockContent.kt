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

object TwinsMockContent : MockContent {

    private val primaryCard = PrimaryCard(
        cardId = "CB64000000015006",
        batchId = "CB64",
        cardPublicKey = byteArrayOf(4, -122, 60, 93, 121, 108, 26, 78, -33, 36, 112, 61, -4, 108, 82, 6, 47, -50, 109, 37, -109, 58, -102, 3, 50, 127, -123, -68, -96, 18, -78, 56, -50, 102, -112, 51, -25, 96, -69, 97, 31, 35, -68, 22, 38, 61, 60, -97, -69, -51, -62, -37, -63, 91, 127, -103, 103, -39, 85, 85, 85, 92, -70, -58, 109),
        linkingKey = byteArrayOf(
            2, 121, 98, 127, -70, 14, 5, -23, -76, 115, -30, -26, 111, 17, 110, 34, -100, -121,
            -57, -123, 74, 3, -91, 56, -20, 56, 50, -40, -101, 96, 82, 70, -91,
        ),
        existingWalletsCount = 3, isHDWalletAllowed = false,
        issuer = Card.Issuer(
            name = "TANGEM SDK",
            publicKey = byteArrayOf(4, 95, 22, -67, 29, 46, -81, -28, 99, -26, 42, 51, 90, 9, -26, -78, -69, -53, -48, 68, 82, 82, 104, -123, -53, 103, -97, -60, -46, 122, -15, -67, 34, -11, 83, -57, -34, -17, -75, 79, -45, -44, -13, 97, -47, 78, 109, -61, -15, 27, 125, 78, -95, -125, 37, 10, 96, 114, 14, -67, -7, -31, 16, -51, 38),
        ),
        manufacturer = Card.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1743759687),
            signature = byteArrayOf(89, -15, -31, 86, 117, 59, -27, -111, 33, 23, -44, 15, -55, -46, -52, 63, 66, 41, -73, -22, 4, -26, 96, -33, -91, 28, 111, 55, -105, -83, -19, -10, 39, -88, 47, 60, 41, -71, 81, 50, -1, -121, 86, -48, -93, 103, -42, 12, -3, -126, -47, -79, 44, 115, 126, 8, -63, 10, 77, 2, 107, -87, -54, -65),
        ),
        walletCurves = listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519, EllipticCurve.Bls12381G2Aug),
        firmwareVersion = FirmwareVersion(
            major = 3,
            minor = 38,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        ),
        isKeysImportAllowed = false,
        certificate = null,
    )

    override val cardDto = CardDTO(
        cardId = "CB64000000015006",
        batchId = "CB64",
        cardPublicKey = byteArrayOf(2, 48, 4, 54, -34, -33, 22, 53, -92, 8, 72, 50, 69, 1, 105, -53, 9, -126, -23, -61, -62, -85, -24, -112, -32, 76, -88, -66, -25, -83, -70, -95, 90),
        firmwareVersion = CardDTO.FirmwareVersion(
            major = 3,
            minor = 38,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Release,
        ),
        manufacturer = CardDTO.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1649635200000),
            signature = byteArrayOf(89, -15, -31, 86, 117, 59, -27, -111, 33, 23, -44, 15, -55, -46, -52, 63, 66, 41, -73, -22, 4, -26, 96, -33, -91, 28, 111, 55, -105, -83, -19, -10, 39, -88, 47, 60, 41, -71, 81, 50, -1, -121, 86, -48, -93, 103, -42, 12, -3, -126, -47, -79, 44, 115, 126, 8, -63, 10, 77, 2, 107, -87, -54, -65),
        ),
        issuer = CardDTO.Issuer(
            name = "TANGEM SDK",
            publicKey = byteArrayOf(4, 95, 22, -67, 29, 46, -81, -28, 99, -26, 42, 51, 90, 9, -26, -78, -69, -53, -48, 68, 82, 82, 104, -123, -53, 103, -97, -60, -46, 122, -15, -67, 34, -11, 83, -57, -34, -17, -75, 79, -45, -44, -13, 97, -47, 78, 109, -61, -15, 27, 125, 78, -95, -125, 37, 10, 96, 114, 14, -67, -7, -31, 16, -51, 38),
        ),
        settings = CardDTO.Settings(
            securityDelay = 15000,
            maxWalletsCount = 1,
            isSettingAccessCodeAllowed = true,
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
        supportedCurves = listOf(
            EllipticCurve.Secp256k1,
        ),
        wallets = listOf(
            CardDTO.Wallet(
                publicKey = byteArrayOf(4, 42, -7, -17, 16, -16, -85, 8, 70, 36, -114, 18, 45, -126, 101, -89, 22, 96, 53, -107, 61, -109, -118, 83, 116, 100, -79, -90, 119, 123, -67, 23, -76, -49, -124, 71, 61, -50, -91, -18, -100, -34, 93, -48, 36, -82, -28, 45, 125, -3, 56, 8, 68, -40, 40, -34, -112, -67, -43, 35, 84, -106, 20, 62, -79),
                chainCode = null,
                curve = EllipticCurve.Secp256k1,
                settings = CardWallet.Settings(isPermanent = false),
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
        backupStatus = null,
    )

    override val scanResponse = ScanResponse(
        card = cardDto,
        productType = ProductType.Twins,
        walletData = WalletData(
            blockchain = "BTC",
            token = null,
        ),
        secondTwinPublicKey = "041535EF77F57A7E7C1541858E5BD3C096A81CEB39FCC72FB9354BBC55FEF86B3C2D5A44750FABE59D268BEC0CB1BAC854A7678F48A26BD355CE39FE197457E2FF",
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

    override val successResponse = SuccessResponse(cardId = "CB61000000001264")

    override val createProductWalletTaskResponse = CreateProductWalletTaskResponse(
        card = cardDto,
        derivedKeys = emptyMap(),
        primaryCard = primaryCard,
    )

    override val importWalletResponse: CreateProductWalletTaskResponse
        get() = error("Available only for Wallet 2")

    override val createFirstTwinResponse: CreateWalletResponse = CreateWalletResponse(
        cardId = "CB64000000015006",
        wallet = CardWallet(
            publicKey = byteArrayOf(4, 42, -7, -17, 16, -16, -85, 8, 70, 36, -114, 18, 45, -126, 101, -89, 22, 96, 53, -107, 61, -109, -118, 83, 116, 100, -79, -90, 119, 123, -67, 23, -76, -49, -124, 71, 61, -50, -91, -18, -100, -34, 93, -48, 36, -82, -28, 45, 125, -3, 56, 8, 68, -40, 40, -34, -112, -67, -43, 35, 84, -106, 20, 62, -79),
            chainCode = null,
            curve = EllipticCurve.Secp256k1,
            settings = CardWallet.Settings(isPermanent = false),
            totalSignedHashes = 1,
            remainingSignatures = 999999,
            index = 0,
            isImported = false,
            hasBackup = false,
            derivedKeys = emptyMap(),
        ),
    )

    override val createSecondTwinResponse: CreateWalletResponse = CreateWalletResponse(
        cardId = "CB65000000015005",
        wallet = CardWallet(
            publicKey = byteArrayOf(4, 21, 53, -17, 119, -11, 122, 126, 124, 21, 65, -123, -114, 91, -45, -64, -106, -88, 28, -21, 57, -4, -57, 47, -71, 53, 75, -68, 85, -2, -8, 107, 60, 45, 90, 68, 117, 15, -85, -27, -99, 38, -117, -20, 12, -79, -70, -56, 84, -89, 103, -113, 72, -94, 107, -45, 85, -50, 57, -2, 25, 116, 87, -30, -1),
            chainCode = null,
            curve = EllipticCurve.Secp256k1,
            settings = CardWallet.Settings(isPermanent = false),
            totalSignedHashes = 1,
            remainingSignatures = 999999,
            index = 0,
            isImported = false,
            hasBackup = false,
            derivedKeys = emptyMap(),
        ),
    )

    override val finalizeTwinResponse: ScanResponse = ScanResponse(
        card = cardDto,
        productType = ProductType.Twins,
        walletData = WalletData(
            blockchain = "BTC",
            token = null,
        ),
        secondTwinPublicKey = "041535EF77F57A7E7C1541858E5BD3C096A81CEB39FCC72FB9354BBC55FEF86B3C2D5A44750FABE59D268BEC0CB1BAC854A7678F48A26BD355CE39FE197457E2FF",
        derivedKeys = emptyMap(),
        primaryCard = primaryCard,
    )
}