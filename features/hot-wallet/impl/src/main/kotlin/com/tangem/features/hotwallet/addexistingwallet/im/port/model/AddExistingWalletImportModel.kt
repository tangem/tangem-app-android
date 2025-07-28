package com.tangem.features.hotwallet.addexistingwallet.im.port.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportComponent
import com.tangem.features.hotwallet.addexistingwallet.im.port.entity.AddExistingWalletImportUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class AddExistingWalletImportModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val mnemonicRepository: MnemonicRepository,
) : Model() {

    private val params: AddExistingWalletImportComponent.Params = paramsContainer.require()

    private val importSeedPhraseUiStateBuilder: ImportSeedPhraseUiStateBuilder

    init {
        importSeedPhraseUiStateBuilder = ImportSeedPhraseUiStateBuilder(
            modelScope = modelScope,
            mnemonicRepository = mnemonicRepository,
            updateUiState = { block -> uiState.update { block(it) } },
            readyToImport = { ready -> uiState.update { it.copy(readyToImport = ready) } },
            importWallet = { mnemonic: Mnemonic, passphrase: String? ->
                importWallet(
                    mnemonic = mnemonic,
                    passphrase = passphrase,
                )
            },
        )
    }

    internal val uiState: StateFlow<AddExistingWalletImportUM>
    field = MutableStateFlow(importSeedPhraseUiStateBuilder.getState())

    @Suppress("UnusedPrivateMember")
    private fun importWallet(mnemonic: Mnemonic, passphrase: String?) {
        // TODO implement importing seed phrase
        params.callbacks.onWalletImported()
    }
}