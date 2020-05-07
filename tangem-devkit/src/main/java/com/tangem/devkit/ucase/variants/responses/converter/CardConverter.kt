package com.tangem.devkit.ucase.variants.responses.converter

import com.tangem.commands.*
import com.tangem.devkit.R
import com.tangem.devkit._arch.structure.StringId
import com.tangem.devkit._arch.structure.abstraction.Item
import com.tangem.devkit._arch.structure.abstraction.ItemGroup
import com.tangem.devkit._arch.structure.abstraction.iterate
import com.tangem.devkit._arch.structure.impl.BoolItem
import com.tangem.devkit._arch.structure.impl.TextItem
import com.tangem.devkit.ucase.variants.responses.CardDataId
import com.tangem.devkit.ucase.variants.responses.CardId
import com.tangem.devkit.ucase.variants.responses.item.TextHeaderItem

/**
[REDACTED_AUTHOR]
 */
class CardConverter : BaseResponseConverter<Card>() {

    override fun convert(from: Card): List<Item> {
        val itemList = mutableListOf<Item>()
        commonGroup(itemList, from)
        cardDataGroup(itemList, from.cardData)
        settingsMaskGroup(itemList, from.settingsMask)
        hideEmptyNullFields(itemList)

        return itemList
    }

    private fun hideEmptyNullFields(itemList: MutableList<Item>) {
        itemList.iterate {
            if (it is ItemGroup) return@iterate

            if (it.getData<Any?>() == null) it.viewModel.viewState.isVisibleState.value = false
        }
    }

    private fun commonGroup(itemList: MutableList<Item>, from: Card) {
        val group = createGroup(CardId.empty, addHeaderItem = false)
        itemList.add(group)

        group.addItem(TextItem(CardId.cardId, from.cardId))
        group.addItem(TextItem(CardId.manufacturerName, from.manufacturerName))
        group.addItem(TextItem(CardId.status, valueToString(from.status)))
        group.addItem(TextItem(CardId.firmwareVersion, from.firmwareVersion))
        group.addItem(TextItem(CardId.cardPublicKey, fieldConverter.byteArrayToHex(from.cardPublicKey)))
        group.addItem(TextItem(CardId.issuerPublicKey, fieldConverter.byteArrayToHex(from.issuerPublicKey)))
        group.addItem(TextItem(CardId.curve, valueToString(from.curve)))
        group.addItem(TextItem(CardId.maxSignatures, valueToString(from.maxSignatures)))
        group.addItem(TextItem(CardId.pauseBeforePin2, valueToString(from.pauseBeforePin2)))
        group.addItem(TextItem(CardId.walletPublicKey, fieldConverter.byteArrayToHex(from.walletPublicKey)))
        group.addItem(TextItem(CardId.walletRemainingSignatures, valueToString(from.walletRemainingSignatures)))
        group.addItem(TextItem(CardId.walletSignedHashes, valueToString(from.walletSignedHashes)))
        group.addItem(TextItem(CardId.health, valueToString(from.health)))
        group.addItem(TextItem(CardId.isActivated, valueToString(from.isActivated)))
        group.addItem(TextItem(CardId.activationSeed, valueToString(from.activationSeed)))
        group.addItem(TextItem(CardId.paymentFlowVersion, valueToString(from.paymentFlowVersion)))
        group.addItem(TextItem(CardId.userCounter, valueToString(from.userCounter)))

        val signingMethodMask = from.signingMethods ?: return

        group.addItem(TextHeaderItem(CardId.signingMethod, ""))
        SigningMethod.values().forEach { group.addItem(BoolItem(StringId(it.name), signingMethodMask.contains(it))) }
    }

    private fun cardDataGroup(itemList: MutableList<Item>, cardData: CardData?) {
        val data = cardData ?: return

        val group = createGroup(CardId.cardData, R.color.group_card_data)
        itemList.add(group)
        group.addItem(TextItem(CardDataId.batchId, data.batchId))
        group.addItem(TextItem(CardDataId.manufactureDateTime, valueToString(data.manufactureDateTime)))
        group.addItem(TextItem(CardDataId.issuerName, data.issuerName))
        group.addItem(TextItem(CardDataId.blockchainName, data.blockchainName))
        group.addItem(TextItem(CardDataId.manufacturerSignature, fieldConverter.byteArrayToHex(data.manufacturerSignature)))
        group.addItem(TextItem(CardDataId.tokenSymbol, data.tokenSymbol))
        group.addItem(TextItem(CardDataId.tokenContractAddress, data.tokenContractAddress))
        group.addItem(TextItem(CardDataId.tokenDecimal, data.tokenSymbol))

        val productMask = data.productMask ?: return

        group.addItem(TextHeaderItem(CardDataId.productMask, ""))
        Product.values().forEach { group.addItem(BoolItem(StringId(it.name), productMask.contains(it))) }
    }

    private fun settingsMaskGroup(itemList: MutableList<Item>, from: SettingsMask?) {
        val data = from ?: return

        val group = createGroup(CardId.settingsMask, R.color.group_settings_mask)
        itemList.add(group)

        Settings.values().forEach { group.addItem(BoolItem(StringId(it.name), data.contains(it))) }
    }
}