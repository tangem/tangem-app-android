package com.tangem.features.hotwallet.updateaccesscode

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.hotwallet.UpdateAccessCodeComponent
import com.tangem.features.hotwallet.updateaccesscode.routing.UpdateAccessCodeRoute
import com.tangem.features.hotwallet.accesscode.AccessCodeComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@ModelScoped
internal class UpdateAccessCodeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    paramsContainer: ParamsContainer,
) : Model(), AccessCodeComponent.ModelCallbacks {

    private val params = paramsContainer.require<UpdateAccessCodeComponent.Params>()

    val stackNavigation = StackNavigation<UpdateAccessCodeRoute>()
    val startRoute: UpdateAccessCodeRoute = UpdateAccessCodeRoute.SetAccessCode(params.userWalletId)
    val currentRoute: MutableStateFlow<UpdateAccessCodeRoute> = MutableStateFlow(startRoute)

    fun onChildBack() {
        when (currentRoute.value) {
            is UpdateAccessCodeRoute.SetAccessCode -> router.pop()
            is UpdateAccessCodeRoute.ConfirmAccessCode -> stackNavigation.pop()
        }
    }

    override fun onNewAccessCodeInput(userWalletId: UserWalletId, accessCode: String) {
        stackNavigation.push(UpdateAccessCodeRoute.ConfirmAccessCode(userWalletId, accessCode))
    }

    override fun onAccessCodeUpdated(userWalletId: UserWalletId) {
        router.pop()
    }
}