package com.tangem.feature.onboarding.presentation.wallet2.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingDescription

/**
 * UI component witch provides single and carousel description for the onboarding process
 */
@Composable
fun OnboardingDescriptionBlock(
    modifier: Modifier = Modifier,
    descriptionsList: List<OnboardingDescription> = emptyList(),
) {
    if (descriptionsList.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.size24),
    ) {
        if (descriptionsList.size == 1) {
            SingleDescription(descriptionsList[0])
        } else {
            CarouselDescription(descriptionsList)
        }
    }
}

@Composable
private fun SingleDescription(description: OnboardingDescription) {
    Column {
        if (description.hasTitle()) {
            DescriptionTitleText(text = description.getTitle(LocalContext.current))
            SpacerH12()
        }
        DescriptionSubTitleText(text = description.getSubTitle(LocalContext.current))
    }
}

@Composable
private fun CarouselDescription(descriptionsList: List<OnboardingDescription>) {
    //TODO: implement
}

@Composable
private fun DescriptionTitleText(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography.h2,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DescriptionSubTitleText(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography.subtitle1,
        color = TangemTheme.colors.text.secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}