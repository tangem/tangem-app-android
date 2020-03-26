package com.tangem.tangemtest.ucase.variants.personalize.converter

import com.tangem.tangemtest._arch.structure.*
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.impl.KeyValue
import com.tangem.tangemtest._arch.structure.impl.ListValueWrapper
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class ValueMapper {
    private val default: IdToValueAssociations = IdToValueAssociations()
    private val updatedByItemList: IdToValueAssociations = IdToValueAssociations()


    fun mapOnObject(itemList: List<Item>, defaultValues: PersonalizeConfig): PersonalizeConfig {
        default.init(defaultValues)
        updatedByItemList.init(defaultValues)
        startMapping(itemList)
        return createJson(defaultValues)
    }

    private fun startMapping(itemList: List<Item>) {
        itemList.forEach { item -> mapItem(item) }
    }

    private fun mapItem(item: Item) {
        when (item) {
            is Block -> startMapping(item.itemList)
            is BaseItem<*> -> {
                val value = updatedByItemList.get(item.id) ?: return
                value.set(item.viewModel.data)
            }
        }
    }

    private fun createJson(defaultConfig: PersonalizeConfig): PersonalizeConfig {
        val export = PersonalizeConfig()
        export.series = getTyped(CardNumber.SERIES, defaultConfig.series)
        export.startNumber = getTyped(CardNumber.NUMBER, defaultConfig.startNumber)
        export.curveID = getTyped(Common.CURVE, defaultConfig.curveID)
        export.blockchain = getTyped(Common.BLOCKCHAIN, defaultConfig.blockchain)
        export.blockchainCustom = getTyped(Common.BLOCKCHAIN_CUSTOM, defaultConfig.blockchainCustom)
        export.MaxSignatures = getTyped(Common.MAX_SIGNATURES, defaultConfig.MaxSignatures)
        export.createWallet = getTyped(Common.CREATE_WALLET, defaultConfig.createWallet)
        export.SigningMethod0 = getTyped(SigningMethod.SIGN_TX, defaultConfig.SigningMethod0)
        export.SigningMethod1 = getTyped(SigningMethod.SIGN_TX_RAW, defaultConfig.SigningMethod1)
        export.SigningMethod2 = getTyped(SigningMethod.SIGN_VALIDATED_TX, defaultConfig.SigningMethod2)
        export.SigningMethod3 = getTyped(SigningMethod.SIGN_VALIDATED_TX_RAW, defaultConfig.SigningMethod3)
        export.SigningMethod4 = getTyped(SigningMethod.SIGN_VALIDATED_TX_ISSUER, defaultConfig.SigningMethod4)
        export.SigningMethod5 = getTyped(SigningMethod.SIGN_VALIDATED_TX_RAW_ISSUER, defaultConfig.SigningMethod5)
        export.SigningMethod6 = getTyped(SigningMethod.SIGN_EXTERNAL, defaultConfig.SigningMethod6)
        export.pinLessFloorLimit = getTyped(SignHashExProp.PIN_LESS_FLOOR_LIMIT, defaultConfig.pinLessFloorLimit)
        export.hexCrExKey = getTyped(SignHashExProp.CRYPTO_EXTRACT_KEY, defaultConfig.hexCrExKey)
        export.requireTerminalCertSignature = getTyped(SignHashExProp.REQUIRE_TERMINAL_CERT_SIG, defaultConfig.requireTerminalCertSignature)
        export.requireTerminalTxSignature = getTyped(SignHashExProp.REQUIRE_TERMINAL_TX_SIG, defaultConfig.requireTerminalTxSignature)
        export.checkPIN3onCard = getTyped(SignHashExProp.CHECK_PIN3, defaultConfig.checkPIN3onCard)
        export.writeOnPersonalization = getTyped(Denomination.WRITE_ON_PERSONALIZE, defaultConfig.writeOnPersonalization)
        export.denomination = getTyped(Denomination.DENOMINATION, defaultConfig.denomination)
        export.itsToken = getTyped(Token.ITS_TOKEN, defaultConfig.itsToken)
        export.symbol = getTyped(Token.SYMBOL, defaultConfig.symbol)
        export.contractAddress = getTyped(Token.CONTRACT_ADDRESS, defaultConfig.contractAddress)
        export.decimal = getTyped(Token.DECIMAL, defaultConfig.decimal)
        export.cardData = export.cardData.apply { this.product_note = getTyped(ProductMask.NOTE, defaultConfig.cardData.product_note) }
        export.cardData = export.cardData.apply { this.product_tag = getTyped(ProductMask.TAG, defaultConfig.cardData.product_tag) }
        export.cardData = export.cardData.apply { this.product_id_card = getTyped(ProductMask.ID_CARD, defaultConfig.cardData.product_id_card) }
        export.isReusable = getTyped(SettingsMask.IS_REUSABLE, defaultConfig.isReusable)
        export.useActivation = getTyped(SettingsMask.NEED_ACTIVATION, defaultConfig.useActivation)
        export.forbidPurgeWallet = getTyped(SettingsMask.FORBID_PURGE, defaultConfig.forbidPurgeWallet)
        export.allowSelectBlockchain = getTyped(SettingsMask.ALLOW_SELECT_BLOCKCHAIN, defaultConfig.allowSelectBlockchain)
        export.useBlock = getTyped(SettingsMask.USE_BLOCK, defaultConfig.useBlock)
        export.useOneCommandAtTime = getTyped(SettingsMask.ONE_APDU, defaultConfig.useOneCommandAtTime)
        export.useCVC = getTyped(SettingsMask.USE_CVC, defaultConfig.useCVC)
        export.allowSwapPIN = getTyped(SettingsMask.ALLOW_SWAP_PIN, defaultConfig.allowSwapPIN)
        export.allowSwapPIN2 = getTyped(SettingsMask.ALLOW_SWAP_PIN2, defaultConfig.allowSwapPIN2)
        export.forbidDefaultPIN = getTyped(SettingsMask.FORBID_DEFAULT_PIN, defaultConfig.forbidDefaultPIN)
        export.smartSecurityDelay = getTyped(SettingsMask.SMART_SECURITY_DELAY, defaultConfig.smartSecurityDelay)
        export.protectIssuerDataAgainstReplay = getTyped(SettingsMask.PROTECT_ISSUER_DATA_AGAINST_REPLAY, defaultConfig.protectIssuerDataAgainstReplay)
        export.skipSecurityDelayIfValidatedByIssuer = getTyped(SettingsMask.SKIP_SECURITY_DELAY_IF_VALIDATED, defaultConfig.skipSecurityDelayIfValidatedByIssuer)
        export.skipCheckPIN2andCVCIfValidatedByIssuer = getTyped(SettingsMask.SKIP_PIN2_CVC_IF_VALIDATED, defaultConfig.skipCheckPIN2andCVCIfValidatedByIssuer)
        export.skipSecurityDelayIfValidatedByLinkedTerminal = getTyped(SettingsMask.SKIP_SECURITY_DELAY_ON_LINKED_TERMINAL, defaultConfig.skipSecurityDelayIfValidatedByLinkedTerminal)
        export.restrictOverwriteIssuerDataEx = getTyped(SettingsMask.RESTRICT_OVERWRITE_EXTRA_ISSUER_DATA, defaultConfig.restrictOverwriteIssuerDataEx)
        export.protocolAllowUnencrypted = getTyped(SettingsMaskProtocolEnc.ALLOW_UNENCRYPTED, defaultConfig.protocolAllowUnencrypted)
        export.allowFastEncryption = getTyped(SettingsMaskProtocolEnc.ALLOW_FAST_ENCRYPTION, defaultConfig.protocolAllowStaticEncryption)
        export.useNDEF = getTyped(SettingsMaskNdef.USE_NDEF, defaultConfig.useNDEF)
        export.useDynamicNDEF = getTyped(SettingsMaskNdef.DYNAMIC_NDEF, defaultConfig.useDynamicNDEF)
        export.disablePrecomputedNDEF = getTyped(SettingsMaskNdef.DISABLE_PRECOMPUTED_NDEF, defaultConfig.disablePrecomputedNDEF)
        export.NDEF = getTyped(SettingsMaskNdef.AAR, defaultConfig.NDEF)
        export.PIN = getTyped(Pins.PIN, defaultConfig.PIN)
        export.PIN2 = getTyped(Pins.PIN2, defaultConfig.PIN2)
        export.PIN3 = getTyped(Pins.PIN3, defaultConfig.PIN3)
        export.CVC = getTyped(Pins.CVC, defaultConfig.CVC)
        export.pauseBeforePIN2 = getTyped(Pins.PAUSE_BEFORE_PIN2, defaultConfig.pauseBeforePIN2)
        return export
    }

    private inline fun <reified Type> getTyped(id: Id, def: Type): Type {
        return getTypedBy<Type>(updatedByItemList, id) ?: getTypedBy<Type>(default, id)!!
    }

    private inline fun <reified Type> getTypedBy(associations: IdToValueAssociations, id: Id): Type? {
        Log.d(this, "getTyped for id: $id")
        var typedValue = associations.get(id)?.get()

        typedValue = when (typedValue) {
            is ListValueWrapper -> (typedValue.selectedItem as KeyValue).value as Type
            else -> typedValue as? Type ?: null
        }
        return typedValue
    }
}