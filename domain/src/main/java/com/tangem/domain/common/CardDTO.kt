package com.tangem.domain.common

import com.squareup.moshi.JsonClass
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.EncryptionMode
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey
import com.tangem.operations.attestation.Attestation
import java.util.*
import com.tangem.common.card.FirmwareVersion as SdkFirmwareVersion

/**
 * [Card] copy
 * */
@JsonClass(generateAdapter = true)
data class CardDTO(
    val cardId: String,
    val batchId: String,
    val cardPublicKey: ByteArray,
    val firmwareVersion: FirmwareVersion,
    val manufacturer: Manufacturer,
    val issuer: Issuer,
    val settings: Settings,
    val linkedTerminalStatus: LinkedTerminalStatus,
    val isAccessCodeSet: Boolean,
    val isPasscodeSet: Boolean?,
    val supportedCurves: List<EllipticCurve>,
    val wallets: List<Wallet>,
    val attestation: Attestation,
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
        linkedTerminalStatus = LinkedTerminalStatus.fromSdkStatus(card.linkedTerminalStatus),
        isAccessCodeSet = card.isAccessCodeSet,
        isPasscodeSet = card.isPasscodeSet,
        supportedCurves = card.supportedCurves,
        wallets = card.wallets.map { Wallet(it) },
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
        if (linkedTerminalStatus != other.linkedTerminalStatus) return false
        if (isAccessCodeSet != other.isAccessCodeSet) return false
        if (isPasscodeSet != other.isPasscodeSet) return false
        if (supportedCurves != other.supportedCurves) return false
        if (wallets != other.wallets) return false
        if (attestation != other.attestation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cardId.hashCode()
        result = 31 * result + batchId.hashCode()
        result = 31 * result + cardPublicKey.contentHashCode()
        result = 31 * result + firmwareVersion.hashCode()
        result = 31 * result + manufacturer.hashCode()
        result = 31 * result + issuer.hashCode()
        result = 31 * result + settings.hashCode()
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
        val securityDelay: Int,
        val maxWalletsCount: Int,
        val isSettingAccessCodeAllowed: Boolean,
        val isSettingPasscodeAllowed: Boolean,
        val isResettingUserCodesAllowed: Boolean,
        val isLinkedTerminalEnabled: Boolean,
        val isBackupAllowed: Boolean,
        val supportedEncryptionModes: List<EncryptionMode>,
        val isFilesAllowed: Boolean,
        val isHDWalletAllowed: Boolean,
    ) {
        constructor(settings: Card.Settings) : this(
            securityDelay = settings.securityDelay,
            maxWalletsCount = settings.maxWalletsCount,
            isSettingAccessCodeAllowed = settings.isSettingAccessCodeAllowed,
            isSettingPasscodeAllowed = settings.isSettingPasscodeAllowed,
            isResettingUserCodesAllowed = settings.isResettingUserCodesAllowed,
            isLinkedTerminalEnabled = settings.isLinkedTerminalEnabled,
            isBackupAllowed = settings.isBackupAllowed,
            supportedEncryptionModes = settings.supportedEncryptionModes,
            isFilesAllowed = settings.isFilesAllowed,
            isHDWalletAllowed = settings.isHDWalletAllowed,
        )
    }

    @JsonClass(generateAdapter = true)
    data class FirmwareVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
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
    }

    @JsonClass(generateAdapter = true)
    data class Manufacturer(
        val name: String,
        val manufactureDate: Date,
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
        val name: String,
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
            if (!publicKey.contentEquals(other.publicKey)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + publicKey.contentHashCode()
            return result
        }
    }

    @JsonClass(generateAdapter = true)
    data class Wallet(
        val publicKey: ByteArray,
        val chainCode: ByteArray?,
        val curve: EllipticCurve,
        val settings: CardWallet.Settings,
        val totalSignedHashes: Int?,
        val remainingSignatures: Int?,
        val index: Int,
        val hasBackup: Boolean,
        val derivedKeys: Map<DerivationPath, ExtendedPublicKey>,
        val extendedPublicKey: ExtendedPublicKey?,
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
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Wallet) return false

            if (!publicKey.contentEquals(other.publicKey)) return false
            if (chainCode != null) {
                if (other.chainCode == null) return false
                if (!chainCode.contentEquals(other.chainCode)) return false
            } else if (other.chainCode != null) return false
            if (curve != other.curve) return false
            if (settings != other.settings) return false
            if (totalSignedHashes != other.totalSignedHashes) return false
            if (remainingSignatures != other.remainingSignatures) return false
            if (index != other.index) return false
            if (hasBackup != other.hasBackup) return false

            return true
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
            return result
        }
    }

    enum class LinkedTerminalStatus {
        Current,
        Other,
        None;

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

        object NoBackup : BackupStatus()

        val isActive: Boolean
            get() = this is Active || this is CardLinked

        companion object {
            internal fun fromSdkStatus(sdkStatus: Card.BackupStatus?): BackupStatus? {
                return when (sdkStatus) {
                    is Card.BackupStatus.NoBackup -> NoBackup
                    is Card.BackupStatus.CardLinked -> CardLinked(sdkStatus.cardCount)
                    is Card.BackupStatus.Active -> Active(sdkStatus.cardCount)
                    null -> null
                }
            }
        }
    }
}
