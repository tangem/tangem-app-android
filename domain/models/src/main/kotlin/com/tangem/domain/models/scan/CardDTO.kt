package com.tangem.domain.models.scan

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.EncryptionMode
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.common.card.FirmwareVersion as SdkFirmwareVersion
import com.tangem.operations.attestation.Attestation
import java.util.Date

// TODO: Move to :domain:card:models
/**
 * [Card] copy
 * */
@JsonClass(generateAdapter = true)
data class CardDTO(
    @Json(name = "cardId")
    val cardId: String,
    @Json(name = "batchId")
    val batchId: String,
    @Json(name = "cardPublicKey")
    val cardPublicKey: ByteArray,
    @Json(name = "firmwareVersion")
    val firmwareVersion: FirmwareVersion,
    @Json(name = "manufacturer")
    val manufacturer: Manufacturer,
    @Json(name = "issuer")
    val issuer: Issuer,
    @Json(name = "settings")
    val settings: Settings,
    @Json(name = "userSettings")
    val userSettings: UserSettings?,
    @Json(name = "linkedTerminalStatus")
    val linkedTerminalStatus: LinkedTerminalStatus,
    @Json(name = "isAccessCodeSet")
    val isAccessCodeSet: Boolean,
    @Json(name = "isPasscodeSet")
    val isPasscodeSet: Boolean?,
    @Json(name = "supportedCurves")
    val supportedCurves: List<EllipticCurve>,
    @Json(name = "wallets")
    val wallets: List<Wallet>,
    @Json(name = "attestation")
    val attestation: Attestation,
    @Json(name = "backupStatus")
    val backupStatus: BackupStatus?,
) {
    constructor(card: Card) : this(
        cardId = card.cardId,
        batchId = card.batchId,
        cardPublicKey = card.cardPublicKey,
        firmwareVersion = FirmwareVersion(card.firmwareVersion),
        manufacturer = Manufacturer(card.manufacturer),
        issuer = Issuer(card.issuer),
        settings = Settings(card.settings),
        userSettings = UserSettings(card.userSettings),
        linkedTerminalStatus = LinkedTerminalStatus.fromSdkStatus(card.linkedTerminalStatus),
        isAccessCodeSet = card.isAccessCodeSet,
        isPasscodeSet = card.isPasscodeSet,
        supportedCurves = card.supportedCurves,
        wallets = card.wallets.map(::Wallet),
        attestation = card.attestation,
        backupStatus = BackupStatus.fromSdkStatus(card.backupStatus),
    )

    @Suppress("ComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CardDTO) return false

        if (cardId != other.cardId) return false
        if (batchId != other.batchId) return false
        if (!cardPublicKey.contentEquals(other.cardPublicKey)) return false
        if (firmwareVersion != other.firmwareVersion) return false
        if (manufacturer != other.manufacturer) return false
        if (issuer != other.issuer) return false
        if (settings != other.settings) return false
        if (userSettings != other.userSettings) return false
        if (linkedTerminalStatus != other.linkedTerminalStatus) return false
        if (isAccessCodeSet != other.isAccessCodeSet) return false
        if (isPasscodeSet != other.isPasscodeSet) return false
        if (supportedCurves != other.supportedCurves) return false
        if (wallets != other.wallets) return false
        return attestation == other.attestation
    }

    override fun hashCode(): Int {
        var result = cardId.hashCode()
        result = 31 * result + batchId.hashCode()
        result = 31 * result + cardPublicKey.contentHashCode()
        result = 31 * result + firmwareVersion.hashCode()
        result = 31 * result + manufacturer.hashCode()
        result = 31 * result + issuer.hashCode()
        result = 31 * result + settings.hashCode()
        result = 31 * result + userSettings.hashCode()
        result = 31 * result + linkedTerminalStatus.hashCode()
        result = 31 * result + isAccessCodeSet.hashCode()
        result = 31 * result + (isPasscodeSet?.hashCode() ?: 0)
        result = 31 * result + supportedCurves.hashCode()
        result = 31 * result + wallets.hashCode()
        result = 31 * result + attestation.hashCode()
        return result
    }

    @JsonClass(generateAdapter = true)
    data class Settings(
        @Json(name = "securityDelay")
        val securityDelay: Int,
        @Json(name = "maxWalletsCount")
        val maxWalletsCount: Int,
        @Json(name = "isSettingAccessCodeAllowed")
        val isSettingAccessCodeAllowed: Boolean,
        @Json(name = "isSettingPasscodeAllowed")
        val isSettingPasscodeAllowed: Boolean,
        @Json(name = "isResettingUserCodesAllowed")
        val isResettingUserCodesAllowed: Boolean,
        @Json(name = "isLinkedTerminalEnabled")
        val isLinkedTerminalEnabled: Boolean,
        @Json(name = "isBackupAllowed")
        val isBackupAllowed: Boolean,
        @Json(name = "supportedEncryptionModes")
        val supportedEncryptionModes: List<EncryptionMode>,
        @Json(name = "isFilesAllowed")
        val isFilesAllowed: Boolean,
        @Json(name = "isHDWalletAllowed")
        val isHDWalletAllowed: Boolean,
        @Json(name = "isKeysImportAllowed")
        val isKeysImportAllowed: Boolean = false,
    ) {
        constructor(settings: Card.Settings) : this(
            securityDelay = settings.securityDelay,
            maxWalletsCount = settings.maxWalletsCount,
            isSettingAccessCodeAllowed = settings.isSettingAccessCodeAllowed,
            isSettingPasscodeAllowed = settings.isSettingPasscodeAllowed,
            isResettingUserCodesAllowed = settings.isRemovingUserCodesAllowed,
            isLinkedTerminalEnabled = settings.isLinkedTerminalEnabled,
            isBackupAllowed = settings.isBackupAllowed,
            supportedEncryptionModes = settings.supportedEncryptionModes,
            isFilesAllowed = settings.isFilesAllowed,
            isHDWalletAllowed = settings.isHDWalletAllowed,
            isKeysImportAllowed = settings.isKeysImportAllowed,
        )
    }

    @JsonClass(generateAdapter = true)
    data class UserSettings(
        @Json(name = "isUserCodeRecoveryAllowed")
        val isUserCodeRecoveryAllowed: Boolean,
    ) {
        constructor(userSettings: com.tangem.common.card.UserSettings) : this(
            userSettings.isUserCodeRecoveryAllowed,
        )
    }

    @JsonClass(generateAdapter = true)
    data class FirmwareVersion(
        @Json(name = "major")
        val major: Int,
        @Json(name = "minor")
        val minor: Int,
        @Json(name = "patch")
        val patch: Int,
        @Json(name = "type")
        val type: SdkFirmwareVersion.FirmwareType,
    ) : Comparable<SdkFirmwareVersion> {
        val stringValue: String
            get() = StringBuilder()
                .append("$major.$minor")
                .append(if (patch != 0) ".$patch" else "")
                .append(type.rawValue ?: "")
                .toString()

        val doubleValue: Double
            get() = "$major.$minor".toDouble()

        constructor(firmwareVersion: SdkFirmwareVersion) : this(
            major = firmwareVersion.major,
            minor = firmwareVersion.minor,
            patch = firmwareVersion.patch,
            type = firmwareVersion.type,
        )

        override fun compareTo(other: SdkFirmwareVersion): Int = when {
            major != other.major -> major.compareTo(other.major)
            minor != other.minor -> minor.compareTo(other.minor)
            else -> patch.compareTo(other.patch)
        }

        fun toSdkFirmwareVersion() = SdkFirmwareVersion(major = major, minor = minor)
    }

    @JsonClass(generateAdapter = true)
    data class Manufacturer(
        @Json(name = "name")
        val name: String,
        @Json(name = "manufactureDate")
        val manufactureDate: Date,
        @Json(name = "signature")
        val signature: ByteArray?,
    ) {
        constructor(manufacturer: Card.Manufacturer) : this(
            name = manufacturer.name,
            manufactureDate = manufacturer.manufactureDate,
            signature = manufacturer.signature,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Manufacturer) return false

            if (name != other.name) return false
            if (manufactureDate != other.manufactureDate) return false
            if (signature != null) {
                if (other.signature == null) return false
                if (!signature.contentEquals(other.signature)) return false
            } else if (other.signature != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + manufactureDate.hashCode()
            result = 31 * result + (signature?.contentHashCode() ?: 0)
            return result
        }
    }

    @JsonClass(generateAdapter = true)
    data class Issuer(
        @Json(name = "name")
        val name: String,
        @Json(name = "publicKey")
        val publicKey: ByteArray,
    ) {
        constructor(issuer: Card.Issuer) : this(
            name = issuer.name,
            publicKey = issuer.publicKey,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Issuer) return false

            if (name != other.name) return false
            return publicKey.contentEquals(other.publicKey)
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + publicKey.contentHashCode()
            return result
        }
    }

    @JsonClass(generateAdapter = true)
    data class Wallet(
        @Json(name = "publicKey")
        val publicKey: ByteArray,
        @Json(name = "chainCode")
        val chainCode: ByteArray?,
        @Json(name = "curve")
        val curve: EllipticCurve,
        @Json(name = "settings")
        val settings: CardWallet.Settings,
        @Json(name = "totalSignedHashes")
        val totalSignedHashes: Int?,
        @Json(name = "remainingSignatures")
        val remainingSignatures: Int?,
        @Json(name = "index")
        val index: Int,
        @Json(name = "hasBackup")
        val hasBackup: Boolean,
        @Json(name = "derivedKeys")
        val derivedKeys: Map<DerivationPath, ExtendedPublicKey>,
        @Json(name = "extendedPublicKey")
        val extendedPublicKey: ExtendedPublicKey?,
        @Json(name = "isImported")
        val isImported: Boolean = false,
    ) {
        constructor(wallet: CardWallet) : this(
            publicKey = wallet.publicKey,
            chainCode = wallet.chainCode,
            curve = wallet.curve,
            settings = wallet.settings,
            totalSignedHashes = wallet.totalSignedHashes,
            remainingSignatures = wallet.remainingSignatures,
            index = wallet.index,
            hasBackup = wallet.hasBackup,
            derivedKeys = wallet.derivedKeys,
            extendedPublicKey = wallet.extendedPublicKey,
            isImported = wallet.isImported,
        )

        @Suppress("CyclomaticComplexMethod")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Wallet) return false

            if (!publicKey.contentEquals(other.publicKey)) return false
            if (chainCode != null) {
                if (other.chainCode == null) return false
                if (!chainCode.contentEquals(other.chainCode)) return false
            } else {
                if (other.chainCode != null) return false
            }
            if (curve != other.curve) return false
            if (settings != other.settings) return false
            if (totalSignedHashes != other.totalSignedHashes) return false
            if (remainingSignatures != other.remainingSignatures) return false
            if (index != other.index) return false
            if (hasBackup != other.hasBackup) return false
            return isImported == other.isImported
        }

        override fun hashCode(): Int {
            var result = publicKey.contentHashCode()
            result = 31 * result + (chainCode?.contentHashCode() ?: 0)
            result = 31 * result + curve.hashCode()
            result = 31 * result + settings.hashCode()
            result = 31 * result + (totalSignedHashes ?: 0)
            result = 31 * result + (remainingSignatures ?: 0)
            result = 31 * result + index
            result = 31 * result + hasBackup.hashCode()
            result = 31 * result + isImported.hashCode()
            return result
        }
    }

    @JsonClass(generateAdapter = false)
    enum class LinkedTerminalStatus {
        @Json(name = "Current")
        Current,

        @Json(name = "Other")
        Other,

        @Json(name = "None")
        None,
        ;

        companion object {
            internal fun fromSdkStatus(sdkStatus: Card.LinkedTerminalStatus): LinkedTerminalStatus {
                return when (sdkStatus) {
                    Card.LinkedTerminalStatus.Current -> Current
                    Card.LinkedTerminalStatus.Other -> Other
                    Card.LinkedTerminalStatus.None -> None
                }
            }
        }
    }

    sealed class BackupStatus {

        data class CardLinked(val cardCount: Int) : BackupStatus()

        data class Active(val cardCount: Int) : BackupStatus()

        data object NoBackup : BackupStatus()

        val isActive: Boolean
            get() = this is Active

        companion object {
            internal fun fromSdkStatus(sdkStatus: Card.BackupStatus?): BackupStatus? {
                return when (sdkStatus) {
                    is Card.BackupStatus.NoBackup -> NoBackup
                    is Card.BackupStatus.CardLinked -> CardLinked(sdkStatus.cardsCount)
                    is Card.BackupStatus.Active -> Active(sdkStatus.cardsCount)
                    null -> null
                }
            }
        }
    }

    companion object {
        val RING_BATCH_IDS = listOf("AC17", "BA01")
        const val RING_BATCH_PREFIX = "BA"
    }
}