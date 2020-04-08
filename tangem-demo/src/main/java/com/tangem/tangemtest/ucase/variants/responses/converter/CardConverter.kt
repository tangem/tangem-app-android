package com.tangem.tangemtest.ucase.variants.responses.converter

import com.tangem.commands.Card
import com.tangem.commands.Settings
import com.tangem.commands.SettingsMask
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.StringId
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ListItemBlock
import com.tangem.tangemtest._arch.structure.abstraction.ModelToItems
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
        itemList.add(cardData(from))
        itemList.add(settingsMask(from.settingsMask))
        return itemList
    }

    private fun simpleFields(from: Card): Item {
        val block = createBlock(BlockId.Common)
        block.addItem(TextItem(CardId.cardId, from.cardId))
        block.addItem(TextItem(CardId.manufacturerName, from.manufacturerName))
        block.addItem(TextItem(CardId.status, stringOf(from.status)))
        block.addItem(TextItem(CardId.firmwareVersion, from.firmwareVersion))
        block.addItem(TextItem(CardId.cardPublicKey, stringOf(from.cardPublicKey)))
        block.addItem(TextItem(CardId.issuerPublicKey, stringOf(from.issuerPublicKey)))
        block.addItem(TextItem(CardId.curve, stringOf(from.curve)))
        block.addItem(TextItem(CardId.maxSignatures, stringOf(from.maxSignatures)))
        block.addItem(TextItem(CardId.signingMethod, stringOf(from.signingMethod)))
        block.addItem(TextItem(CardId.pauseBeforePin2, stringOf(from.pauseBeforePin2)))
        block.addItem(TextItem(CardId.walletPublicKey, stringOf(from.walletPublicKey)))
        block.addItem(TextItem(CardId.walletRemainingSignatures, stringOf(from.walletRemainingSignatures)))
        block.addItem(TextItem(CardId.walletSignedHashes, stringOf(from.walletSignedHashes)))
        block.addItem(TextItem(CardId.health, stringOf(from.health)))
        block.addItem(TextItem(CardId.isActivated, stringOf(from.isActivated)))
        block.addItem(TextItem(CardId.activationSeed, stringOf(from.activationSeed)))
        block.addItem(TextItem(CardId.paymentFlowVersion, stringOf(from.paymentFlowVersion)))
        block.addItem(TextItem(CardId.userCounter, stringOf(from.userCounter)))
//        block.addItem(TextItem(CardId.UserProtectedCounter, stringOf(from.userProtectedCounter)))
        return block
    }

    private fun cardData(from: Card): Item {
        val block = createBlock(BlockId.Common)
        val data = from.cardData ?: return block

        val itemList = block.itemList
        itemList.add(TextItem(CardDataId.batchId, data.batchId))
//        Format: Year (2 bytes) | Month (1 byte) | Day (1 byte)
        itemList.add(TextItem(CardDataId.manufactureDateTime, stringOf(data.manufactureDateTime)))
        itemList.add(TextItem(CardDataId.issuerName, data.issuerName))
        itemList.add(TextItem(CardDataId.blockchainName, data.blockchainName))
        itemList.add(TextItem(CardDataId.manufacturerSignature, stringOf(data.manufacturerSignature)))
        itemList.add(TextItem(CardDataId.productMask, stringOf(data.productMask)))
        itemList.add(TextItem(CardDataId.tokenSymbol, data.tokenSymbol))
        itemList.add(TextItem(CardDataId.tokenContractAddress, data.tokenContractAddress))
        itemList.add(TextItem(CardDataId.tokenDecimal, data.tokenSymbol))

        return block
    }

    private fun settingsMask(from: SettingsMask?): Item {
        val block = createBlock(CardId.settingsMask)
        val data = from ?: return block

        Settings.values().forEach { block.addItem(BoolItem(StringId(it.name), data.contains(it))) }
        return block
    }

    private fun createBlock(id: Id): ListItemBlock = ListItemBlock(id).apply { addItem(TextItem(id)) }
}