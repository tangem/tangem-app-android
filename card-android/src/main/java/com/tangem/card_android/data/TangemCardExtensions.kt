package com.tangem.card_android.data

import android.os.Bundle
import com.tangem.card_common.data.Manufacturer
import com.tangem.card_common.data.TangemCard
import com.tangem.card_common.util.Log
import java.util.*

const val EXTRA_TANGEM_CARD = "Card"
const val EXTRA_TANGEM_CARD_UID = "UID"

fun TangemCard.loadFromBundle(B: Bundle) {
    uid = B.getString("UID")
    cid = B.getByteArray("CID")
    pin = B.getString("PIN")
    PIN2 = TangemCard.PIN2_Mode.valueOf(B.getString("PIN2")!!)
    status = TangemCard.Status.valueOf(B.getString("Status")!!)
    blockchainID = B.getString("Blockchain")
    tokensDecimal = B.getInt("TokensDecimal", 18)
    tokenSymbol = B.getString("TokenSymbol", "")
    contractAddress = B.getString("ContractAddress", "")
//        if (B.containsKey("BlockchainName"))
//            blockchainName = B.getString("BlockchainName", "");
    if (B.containsKey("dtPersonalization")) {
        personalizationDateTime = Date(B.getLong("dtPersonalization"))
    }
    remainingSignatures = B.getInt("RemainingSignatures")
    maxSignatures = B.getInt("MaxSignatures")
    health = B.getInt("health")
    if (B.containsKey("settingsMask")) settingsMask = B.getInt("settingsMask")
    pauseBeforePIN2 = B.getInt("pauseBeforePIN2")
    if (B.containsKey("signingMethod"))
        setSigningMethod(B.getInt("signingMethod"))
    if (B.containsKey("Manufacturer"))
        setManufacturer(Manufacturer.valueOf(B.getString("Manufacturer")!!), B.getBoolean("ManufacturerConfirmed", false))
    if (B.containsKey("EncryptionMode"))
        encryptionMode = TangemCard.EncryptionMode.valueOf(B.getString("EncryptionMode")!!)
    else
        encryptionMode = null

    if (B.containsKey("SignedHashes")) signedHashes = B.getInt("SignedHashes")

    if (B.containsKey("Issuer")) setIssuer(B.getString("Issuer"), B.getByteArray("IssuerPublicDataKey"))

    if (B.containsKey("FirmwareVersion")) firmwareVersion = B.getString("FirmwareVersion")
    if (B.containsKey("Batch")) batch = B.getString("Batch")

    isCardPublicKeyValid = B.getBoolean("CardPublicKeyValid")
    if (B.containsKey("CardPublicKey")) cardPublicKey = B.getByteArray("CardPublicKey")

    if (B.containsKey("OfflineBalance")) offlineBalance = B.getByteArray("OfflineBalance")
    else clearOfflineBalance()

    if (B.containsKey("Denomination") && B.containsKey("DenominationText")) {
        setDenomination(B.getByteArray("Denomination"), B.getString("DenominationText"))
    } else if (B.containsKey("Denomination")) {
        setDenomination(B.getByteArray("Denomination"), "N/A")
    } else clearDenomination()

    if (B.containsKey("IssuerData") && B.containsKey("IssuerDataSignature"))
        setIssuerData(B.getByteArray("IssuerData"), B.getByteArray("IssuerDataSignature"))
    else setIssuerData(null, null)

    if (B.containsKey("NeedWriteIssuerData"))
        needWriteIssuerData = B.getBoolean("NeedWriteIssuerData")

    isWalletPublicKeyValid = B.getBoolean("WalletPublicKeyValid")
    if (B.containsKey("PublicKey")) {
        walletPublicKey = B.getByteArray("PublicKey")
    }
    if (B.containsKey("PublicKeyRar")) {
        walletPublicKeyRar = B.getByteArray("PublicKeyRar")
    }

    if (B.containsKey("codeConfirmed"))
        isCodeConfirmed = B.getBoolean("codeConfirmed")

    if (B.containsKey("onlineVerified"))
        isOnlineVerified = B.getBoolean("onlineVerified")

    if (B.containsKey("onlineValidated"))
        isOnlineValidated = B.getBoolean("onlineValidated")

}

val TangemCard.asBundle: Bundle
    get() {
        val bundle = Bundle()
        saveToBundle(bundle)
        return bundle
    }

fun TangemCard.saveToBundle(B: Bundle) {
    try {
        B.putString("UID", uid)
        B.putByteArray("CID", cid)
        B.putString("PIN", pin)
        B.putString("PIN2", PIN2.name)
        B.putString("Status", status.name)
        B.putString("Blockchain", blockchainID)
        B.putInt("TokensDecimal", tokensDecimal)
        B.putString("TokenSymbol", tokenSymbol)
        B.putString("ContractAddress", contractAddress)
        if (personalizationDateTime != null) B.putLong("dtPersonalization", personalizationDateTime.time)

        B.putInt("RemainingSignatures", remainingSignatures)
        B.putInt("MaxSignatures", maxSignatures)
        B.putInt("Health", health)
        if (settingsMask != null) B.putInt("settingsMask", settingsMask)
        B.putInt("pauseBeforePIN2", pauseBeforePIN2)
        if( allowedSigningMethod!=null ){
            var iSigningMethod=0x80
            for(sM in allowedSigningMethod)
            {
                iSigningMethod=iSigningMethod.or(0x01.shl(sM.ID))
            }
            B.putInt("signingMethod", iSigningMethod)
        }
        if (manufacturer != null) B.putString("Manufacturer", manufacturer.name)
        if (encryptionMode != null) B.putString("EncryptionMode", encryptionMode.name)
        if (issuer != null) B.putString("Issuer", issuer.getID())
        if (issuerPublicDataKey != null) B.putByteArray("IssuerPublicDataKey", issuerPublicDataKey)
        if (firmwareVersion != null) B.putString("FirmwareVersion", firmwareVersion)
        if (batch != null) B.putString("Batch", batch)
        B.putBoolean("ManufacturerConfirmed", isManufacturerConfirmed)
        B.putBoolean("CardPublicKeyValid", isCardPublicKeyValid)
        B.putByteArray("CardPublicKey", cardPublicKey)

        B.putInt("SignedHashes", signedHashes)
        B.putBoolean("WalletPublicKeyValid", isWalletPublicKeyValid)
        if (walletPublicKey != null)
            B.putByteArray("PublicKey", walletPublicKey)
        if (walletPublicKeyRar != null)
            B.putByteArray("PublicKeyRar", walletPublicKeyRar)

        if (offlineBalance != null) B.putByteArray("OfflineBalance", offlineBalance)

        if (denomination != null) B.putByteArray("Denomination", denomination)
        if (denominationText != null) B.putString("DenominationText", denominationText)

        if (issuerData != null && issuerDataSignature != null) {
            B.putByteArray("IssuerData", issuerData)
            B.putByteArray("IssuerDataSignature", issuerDataSignature)
            B.putBoolean("NeedWriteIssuerData", needWriteIssuerData)
        }

        if (isCodeConfirmed != null)
            B.putBoolean("codeConfirmed", isCodeConfirmed)

        if (isOnlineVerified != null)
            B.putBoolean("onlineVerified", isOnlineVerified)

        if (isOnlineValidated != null)
            B.putBoolean("onlineValidated", isOnlineValidated)
    } catch (e: Exception) {
        Log.e("Can't save to bundle ", e.message)
    }

}
