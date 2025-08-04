package com.tangem.features.hotwallet.addexistingwallet.im.port.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportComponent
import com.tangem.features.hotwallet.addexistingwallet.im.port.entity.AddExistingWalletImportUM
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
internal class AddExistingWalletImportModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val mnemonicRepository: MnemonicRepository,
    private val tangemHotSdk: TangemHotSdk,
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory,
    private val saveUserWalletUseCase: SaveWalletUseCase,
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
        modelScope.launch {
            uiState.update {
                it.copy(createWalletProgress = true)
            }

            runCatching {
                val hotWalletId = tangemHotSdk.importWallet(mnemonic, passphrase?.toCharArray(), HotAuth.NoAuth)
                val hotUserWalletBuilder = hotUserWalletBuilderFactory.create(hotWalletId)
                val userWallet = hotUserWalletBuilder.build()
                saveUserWalletUseCase(userWallet)
                params.callbacks.onWalletImported(userWallet.walletId)
            }.onFailure {
                Timber.e(it)

                uiState.update {
                    it.copy(createWalletProgress = false)
                }
            }
        }
    }
}