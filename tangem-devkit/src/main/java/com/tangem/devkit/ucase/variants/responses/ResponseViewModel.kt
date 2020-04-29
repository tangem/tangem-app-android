package com.tangem.devkit.ucase.variants.responses

import android.view.View
import androidx.lifecycle.ViewModel
import com.tangem.commands.*
import com.tangem.commands.personalization.DepersonalizeResponse
import com.tangem.devkit.R
import com.tangem.devkit._arch.structure.abstraction.Item
import com.tangem.devkit._arch.structure.abstraction.ModelToItems
import com.tangem.devkit._arch.structure.abstraction.iterate
import com.tangem.devkit.ucase.variants.responses.converter.ConvertersStore
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class ResponseViewModel : ViewModel() {

    private val convertersHolder = ConvertersStore()
    private var itemList: List<Item>? = null

    fun createItemList(response: CommandResponse?): List<Item> {
        Log.d(this, "createItemList: itemList size: ${itemList?.size ?: 0}")

        val responseEvent = response ?: return emptyList()
        val type = responseEvent::class.java
        val converter = convertersHolder.get(type) as? ModelToItems<Any> ?: return emptyList()
        itemList = converter.convert(responseEvent)
        return itemList!!
    }

    fun toggleDescriptionVisibility(state: Boolean) {
        itemList?.iterate {
            it.viewModel.viewState.descriptionVisibility.value = if (state) View.VISIBLE else View.GONE
        }
    }

    fun determineTitleId(response: CommandResponse?): Int {
        val responseEvent = response ?: return R.string.unknown

        return when (responseEvent) {
            is Card -> R.string.fg_name_response_personalization
            is SignResponse -> R.string.fg_name_response_sign
            is DepersonalizeResponse -> R.string.fg_name_response_depersonalization
            is CreateWalletResponse -> R.string.fg_name_response_create_wallet
            is PurgeWalletResponse -> R.string.fg_name_response_purge_wallet
            is ReadIssuerDataResponse -> R.string.fg_name_response_read_issuer_data
            is WriteIssuerDataResponse -> R.string.fg_name_response_write_issuer_data
            is ReadIssuerExtraDataResponse -> R.string.fg_name_response_read_issuer_extra_data
            is ReadUserDataResponse -> R.string.fg_name_response_read_user_data
            is WriteUserDataResponse -> R.string.fg_name_response_write_user_data

            else -> R.string.unknown
        }
    }
}




