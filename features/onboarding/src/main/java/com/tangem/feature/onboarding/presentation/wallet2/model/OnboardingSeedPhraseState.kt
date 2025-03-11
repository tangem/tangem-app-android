package com.tangem.feature.onboarding.presentation.wallet2.model

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.feature.onboarding.domain.SeedPhraseError
import com.tangem.feature.onboarding.domain.models.MnemonicType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
[REDACTED_AUTHOR]
 */
data class OnboardingSeedPhraseState(
    val introState: IntroState,
    val aboutState: AboutState,
    val yourSeedPhraseState: YourSeedPhraseState,
    val checkSeedPhraseState: CheckSeedPhraseState,
    val importSeedPhraseState: ImportSeedPhraseState,
    val menuButtonChat: ButtonState,
    val onBackClick: () -> Unit,
)

data class IntroState(
    val cardImageUrl: String? = null,
    val buttonCreateWallet: ButtonState,
    val buttonOtherOptions: ButtonState,
)

data class AboutState(
    val buttonReadMoreAboutSeedPhrase: ButtonState,
    val buttonGenerateSeedPhrase: ButtonState,
    val buttonImportSeedPhrase: ButtonState,
)

data class YourSeedPhraseState(
    val segmentSeedState: SegmentSeedState,
    val mnemonicGridItems: PersistentList<MnemonicGridItem> = persistentListOf(),
    val buttonContinue: ButtonState,
)

data class SegmentSeedState(
    val seedSegments: PersistentList<SegmentSeedType>,
    val selectedSeedType: SegmentSeedType,
    val onSelectType: (SegmentSeedType) -> Unit,
)

enum class SegmentSeedType(val count: Int) {
    SEED_12(count = 12), SEED_24(count = 24);

    fun toMnemonicType(): MnemonicType {
        return when (this) {
            SEED_12 -> MnemonicType.Mnemonic12
            SEED_24 -> MnemonicType.Mnemonic24
        }
    }

    companion object {
        fun fromMnemonicType(mnemonicType: MnemonicType): SegmentSeedType {
            return when (mnemonicType) {
                MnemonicType.Mnemonic12 -> SEED_12
                MnemonicType.Mnemonic24 -> SEED_24
            }
        }
    }
}

data class MnemonicGridItem(
    val index: Int,
    val mnemonic: String,
)

data class CheckSeedPhraseState(
    val tvSecondPhrase: TextFieldState,
    val tvSeventhPhrase: TextFieldState,
    val tvEleventhPhrase: TextFieldState,
    val buttonCreateWallet: ButtonState,
)

data class ImportSeedPhraseState(
    val fieldSeedPhrase: TextFieldState,
    val fieldPassphrase: TextFieldState,
    val onSuggestedPhraseClick: (Int) -> Unit,
    val onPassphraseInfoClick: () -> Unit,
    val buttonCreateWallet: ButtonState,
    val invalidWords: Set<String> = emptySet(),
    val suggestionsList: ImmutableList<String> = persistentListOf(),
    val error: SeedPhraseError? = null,
    val bottomSheetConfig: TangemBottomSheetConfig? = null,
)

data class ShowPassphraseInfoBottomSheetContent(val onOkClick: () -> Unit) : TangemBottomSheetConfigContent

data class TextFieldState(
    val onTextFieldValueChanged: (TextFieldValue) -> Unit,
    val textFieldValue: TextFieldValue = TextFieldValue(),
    val label: String? = null,
    val isError: Boolean = false,
    val isFocused: Boolean = false,
    val onFocusChanged: (Boolean) -> Unit = {},
)