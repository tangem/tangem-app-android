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
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingDescription

@Composable
fun OnboardingDescriptionBlock(
    modifier: Modifier = Modifier,
    descriptionsList: List<OnboardingDescription> = emptyList(),
) {
    if (descriptionsList.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        if (descriptionsList.size == 1) {
            SingleDescription(descriptionsList[0])
        } else {
            CarouselDescription(descriptionsList)
        }
    }
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

@Composable
private fun SingleDescription(description: OnboardingDescription) {
    Column {
        DescriptionTitleText(text = description.getTitle(LocalContext.current))
        if (description.hasTitle()) SpacerH12()
        DescriptionSubTitleText(text = description.getSubTitle(LocalContext.current))
    }
}

@Composable
private fun CarouselDescription(descriptionsList: List<OnboardingDescription>) {
    Box {}
}

