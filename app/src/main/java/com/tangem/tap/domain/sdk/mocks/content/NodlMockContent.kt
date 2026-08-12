package com.tangem.tap.domain.sdk.mocks.content

import com.tangem.common.SuccessResponse
import com.tangem.common.card.*
import com.tangem.common.encryption.EncryptionMode
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.attestation.Attestation
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.sdk.api.CreateProductWalletTaskResponse
import com.tangem.tap.domain.sdk.mocks.MockContent
import java.util.Date

// Nodl: single-currency card with a token (Stellar XLM + NODL Stellar asset, ed25519, firmware 2.42 SDK).
// Card fields mirror the iOS Nodl mock. ed25519 first wallet + firmware < 4.0 → isMultiwalletAllowed = false,
// and walletData.token != null → isSingleWalletWithToken = true. walletData.blockchain is set, so getBlockchain()
// resolves XLM directly (the Note batch→blockchain table is NOT consulted). Currencies (XLM coin + NODL token)
// are built locally from walletData, not from the server, and both resolve to the same Stellar address derived
// directly from the raw ed25519 wallet key.
object NodlMockContent : MockContent {

    override val cardDto = CardDTO(
        cardId = "BB00000000000000",
        batchId = "FFFF",
        cardPublicKey = byteArrayOf(0, 0),
        firmwareVersion = CardDTO.FirmwareVersion(
            major = 2,
            minor = 42,
            patch = 0,
            type = FirmwareVersion.FirmwareType.Sdk,
        ),
        manufacturer = CardDTO.Manufacturer(
            name = "TANGEM",
            manufactureDate = Date(1573603200000), // 2019-11-13
            signature = byteArrayOf(0, 0),
        ),
        issuer = CardDTO.Issuer(
            name = "TANGEM SDK",
            publicKey = byteArrayOf(0, 0),
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
            isFilesAllowed = false,
            isHDWalletAllowed = false,
            isKeysImportAllowed = false,
        ),
        userSettings = CardDTO.UserSettings(isUserCodeRecoveryAllowed = false),
        linkedTerminalStatus = CardDTO.LinkedTerminalStatus.None,
        isAccessCodeSet = false,
        isPasscodeSet = false,
        supportedCurves = listOf(EllipticCurve.Ed25519),
        wallets = listOf(
            CardDTO.Wallet(
                publicKey = byteArrayOf(6, 127, 99, 79, 34, -121, 84, -23, -66, 124, 72, -120, -97, -13, -123, 115, -98, -67, -78, -31, 124, -106, -116, 77, 109, 46, -48, -89, -15, 52, 25, 50),
                chainCode = byteArrayOf(),
                curve = EllipticCurve.Ed25519,
                settings = CardWallet.Settings(isPermanent = false),
                totalSignedHashes = 5,
                remainingSignatures = 95,
                index = 0,
                hasBackup = false,
                derivedKeys = emptyMap(),
                extendedPublicKey = null,
                isImported = false,
            ),
        ),
        attestation = Attestation(
            cardKeyAttestation = Attestation.Status.Failed,
            walletKeysAttestation = Attestation.Status.Skipped,
            firmwareAttestation = Attestation.Status.Skipped,
            cardUniquenessAttestation = Attestation.Status.Skipped,
        ),
        backupStatus = CardDTO.BackupStatus.NoBackup,
    )

    override val scanResponse = ScanResponse(
        card = cardDto,
        productType = ProductType.Note,
        walletData = WalletData(
            blockchain = "XLM",
            token = WalletData.Token(
                name = "NODL",
                symbol = "NODL",
                contractAddress = "GB2Y3AWXVROM2BHFQKQPTWKIOI3TZEBBD3LTKTVQTKEPXGOBE742NODL",
                decimals = 7,
            ),
        ),
        secondTwinPublicKey = null,
        derivedKeys = emptyMap(),
        primaryCard = null,
    )

    override val derivationTaskResponse = DerivationTaskResponse(entries = emptyMap())

    override val extendedPublicKey
        get() = error("Available only for wallet+?")

    override val successResponse = SuccessResponse(cardId = "BB00000000000000")

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