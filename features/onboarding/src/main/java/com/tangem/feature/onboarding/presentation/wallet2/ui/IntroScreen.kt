package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.R
import com.tangem.feature.onboarding.presentation.wallet2.model.IntroState
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.Description
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingActionBlock
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingDescriptionBlock

/**
[REDACTED_AUTHOR]
 */
@Composable
fun IntroScreen(state: IntroState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.weight(weight = 1.5f),
        ) {
            CardImageBlock(state.cardImageUrl)
        }
        Box(
            modifier = Modifier.weight(weight = 1f),
        ) {
            OnboardingDescriptionBlock {
                Description(
                    titleRes = R.string.onboarding_create_wallet_options_title,
                    subTitleRes = R.string.onboarding_create_wallet_options_message,
                )
            }
        }
        Box {
            OnboardingActionBlock(
                firstActionContent = {
                    PrimaryButtonIconEnd(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResourceSafe(id = R.string.onboarding_create_wallet_button_create_wallet),
                        iconResId = R.drawable.ic_tangem_24,
                        enabled = state.buttonCreateWallet.enabled,
                        showProgress = state.buttonCreateWallet.showProgress,
                        onClick = state.buttonCreateWallet.onClick,
                    )
                },
                secondActionContent = {
                    SecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResourceSafe(id = R.string.onboarding_create_wallet_options_button_options),
                        enabled = state.buttonOtherOptions.enabled,
                        showProgress = state.buttonOtherOptions.showProgress,
                        onClick = state.buttonOtherOptions.onClick,
                    )
                },
            )
        }
    }
}

@Composable
private fun CardImageBlock(cardImageUrl: String? = null) {
    Box(modifier = Modifier.fillMaxSize()) {
        val cardImageModifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center)

        SubcomposeAsyncImage(
            modifier = cardImageModifier.padding(horizontal = TangemTheme.dimens.size34),
            model = ImageRequest.Builder(LocalContext.current)
                .data(cardImageUrl)
                .crossfade(true)
                .build(),
            loading = { CardPlaceHolder(cardImageModifier) },
            error = { CardPlaceHolder(cardImageModifier) },
            contentDescription = null,
        )
    }
}

@Composable
private fun CardPlaceHolder(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(id = R.drawable.card_placeholder_black),
        contentScale = ContentScale.Fit,
        contentDescription = null,
    )
}