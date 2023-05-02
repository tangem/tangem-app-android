package com.tangem.feature.onboarding.presentation.wallet2.ui.components

import androidx.annotation.StringRes
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
import com.tangem.feature.onboarding.presentation.wallet2.model.DescriptionResource
import kotlinx.collections.immutable.ImmutableList

/**
 * UI component witch provides single and carousel description for the onboarding process
 */
@Composable
fun OnboardingDescriptionBlock(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.size24),
    ) {
        content()
    }
}

@Composable
fun Description(@StringRes titleRes: Int, @StringRes subTitleRes: Int) {
    Column {
        DescriptionTitleText(text = stringResource(id = titleRes))
        SpacerH12()
        DescriptionSubTitleText(text = stringResource(id = subTitleRes))
    }
}

@Suppress("UnusedPrivateMember")
@Composable
fun OnboardingCarouselDescriptionBlock(
    descriptionsList: ImmutableList<DescriptionResource>,
    modifier: Modifier = Modifier,
) {
    OnboardingDescriptionBlock(modifier) {
// [REDACTED_TODO_COMMENT]
    }
}

@Composable
fun DescriptionTitleText(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography.h2,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun DescriptionSubTitleText(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography.subtitle1,
        color = TangemTheme.colors.text.secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
