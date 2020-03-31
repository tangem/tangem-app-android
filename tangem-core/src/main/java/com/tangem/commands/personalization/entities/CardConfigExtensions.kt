package com.tangem.commands.personalization.entities

import com.tangem.commands.Settings
import com.tangem.commands.SettingsMask
import com.tangem.commands.SettingsMaskBuilder
import com.tangem.commands.personalization.NdefEncoder
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.sign

/**
[REDACTED_AUTHOR]
 */
internal fun CardConfig.createSettingsMask(): SettingsMask {
    val builder = SettingsMaskBuilder()

    if (allowSwapPin) builder.add(Settings.AllowSwapPIN)
    if (allowSwapPin2) builder.add(Settings.AllowSwapPIN2)
    if (useCvc) builder.add(Settings.UseCVC)
    if (isReusable) builder.add(Settings.IsReusable)

    if (useOneCommandAtTime) builder.add(Settings.UseOneCommandAtTime)
    if (useNdef) builder.add(Settings.UseNdef)
    if (useDynamicNdef) builder.add(Settings.UseDynamicNdef)
    if (disablePrecomputedNdef) builder.add(Settings.DisablePrecomputedNdef)

    if (protocolAllowUnencrypted) builder.add(Settings.ProtocolAllowUnencrypted)
    if (protocolAllowStaticEncryption) builder.add(Settings.ProtocolAllowStaticEncryption)

    if (forbidDefaultPin) builder.add(Settings.ForbidDefaultPIN)

    if (useActivation) builder.add(Settings.UseActivation)

    if (useBlock) builder.add(Settings.UseBlock)
    if (smartSecurityDelay) builder.add(Settings.SmartSecurityDelay)

    if (protectIssuerDataAgainstReplay) builder.add(Settings.ProtectIssuerDataAgainstReplay)

    if (forbidPurgeWallet) builder.add(Settings.ForbidPurgeWallet)
    if (allowSelectBlockchain) builder.add(Settings.AllowSelectBlockchain)

    if (skipCheckPIN2andCVCIfValidatedByIssuer) builder.add(Settings.SkipCheckPin2andCvcIfValidatedByIssuer)
    if (skipSecurityDelayIfValidatedByIssuer) builder.add(Settings.SkipSecurityDelayIfValidatedByIssuer)

    if (skipSecurityDelayIfValidatedByLinkedTerminal) builder.add(Settings.SkipSecurityDelayIfValidatedByLinkedTerminal)
    if (restrictOverwriteIssuerDataEx) builder.add(Settings.RestrictOverwriteIssuerDataEx)

    if (requireTerminalTxSignature) builder.add(Settings.RequireTermTxSignature)

    if (requireTerminalCertSignature) builder.add(Settings.RequireTermCertSignature)

    if (checkPin3onCard) builder.add(Settings.CheckPIN3onCard)

    return builder.build()
}

internal fun CardConfig.createCardId(): String? {
    if (series == null) return null
    if (startNumber <= 0 || (series.length != 2 && series.length != 4)) return null

    val Alf = "ABCDEF0123456789"
    fun checkSeries(series: String): Boolean {
        val containsList = series.filter { Alf.contains(it) }
        return containsList.length == series.length
    }
    if (!checkSeries(series)) return null

    val tail = if (series.length == 2) String.format("%013d", startNumber) else String.format("%011d", startNumber)
    var cardId = (series + tail).replace(" ", "")
    if (cardId.length != 15 || Alf.indexOf(cardId[0]) == -1 || Alf.indexOf(cardId[1]) == -1)
        return null

    cardId += "0"
    val length = cardId.length
    var sum = 0
    for (i in 0 until length) {
        // get digits in reverse order
        var digit: Int
        val cDigit = cardId[length - i - 1]
        digit = if (cDigit in '0'..'9') cDigit - '0' else cDigit - 'A'

        // every 2nd number multiply with 2
        if (i % 2 == 1) digit *= 2
        sum += if (digit > 9) digit - 9 else digit
    }
    val lunh = (10 - sum % 10) % 10
    return cardId.substring(0, 15) + String.format("%d", lunh)
}

internal fun CardConfig.serializeNdef(ndefRecords: List<NdefRecord>): ByteArray {
    return NdefEncoder(ndefRecords, useDynamicNdef).encode()
}

internal fun CardConfig.serializeCardData(cardId: String, issuer: Issuer, manufacturer: Manufacturer): ByteArray {
    val tlvBuilder = TlvBuilder()
    tlvBuilder.append(TlvTag.Batch, cardData.batchId)
    tlvBuilder.append(TlvTag.ProductMask, cardData.productMask)
    tlvBuilder.append(TlvTag.ManufactureDateTime, cardData.manufactureDateTime)
    tlvBuilder.append(TlvTag.IssuerId, issuer.id)
    tlvBuilder.append(TlvTag.BlockchainId, cardData.blockchainName)

    if (cardData.tokenSymbol != null) {
        tlvBuilder.append(TlvTag.TokenSymbol, cardData.tokenSymbol)
        tlvBuilder.append(TlvTag.TokenContractAddress, cardData.tokenContractAddress)
        tlvBuilder.append(TlvTag.TokenDecimal, cardData.tokenDecimal)
    }
    tlvBuilder.append(
            TlvTag.CardIdManufacturerSignature,
            cardId.hexToBytes().sign(manufacturer.keyPair.privateKey)
    )
    return tlvBuilder.serialize()
}