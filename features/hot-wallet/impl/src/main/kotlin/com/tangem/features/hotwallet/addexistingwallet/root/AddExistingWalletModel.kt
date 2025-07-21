package com.tangem.features.hotwallet.addexistingwallet.root

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.hotwallet.setaccesscode.SetAccessCodeComponent
import com.tangem.features.hotwallet.addexistingwallet.start.AddExistingWalletStartComponent
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportComponent
import com.tangem.features.hotwallet.addexistingwallet.root.routing.AddExistingWalletRoute
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@ModelScoped
internal class AddExistingWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    val addExistingWalletStartModelCallbacks = AddExistingWalletStartModelCallbacks()
    val addExistingWalletImportModelCallbacks = AddExistingWalletImportModelCallbacks()
    val pushNotificationsComponentModelCallbacks = PushNotificationsComponentModelCallbacks()
    val accessCodeModelCallbacks = AccessCodeModelCallbacks()

    val stackNavigation = StackNavigation<AddExistingWalletRoute>()

    inner class AddExistingWalletStartModelCallbacks : AddExistingWalletStartComponent.ModelCallbacks {
        override fun onBackClick() {
            router.pop()
        }

        override fun onImportPhraseClick() {
            stackNavigation.push(AddExistingWalletRoute.Import)
        }
    }

    inner class AddExistingWalletImportModelCallbacks : AddExistingWalletImportComponent.ModelCallbacks {
        override fun onBackClick() {
            stackNavigation.pop()
        }
    }

    inner class PushNotificationsComponentModelCallbacks : PushNotificationsComponent.ModelCallbacks {
        override fun onResult() {
            // TODO [REDACTED_TASK_KEY]
        }
    }

    inner class AccessCodeModelCallbacks : SetAccessCodeComponent.ModelCallbacks {
        override fun onBackClick() {
            // TODO [REDACTED_TASK_KEY]
        }

        override fun onAccessCodeSet() {
            // TODO [REDACTED_TASK_KEY]
        }
    }
}