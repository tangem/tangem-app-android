package com.tangem.features.hotwallet.manualbackup.check.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.hotwallet.manualbackup.check.ManualBackupCheckComponent
import com.tangem.features.hotwallet.manualbackup.check.entity.ManualBackupCheckUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.all
import kotlin.collections.map
import kotlin.error

@Stable
@ModelScoped
internal class ManualBackupCheckModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<ManualBackupCheckComponent.Params>()
    private val callbacks = params.callbacks

    internal val uiState: StateFlow<ManualBackupCheckUM>
    field = MutableStateFlow(getInitialUIState())

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
                    callbacks.onCompleteClick()
                }
            },
        )
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
        val generatedWords = params.generatedWords
        val wordList = generatedWords.mnemonicComponents

        return if (shownIndex <= wordList.size) {
            wordList[shownIndex - 1] == word
        } else {
            false
        }
    }

    companion object {
        private val WORD_FIELD_INDICES = listOf(2, 7, 11)
    }
}