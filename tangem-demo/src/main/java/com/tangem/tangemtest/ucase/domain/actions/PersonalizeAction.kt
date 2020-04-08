package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.findDataItem
import com.tangem.tangemtest._arch.structure.impl.EditTextItem
import com.tangem.tangemtest._arch.structure.impl.NumberItem
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.ItemError
import com.tangem.tangemtest.ucase.variants.personalize.CardNumber
import com.tangem.tangemtest.ucase.variants.personalize.converter.PersonalizationConfigConverter
import com.tangem.tangemtest.ucase.variants.personalize.converter.fromTo.PersonalizeConfigToCardConfig
import com.tangem.tangemtest.ucase.variants.personalize.dto.DefaultPersonalizationParams
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizationConfig
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
class PersonalizeAction : BaseAction() {
    override fun executeMainAction(payload: PayloadHolder, attrs: AttrForAction, callback: ActionCallback) {
        val itemList = attrs.payload[PayloadKey.itemList] as? List<Item> ?: return
        val actionView = attrs.payload[PayloadKey.actionView] as? ActionView ?: return

        if (!checkSeries(itemList)) {
            actionView.showSnackbar(ItemError.BadSeries)
            return
        }
        if (!checkNumber(itemList)) {
            actionView.showSnackbar(ItemError.BadCardNumber)
            return
        }

        val issuer = DefaultPersonalizationParams.issuer()
        val acquirer = DefaultPersonalizationParams.acquirer()
        val manufacturer = DefaultPersonalizationParams.manufacturer()

        val personalizeConfig = PersonalizationConfigConverter().convert(itemList, PersonalizationConfig())
        val cardConfig = PersonalizeConfigToCardConfig().convert(personalizeConfig)

        attrs.cardManager.personalize(cardConfig, issuer, manufacturer, acquirer) {
            handleResult(payload, it, null, attrs, callback)
        }
    }

    private fun checkSeries(itemList: List<Item>): Boolean {
        val seriesItem = itemList.findDataItem(CardNumber.Series) as? EditTextItem ?: return false
        val data = seriesItem.getData() ?: return false

        seriesItem.setData(data.toUpperCase())
        return data.length == 2 || data.length == 4
    }

    private fun checkNumber(itemList: List<Item>): Boolean {
        val seriesItem = itemList.findDataItem(CardNumber.Series) as? EditTextItem ?: return false
        val numberItem = itemList.findDataItem(CardNumber.Number) as? NumberItem ?: return false
        val seriesData = seriesItem.getData() ?: return false
        val numberData = stringOf(numberItem.getData())

        return if (seriesData.length == 2 && numberData.length > 13) false
        else !(seriesData.length == 4 && numberData.length > 11)
    }
}