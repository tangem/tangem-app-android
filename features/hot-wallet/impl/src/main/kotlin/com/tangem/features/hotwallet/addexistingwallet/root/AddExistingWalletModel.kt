package com.tangem.features.hotwallet.addexistingwallet.root

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.addexistingwallet.start.AddExistingWalletStartComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@ModelScoped
internal class AddExistingWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val addExistingWalletStartModelCallbacks = AddExistingWalletStartModelCallbacks()

    inner class AddExistingWalletStartModelCallbacks : AddExistingWalletStartComponent.ModelCallbacks {
        override fun onBackClick() {
            // TODO implement in [REDACTED_TASK_KEY]
        }

        override fun onImportPhraseClick() {
            // TODO implement in [REDACTED_TASK_KEY]
        }
    }
}