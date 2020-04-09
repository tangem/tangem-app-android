package com.tangem.tangemtest.ucase.variants.responses.converter

import com.tangem.commands.Card
import com.tangem.commands.CardData
import com.tangem.commands.Settings
import com.tangem.commands.SettingsMask
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.StringId
import com.tangem.tangemtest._arch.structure.abstraction.*
import com.tangem.tangemtest._arch.structure.impl.BoolItem
import com.tangem.tangemtest._arch.structure.impl.TextItem
import com.tangem.tangemtest.ucase.variants.personalize.BlockId
import com.tangem.tangemtest.ucase.variants.responses.CardDataId
import com.tangem.tangemtest.ucase.variants.responses.CardId
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
class CardConverter : ModelToItems<Card> {
    override fun convert(from: Card): List<Item> {
        val itemList = mutableListOf<Item>()
//        val holder = GsonInitializer()
//        itemList.add(TextItem(Additional.JSON_INCOMING, holder.gson.toJson(from)))

        itemList.add(simpleFields(from))
        itemList.add(cardData(from.cardData))
        itemList.add(settingsMask(from.settingsMask))
        hideEmptyNullFields(itemList)

        return itemList
    }

    private fun hideEmptyNullFields(itemList: MutableList<Item>) {
        itemList.iterate {
            val data = it.getData<Any?>()
            val isHidden = if (data == null) true
            else when (data) {
                is String -> data.isEmpty()
                else -> false
            }

            if (isHidden) it.viewModel.viewState.isHidden = true
        }
    }

    private fun simpleFields(from: Card): Item {
        val group = createGroup(BlockId.Common)
        group.addItem(TextItem(CardId.cardId, from.cardId))
        group.addItem(TextItem(CardId.manufacturerName, from.manufacturerName))
        group.addItem(TextItem(CardId.status, stringOf(from.status)))
        group.addItem(TextItem(CardId.firmwareVersion, from.firmwareVersion))
        group.addItem(TextItem(CardId.cardPublicKey, stringOf(from.cardPublicKey)))
        group.addItem(TextItem(CardId.issuerPublicKey, stringOf(from.issuerPublicKey)))
        group.addItem(TextItem(CardId.curve, stringOf(from.curve)))
        group.addItem(TextItem(CardId.maxSignatures, stringOf(from.maxSignatures)))
        group.addItem(TextItem(CardId.signingMethod, stringOf(from.signingMethod)))
        group.addItem(TextItem(CardId.pauseBeforePin2, stringOf(from.pauseBeforePin2)))
        group.addItem(TextItem(CardId.walletPublicKey, stringOf(from.walletPublicKey)))
        group.addItem(TextItem(CardId.walletRemainingSignatures, stringOf(from.walletRemainingSignatures)))
        group.addItem(TextItem(CardId.walletSignedHashes, stringOf(from.walletSignedHashes)))
        group.addItem(TextItem(CardId.health, stringOf(from.health)))
        group.addItem(TextItem(CardId.isActivated, stringOf(from.isActivated)))
        group.addItem(TextItem(CardId.activationSeed, stringOf(from.activationSeed)))
        group.addItem(TextItem(CardId.paymentFlowVersion, stringOf(from.paymentFlowVersion)))
        group.addItem(TextItem(CardId.userCounter, stringOf(from.userCounter)))
//        block.addItem(TextItem(CardId.UserProtectedCounter, stringOf(from.userProtectedCounter)))
        return group
    }

    private fun cardData(from: CardData?): Item {
        val group = createGroup(BlockId.Common, R.color.group_card_data)
        val data = from ?: return group

//        group.addItem(TextItem(StringResId(R.string.response_card_card_data)))
        group.addItem(TextItem(CardDataId.batchId, data.batchId))
//        Format: Year (2 bytes) | Month (1 byte) | Day (1 byte)
        group.addItem(TextItem(CardDataId.manufactureDateTime, stringOf(data.manufactureDateTime)))
        group.addItem(TextItem(CardDataId.issuerName, data.issuerName))
        group.addItem(TextItem(CardDataId.blockchainName, data.blockchainName))
        group.addItem(TextItem(CardDataId.manufacturerSignature, stringOf(data.manufacturerSignature)))
        group.addItem(TextItem(CardDataId.productMask, stringOf(data.productMask)))
        group.addItem(TextItem(CardDataId.tokenSymbol, data.tokenSymbol))
        group.addItem(TextItem(CardDataId.tokenContractAddress, data.tokenContractAddress))
        group.addItem(TextItem(CardDataId.tokenDecimal, data.tokenSymbol))

        return group
    }

    private fun settingsMask(from: SettingsMask?): Item {
        val group = createGroup(CardId.settingsMask, R.color.group_signing_method)
        val data = from ?: return group

//        group.addItem(TextItem(StringResId(R.string.response_card_settings_mask)))
        Settings.values().forEach { group.addItem(BoolItem(StringId(it.name), data.contains(it))) }
        return group
    }

    private fun createGroup(id: Id, colorId: Int? = null): ItemGroup {
        return if (colorId == null) SimpleItemGroup(id)
        else SimpleItemGroup(id, BaseItemViewModel(viewState = ViewState(backgroundColor = colorId)))
    }
}