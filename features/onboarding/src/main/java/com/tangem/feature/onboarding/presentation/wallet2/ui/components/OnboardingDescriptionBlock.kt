package com.tangem.feature.onboarding.presentation.wallet2.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingDescription
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * UI component witch provides single and carousel description for the onboarding process
 */
@Composable
fun OnboardingDescriptionBlock(
    modifier: Modifier = Modifier,
    descriptionsList: ImmutableList<OnboardingDescription> = persistentListOf(),
) {
    if (descriptionsList.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.size24),
    ) {
        if (descriptionsList.size == 1) {
            Description(descriptionsList[0])
        } else {
            CarouselDescription(descriptionsList)
        }
    }
}

@Composable
private fun Description(description: OnboardingDescription) {
    Column {
        DescriptionTitleText(text = stringResource(id = description.titleRes))
        SpacerH12()
        DescriptionSubTitleText(text = stringResource(id = description.subTitleRes))
    }
}

@Composable
private fun CarouselDescription(descriptionsList: ImmutableList<OnboardingDescription>) {
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