package com.tangem.features.jointaccount.creation.composition.ui

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_percent_24
import com.tangem.core.ui.res.generated.icons.ic_sign_minus_24
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_24
import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM
import com.tangem.features.jointaccount.creation.impl.R

@Composable
internal fun JointAccountCompositionScreen(
    state: JointAccountCompositionUM,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.primary),
    ) {
        var topBarHeightPx by remember { mutableIntStateOf(0) }
        var footerHeightPx by remember { mutableIntStateOf(0) }
        val topBarHeight = with(density) { topBarHeightPx.toDp() }
        val footerHeight = with(density) { footerHeightPx.toDp() }
        val bottomNavHeight = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem()
                .background(color = TangemTheme.colors3.bg.primary)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = topBarHeight + 12.dp, bottom = footerHeight + 12.dp + bottomNavHeight),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResourceSafe(R.string.joint_account_members_title),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )

            Text(
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                text = explanationText(state = state),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )

            MemberSlotsRow(state = state, modifier = Modifier.padding(start = 8.dp, bottom = 20.dp))

            SteppersCard(state = state)
        }

        Footer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .onSizeChanged { footerHeightPx = it.height },
            onContinueClick = state.onContinueClick,
        )

        TangemTopNavigation(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeightPx = it.height },
            startButton = { TangemButton.Back(onClick = state.onBackClick) },
            endButton = { TangemButton.Close(onClick = onCloseClick) },
        )
    }
}

@Composable
private fun explanationText(state: JointAccountCompositionUM): AnnotatedString {
    val required = state.requiredToSign.value.toString()
    val total = state.totalMembers.value.toString()
    val highlightedPart = stringResourceSafe(
        R.string.joint_account_members_subtitle_helper_part,
        required,
        total,
    )
    val text = stringResourceSafe(
        R.string.joint_account_members_subtitle_main_part,
        highlightedPart,
    )

    return buildAnnotatedString {
        append(text)

        val start = text.indexOf(highlightedPart)
        if (start >= 0 && highlightedPart.isNotEmpty()) {
            addStyle(
                style = SpanStyle(
                    color = TangemTheme.colors3.text.primary,
                    fontWeight = FontWeight.SemiBold,
                ),
                start = start,
                end = start + highlightedPart.length,
            )
        }
    }
}

@Composable
private fun MemberSlotsRow(state: JointAccountCompositionUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(times = state.totalMembers.value) { index ->
            MemberSlot(isActive = index < state.requiredToSign.value)
        }
    }
}

@Composable
private fun MemberSlot(isActive: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                color = if (isActive) {
                    TangemTheme.colors3.bg.status.infoSubtle
                } else {
                    TangemTheme.colors3.bg.opaque.primary
                },
            )
            .border(
                color = if (isActive) {
                    TangemTheme.colors3.border.status.infoSubtle
                } else {
                    TangemTheme.colors3.border.primary
                },
                shape = CircleShape,
                width = 1.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = Icons.ic_percent_24, // TODO change with real icon
            contentDescription = null,
            tint = if (isActive) {
                TangemTheme.colors3.icon.brand
            } else {
                TangemTheme.colors3.icon.secondary
            },
        )
    }
}

@Composable
private fun SteppersCard(state: JointAccountCompositionUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        StepperRow(
            title = stringResourceSafe(R.string.joint_account_members_total_title),
            subtitle = stringResourceSafe(
                R.string.joint_account_members_total_subtitle,
                state.maxMembers.toString(),
            ),
            stepper = state.totalMembers,
        )

        StepperRow(
            title = stringResourceSafe(R.string.joint_account_members_signers_title),
            subtitle = stringResourceSafe(R.string.joint_account_members_signers_subtitle),
            stepper = state.requiredToSign,
        )
    }
}

@Composable
private fun StepperRow(title: String, subtitle: String, stepper: JointAccountCompositionUM.StepperUM) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            TangemRowText(text = stringReference(title), role = TangemRowTextRole.Title)
        },
        subtitleSlot = {
            TangemRowText(
                text = stringReference(subtitle),
                role = TangemRowTextRole.Subtitle,
                maxLines = 2,
            )
        },
        endSlot = {
            StepperControls(stepper = stepper)
        },
        divider = true,
    )
}

@Composable
private fun StepperControls(stepper: JointAccountCompositionUM.StepperUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StepperButton(
            icon = Icons.ic_sign_minus_24,
            isEnabled = stepper.isDecrementEnabled,
            onClick = stepper.onDecrement,
        )

        Text(
            modifier = Modifier.widthIn(min = 20.dp),
            text = stepper.value.toString(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )

        StepperButton(
            icon = Icons.ic_sign_plus_24,
            isEnabled = stepper.isIncrementEnabled,
            onClick = stepper.onIncrement,
        )
    }
}

@Composable
private fun StepperButton(icon: ImageVector, isEnabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(contentAlignment = Alignment.Center) {
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X8,
            iconStart = TangemIconUM.Icon(imageVector = icon),
            isEnabled = isEnabled,
            interactionSource = interactionSource,
            onClick = onClick,
        )

        Box(
            modifier = Modifier
                .requiredSize(48.dp)
                .clearAndSetSemantics {}
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = isEnabled,
                    onClick = onClick,
                ),
        )
    }
}

@Composable
private fun Footer(onContinueClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        TangemFade(
            position = TangemFade.Position.Bottom,
            modifier = Modifier.matchParentSize(),
        )

        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.common_continue),
            onClick = onContinueClick,
        )
    }
}

@Preview(showBackground = true, heightDp = 874)
@Preview(showBackground = true, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_JointAccountCompositionScreen(
    @PreviewParameter(JointAccountCompositionStateProvider::class) state: JointAccountCompositionUM,
) {
    TangemThemePreviewRedesign {
        JointAccountCompositionScreen(state = state, onCloseClick = {})
    }
}

private class JointAccountCompositionStateProvider : CollectionPreviewParameterProvider<JointAccountCompositionUM>(
    collection = listOf(
        createPreviewState(totalMembers = 2, requiredToSign = 2),
        createPreviewState(totalMembers = 5, requiredToSign = 3),
    ),
)

private fun createPreviewState(totalMembers: Int, requiredToSign: Int): JointAccountCompositionUM {
    return JointAccountCompositionUM(
        totalMembers = JointAccountCompositionUM.StepperUM(
            value = totalMembers,
            isDecrementEnabled = totalMembers > JointAccountCompositionUM.MIN_MEMBERS,
            isIncrementEnabled = totalMembers < JointAccountCompositionUM.MAX_MEMBERS,
            onDecrement = {},
            onIncrement = {},
        ),
        requiredToSign = JointAccountCompositionUM.StepperUM(
            value = requiredToSign,
            isDecrementEnabled = requiredToSign > JointAccountCompositionUM.MIN_REQUIRED_TO_SIGN,
            isIncrementEnabled = requiredToSign < totalMembers,
            onDecrement = {},
            onIncrement = {},
        ),
        maxMembers = JointAccountCompositionUM.MAX_MEMBERS,
        onContinueClick = {},
        onBackClick = {},
    )
}