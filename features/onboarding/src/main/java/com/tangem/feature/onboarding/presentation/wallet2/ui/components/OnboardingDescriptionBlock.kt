package com.tangem.feature.onboarding.presentation.wallet2.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

/**
 * UI component witch provides single and carousel description for the onboarding process
 */
@Composable
fun OnboardingDescriptionBlock(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.size36),
    ) {
        content()
    }
}

@Composable
fun Description(@StringRes titleRes: Int, @StringRes subTitleRes: Int) {
    Column {
        DescriptionTitleText(text = stringResourceSafe(id = titleRes))
        SpacerH16()
        DescriptionSubTitleText(text = stringResourceSafe(id = subTitleRes))
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
    // the text style made similar to TextViewOnboarding.Body
    Text(
        text = text,
        style = TangemTheme.typography.body1.copy(
            lineHeight = 20.sp,
            letterSpacing = 0.03.sp,
        ),
        color = TangemTheme.colors.text.secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}