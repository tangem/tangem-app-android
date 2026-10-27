package com.tangem.feature.wallet.child.wallet.model.intents

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_28
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

internal interface MoneyAccountIntents {

    fun onMoneyAccountActivatingClicked()

    fun onMoneyAccountClicked(userWalletId: UserWalletId)
}

@ModelScoped
internal class MoneyAccountClickIntentsImplementor @Inject constructor(
    private val uiMessageSender: UiMessageSender,
) : BaseWalletClickIntents(), MoneyAccountIntents {

    override fun onMoneyAccountActivatingClicked() {
        val bottomSheet = bottomSheetMessage {
            infoBlock {
                vector(Icons.ic_clock_28) {
                    type = MessageBottomSheetUM.Vector.Type.Informative
                    backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Informative
                }
                title = resourceReference(R.string.money_account_main_status_dialog_activating_title)
                body = resourceReference(R.string.money_account_main_status_dialog_activating_description)
            }
            secondaryButton {
                text = resourceReference(R.string.common_got_it)
                onClick { closeBs() }
            }
        }

        uiMessageSender.send(bottomSheet)
    }

    override fun onMoneyAccountClicked(userWalletId: UserWalletId) {
        router.openMoneyAccountDetails(userWalletId = userWalletId)
    }
}