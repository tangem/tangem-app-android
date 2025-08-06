package com.tangem.features.account.createedit

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.features.account.AccountCreateEditComponent
import com.tangem.features.account.createedit.entity.AccountCreateEditUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class AccountCreateEditModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val messageSender: UiMessageSender,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<AccountCreateEditComponent.Params>()

    val uiState: StateFlow<AccountCreateEditUM>
    field = MutableStateFlow(TODO())

    init {
        params
    }

    fun unsaveChangeDialog() {
        val firstAction = EventMessageAction(
            title = TextReference.EMPTY, // todo account res
            onClick = {},
        )
        val secondAction = EventMessageAction(
            title = TextReference.EMPTY, // todo account res
            onClick = { router.pop() },
        )
        messageSender.send(
            DialogMessage(
                title = TextReference.EMPTY, // todo account res
                message = TextReference.EMPTY, // todo account res
                firstActionBuilder = { firstAction },
                secondActionBuilder = { secondAction },
            ),
        )
    }
}