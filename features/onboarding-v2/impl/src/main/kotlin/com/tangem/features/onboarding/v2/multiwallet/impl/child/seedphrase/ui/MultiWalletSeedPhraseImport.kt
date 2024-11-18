package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM

@Suppress("UnusedPrivateMember")
@Composable
fun MultiWalletSeedPhraseImport(state: MultiWalletSeedPhraseUM.Import, modifier: Modifier = Modifier) {
    // TODO
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(id = R.string.onboarding_seed_import_message),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(start = 36.dp, end = 36.dp, top = 24.dp, bottom = 16.dp)
                .fillMaxWidth(),
        )
    }
}

// @Composable
// private fun PhraseBlock(state: MultiWalletSeedPhraseUM.Import, modifier: Modifier = Modifier) {
// TODO
// TODO: replace by TextReference
// val errorConverter = remember { SeedPhraseErrorConverter() }
//
// Column(
//     modifier = modifier.fillMaxWidth(),
// ) {
//     OutlinedTextField(
//         modifier = Modifier
//             .fillMaxWidth()
//             .height(TangemTheme.dimens.size142),
//         value = state.fieldSeedPhrase.textFieldValue,
//         onValueChange = state.fieldSeedPhrase.onTextFieldValueChanged,
//         textStyle = TangemTheme.typography.body1,
//         singleLine = false,
//         visualTransformation = InvalidWordsColorTransformation(
//             wordsToBrush = state.invalidWords,
//             style = SpanStyle(color = TangemTheme.colors.text.warning),
//         ),
//         colors = TangemTextFieldsDefault.defaultTextFieldColors,
//     )
//
//     Box(
//         modifier = Modifier
//             .fillMaxWidth()
//             .height(TangemTheme.dimens.size32),
//     ) {
//         val message = errorConverter.convert(LocalContext.current to state.error)
//         if (message != null) {
//             Text(
//                 modifier = Modifier.fillMaxSize(),
//                 text = message,
//                 style = TangemTheme.typography.caption2.copy(
//                     color = TangemTheme.colors.text.warning,
//                 ),
//             )
//         }
//     }
// }
// }

@Preview
@Composable
private fun Preview() {
    TangemThemePreview {
        MultiWalletSeedPhraseImport(
            state = MultiWalletSeedPhraseUM.Import(),
        )
    }
}
