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
import androidx.compose.ui.platform.testTag
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
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_minus_24
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_24
import com.tangem.core.ui.res.generated.icons.ic_user_24
import com.tangem.core.ui.test.accounts.JointAccountCreationTestTags
import com.tangem.core.ui.utils.dashedBorder
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM
import com.tangem.features.jointaccount.impl.R

@Composable
internal fun JointAccountCompositionScreen(
    state: JointAccountCompositionUM,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var footerHeightPx by remember { mutableIntStateOf(0) }

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                startButton = { TangemButton.Back(onClick = state.onBackClick) },
                endButton = { TangemButton.Close(onClick = onCloseClick) },
            )
        },
        overlay = {
            Footer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size -> footerHeightPx = size.height },
                onContinueClick = state.onContinueClick,
            )
        },
    ) { contentPadding ->
        val footerHeight = with(LocalDensity.current) { footerHeightPx.toDp() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    bottom = maxOf(contentPadding.calculateBottomPadding(), footerHeight) + 12.dp,
                ),
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
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
            )

            MemberSlotsRow(state = state, modifier = Modifier.padding(start = 8.dp, bottom = 20.dp))

            SteppersCard(state = state)
        }
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
    val highlightColor = TangemTheme.colors3.text.primary

    return remember(text, highlightedPart, highlightColor) {
        buildAnnotatedString {
            append(text)

            val start = text.indexOf(highlightedPart)
            if (start >= 0 && highlightedPart.isNotEmpty()) {
                addStyle(
                    style = SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold),
                    start = start,
                    end = start + highlightedPart.length,
                )
            }
        }
    }
}

@Composable
private fun MemberSlotsRow(state: JointAccountCompositionUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(times = state.maxMembers) { index ->
            val slotState = when {
                index < state.requiredToSign.value -> MemberSlotState.ACTIVE
                index < state.totalMembers.value -> MemberSlotState.FILLED
                else -> MemberSlotState.PLACEHOLDER
            }
            MemberSlot(state = slotState)
        }
    }
}

private enum class MemberSlotState { ACTIVE, FILLED, PLACEHOLDER }

@Composable
private fun MemberSlot(state: MemberSlotState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(
                when (state) {
                    MemberSlotState.ACTIVE -> Modifier
                        .background(color = TangemTheme.colors3.bg.status.infoSubtle)
                        .border(
                            color = TangemTheme.colors3.border.status.infoSubtle,
                            shape = CircleShape,
                            width = 1.dp,
                        )
                    MemberSlotState.FILLED -> Modifier
                        .background(color = TangemTheme.colors3.bg.transparent.primary)
                        .border(
                            color = TangemTheme.colors3.border.primary,
                            shape = CircleShape,
                            width = 1.dp,
                        )
                    MemberSlotState.PLACEHOLDER -> Modifier.dashedBorder(
                        color = TangemTheme.colors3.border.secondary,
                        shape = CircleShape,
                        dashLength = 4.dp,
                        gapLength = 4.dp,
                        strokeWidth = 1.5.dp,
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (state != MemberSlotState.PLACEHOLDER) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.ic_user_24,
                contentDescription = null,
                tint = if (state == MemberSlotState.ACTIVE) {
                    TangemTheme.colors3.icon.brand
                } else {
                    TangemTheme.colors3.icon.secondary
                },
            )
        }
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
            isLast = false,
        )

        StepperRow(
            title = stringResourceSafe(R.string.joint_account_members_signers_title),
            subtitle = stringResourceSafe(R.string.joint_account_members_signers_subtitle),
            stepper = state.requiredToSign,
            isLast = true,
        )
    }
}

@Composable
private fun StepperRow(
    title: String,
    subtitle: String,
    stepper: JointAccountCompositionUM.StepperUM,
    isLast: Boolean,
) {
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
        divider = !isLast,
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
    TangemFooter(modifier = modifier) {
        TangemButton(
            modifier = Modifier
                .testTag(JointAccountCreationTestTags.COMPOSITION_CONTINUE_BUTTON)
                .fillMaxWidth(),
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
        createPreviewState(totalMembers = 3, requiredToSign = 2),
        createPreviewState(totalMembers = 5, requiredToSign = 3),
    ),
)

private fun createPreviewState(totalMembers: Int, requiredToSign: Int): JointAccountCompositionUM {
    return JointAccountCompositionUM(
        totalMembers = JointAccountCompositionUM.StepperUM(
            value = totalMembers,
            isDecrementEnabled = totalMembers > JointAccountConfig.MIN_MEMBERS_COUNT,
            isIncrementEnabled = totalMembers < JointAccountConfig.MAX_MEMBERS_COUNT,
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
        maxMembers = JointAccountConfig.MAX_MEMBERS_COUNT,
        onContinueClick = {},
        onBackClick = {},
    )
}