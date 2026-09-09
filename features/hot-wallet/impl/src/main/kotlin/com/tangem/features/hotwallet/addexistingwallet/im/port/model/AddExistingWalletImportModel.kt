package com.tangem.features.hotwallet.addexistingwallet.im.port.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportComponent
import com.tangem.features.hotwallet.addexistingwallet.im.port.entity.AddExistingWalletImportUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class AddExistingWalletImportModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val mnemonicRepository: MnemonicRepository,
    private val hotWalletImporter: HotWalletImporter,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params: AddExistingWalletImportComponent.Params = paramsContainer.require()

    private val importSeedPhraseUiStateBuilder: ImportSeedPhraseUiStateBuilder

    private val passphraseInfoAlertBS
        get() = bottomSheetMessage {
            infoBlock {
                icon(R.drawable.ic_passcode_lock_56) {
                    type = MessageBottomSheetUM.Icon.Type.Accent
                    backgroundType = MessageBottomSheetUM.Icon.BackgroundType.SameAsTint
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
        analyticsEventHandler.send(OnboardingAnalyticsEvent.SeedPhrase.ImportSeedPhraseScreenOpened())
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
            onImportClick = { analyticsEventHandler.send(OnboardingAnalyticsEvent.SeedPhrase.ButtonImport()) },
        )
    }

    internal val uiState: StateFlow<AddExistingWalletImportUM>
        field = MutableStateFlow(importSeedPhraseUiStateBuilder.getState())

    private fun importWallet(mnemonic: Mnemonic, passphrase: String?) {
        modelScope.launch {
            setImportProgress(true)
            val result = hotWalletImporter.import(
                scope = modelScope,
                mnemonic = mnemonic,
                passphrase = passphrase?.toCharArray(),
                isSeedPhraseBackedUp = true,
            )
            setImportProgress(false)
            result.fold(
                ifLeft = { error ->
                    val message = when (error) {
                        HotWalletImportError.AlreadySaved -> resourceReference(
                            R.string.hw_import_seed_phrase_already_imported,
                        )
                        is HotWalletImportError.PassphraseTooLong -> resourceReference(
                            R.string.hw_import_seed_phrase_passphrase_too_long,
                            wrappedList(error.maxByteCount),
                        )
                        is HotWalletImportError.Unknown -> resourceReference(R.string.common_unknown_error)
                    }
                    uiMessageSender.send(SnackbarMessage(message))
                },
                ifRight = { userWalletId -> params.callbacks.onWalletImported(userWalletId) },
            )
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