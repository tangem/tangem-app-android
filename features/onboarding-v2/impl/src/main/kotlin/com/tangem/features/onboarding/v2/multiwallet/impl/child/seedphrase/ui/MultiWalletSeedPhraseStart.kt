package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM

@Composable
internal fun MultiWalletSeedPhraseStart(state: MultiWalletSeedPhraseUM.Start, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(top = 48.dp),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                modifier = Modifier.size(56.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_onboarding_text_edit_56),
                contentDescription = null,
            )
            Box(
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.icon.warning.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(TangemTheme.dimens.radius8),
                    ),
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = TangemTheme.dimens.size12, vertical = TangemTheme.dimens.size4),
                    text = stringResourceSafe(id = R.string.onboarding_seed_phrase_intro_legacy),
                    color = TangemTheme.colors.text.warning,
                    style = TangemTheme.typography.body1,
                )
            }
            BodyContent(state)
        }
        SpacerH32()
        Buttons(state)
    }
}

@Composable
private fun BodyContent(state: MultiWalletSeedPhraseUM.Start, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResourceSafe(id = R.string.onboarding_seed_intro_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = stringResourceSafe(id = R.string.onboarding_seed_intro_message),
            style = TangemTheme.typography.body1
                .copy(lineBreak = LineBreak.Paragraph),
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth(),
        )
        ReadMoreBlock(state)
    }
}

@Composable
private fun ReadMoreBlock(state: MultiWalletSeedPhraseUM.Start, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        val shape = TangemTheme.shapes.roundedCornersLarge
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(shape)
                .border(
                    width = 1.dp,
                    color = TangemTheme.colors.stroke.primary,
                    shape = shape,
                )
                .clickable(onClick = state.onLearnMoreClicked)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_top_right_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.primary1,
            )
            SpacerW8()
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                style = TangemTheme.typography.button,
                text = stringResourceSafe(id = R.string.onboarding_seed_button_read_more),
            )
        }
    }
}

@Composable
private fun Buttons(state: MultiWalletSeedPhraseUM.Start, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(id = R.string.onboarding_seed_intro_button_generate),
            onClick = state.onGenerateSeedPhraseClicked,
        )
        SecondaryButton(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResourceSafe(id = R.string.onboarding_seed_intro_button_import),
            onClick = state.onImportSeedPhraseClicked,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        MultiWalletSeedPhraseStart(
            state = MultiWalletSeedPhraseUM.Start(),
        )
    }
}