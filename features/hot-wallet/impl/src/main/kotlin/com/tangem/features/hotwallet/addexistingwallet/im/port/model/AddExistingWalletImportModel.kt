package com.tangem.features.hotwallet.addexistingwallet.im.port.model

import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.components.bottomsheets.message.icon
import com.tangem.core.ui.components.bottomsheets.message.infoBlock
import com.tangem.core.ui.components.bottomsheets.message.onClick
import com.tangem.core.ui.components.bottomsheets.message.secondaryButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.domain.core.wallets.error.SaveWalletError
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

@Suppress("LongParameterList")
@ModelScoped
internal class AddExistingWalletImportModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val mnemonicRepository: MnemonicRepository,
    private val tangemHotSdk: TangemHotSdk,
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory,
    private val saveUserWalletUseCase: SaveWalletUseCase,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params: AddExistingWalletImportComponent.Params = paramsContainer.require()

    private val importSeedPhraseUiStateBuilder: ImportSeedPhraseUiStateBuilder

    private val passphraseInfoAlertBS
        get() = bottomSheetMessage {
            infoBlock {
                icon(R.drawable.ic_passcode_lock_56) {
                    type = MessageBottomSheetUMV2.Icon.Type.Accent
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.common_passphrase)
                body = resourceReference(R.string.onboarding_bottom_sheet_passphrase_description)
            }
            secondaryButton {
                text = resourceReference(R.string.common_got_it)
                onClick { closeBs() }
            }
        }

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
            onPassphraseInfoClick = ::onPassphraseInfoClick,
        )
    }

    internal val uiState: StateFlow<AddExistingWalletImportUM>
    field = MutableStateFlow(importSeedPhraseUiStateBuilder.getState())

    @Suppress("UnusedPrivateMember")
    private fun importWallet(mnemonic: Mnemonic, passphrase: String?) {
        modelScope.launch {
            setImportProgress(true)

            runCatching {
                val hotWalletId = tangemHotSdk.importWallet(mnemonic, passphrase?.toCharArray(), HotAuth.NoAuth)
                val hotUserWalletBuilder = hotUserWalletBuilderFactory.create(hotWalletId)
                val userWallet = hotUserWalletBuilder.build()
                saveUserWalletUseCase.invoke(userWallet.copy(backedUp = true))
                    .onLeft {
                        setImportProgress(false)
                        when (it) {
                            is SaveWalletError.DataError -> Timber.e(it.toString(), "Unable to save user wallet")
                            is SaveWalletError.WalletAlreadySaved -> {
                                uiMessageSender.send(
                                    SnackbarMessage(resourceReference(R.string.hw_import_seed_phrase_already_imported)),
                                )
                            }
                        }
                    }
                    .onRight {
                        setImportProgress(false)
                        params.callbacks.onWalletImported(userWallet.walletId)
                    }
            }.onFailure {
                Timber.e(it)
                setImportProgress(false)
            }
        }
    }

    private fun setImportProgress(progress: Boolean) {
        uiState.update {
            it.copy(importWalletProgress = progress)
        }
    }

    private fun onPassphraseInfoClick() {
        uiMessageSender.send(passphraseInfoAlertBS)
    }
}