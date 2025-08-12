package com.tangem.features.hotwallet.manualbackup.check.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UpdateWalletUseCase
import com.tangem.features.hotwallet.manualbackup.check.ManualBackupCheckComponent
import com.tangem.features.hotwallet.manualbackup.check.entity.ManualBackupCheckUM
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.all
import kotlin.collections.map

@Stable
@ModelScoped
internal class ManualBackupCheckModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val tangemHotSdk: TangemHotSdk,
) : Model() {

    private val params = paramsContainer.require<ManualBackupCheckComponent.Params>()
    private val callbacks = params.callbacks

    internal val uiState: StateFlow<ManualBackupCheckUM>
    field = MutableStateFlow(getInitialUIState())

    init {
        modelScope.launch {
            runCatching {
                val userWallet = getUserWalletUseCase(params.userWalletId)
                    .getOrElse { error("User wallet with id ${params.userWalletId} not found") }
                if (userWallet is UserWallet.Hot) {
                    val unlockHotWallet = UnlockHotWallet(userWallet.hotWalletId, HotAuth.NoAuth)
                    val seedPhrasePrivateInfo = tangemHotSdk.exportMnemonic(unlockHotWallet)
                    uiState.update {
                        it.copy(
                            words = seedPhrasePrivateInfo.mnemonic.mnemonicComponents.filterIndexed { index, _ ->
                                WORD_FIELD_INDICES.contains(index + 1)
                            }.toImmutableList(),
                        )
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    @Suppress("MagicNumber")
    private fun getInitialUIState(): ManualBackupCheckUM {
        val wordFields = List(WORD_FIELD_INDICES.size) { index ->
            val shownIndex = WORD_FIELD_INDICES[index]

            ManualBackupCheckUM.WordField(
                index = shownIndex,
                word = TextFieldValue(""),
                error = false,
                onChange = { textFieldValue ->
                    updateWordField(shownIndex, textFieldValue)
                },
            )
        }.toImmutableList()

        return ManualBackupCheckUM(
            wordFields = wordFields,
            completeButtonEnabled = false,
            completeButtonProgress = false,
            onCompleteButtonClick = {
                val currentUIState = uiState.value
                if (currentUIState.completeButtonEnabled) {
                    backupWallet()
                }
            },
        )
    }

    private fun backupWallet() {
        modelScope.launch {
            uiState.update {
                it.copy(completeButtonProgress = true)
            }

            runCatching {
                val userWallet = getUserWalletUseCase(params.userWalletId)
                    .getOrElse { error("User wallet with id ${params.userWalletId} not found") }
                if (userWallet is UserWallet.Hot) {
                    updateWalletUseCase(userWallet.walletId) {
                        userWallet.copy(backedUp = true)
                    }
                    callbacks.onCompleteClick()
                }
                uiState.update {
                    it.copy(completeButtonProgress = false)
                }
            }.onFailure {
                Timber.e(it)

                uiState.update {
                    it.copy(completeButtonProgress = false)
                }
            }
        }
    }

    private fun updateWordField(shownIndex: Int, newText: TextFieldValue) {
        uiState.update { currentState ->
            val updatedFields = currentState.wordFields.map { wordField ->
                if (wordField.index == shownIndex) {
                    val isCorrect = checkWordField(newText.text, shownIndex)
                    wordField.copy(
                        word = newText,
                        error = !isCorrect,
                    )
                } else {
                    wordField
                }
            }.toImmutableList()

            val allFieldsCorrect = updatedFields.all { field ->
                checkWordField(field.word.text, field.index)
            }

            currentState.copy(
                wordFields = updatedFields,
                completeButtonEnabled = allFieldsCorrect,
            )
        }
    }

    private fun checkWordField(word: String, shownIndex: Int): Boolean {
        val words = uiState.value.words
        val listIndex = WORD_FIELD_INDICES.indexOf(shownIndex)
        return words.getOrNull(listIndex)?.let { word == it } == true
    }

    companion object {
        private val WORD_FIELD_INDICES = listOf(2, 7, 11)
    }
}