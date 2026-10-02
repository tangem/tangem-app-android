package com.tangem.features.introduction.impl.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.checkbox.TangemCheckbox
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.introduction.impl.ui.state.SocialLoginConsentUM
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun SocialLoginConsentScreen(state: SocialLoginConsentUM, modifier: Modifier = Modifier) {
    BackHandler(enabled = true, onBack = state.onCloseClick)

    TangemTopBarScaffold(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(pass = PointerEventPass.Final).changes.forEach { it.consume() }
                    }
                }
            },
        topBar = {
            TangemTopNavigation(
                endButton = { TangemButton.Close(onClick = state.onCloseClick) },
            )
        },
    ) { contentPadding ->
        Content(state = state, contentPadding = contentPadding)
    }
}

@Composable
private fun Content(state: SocialLoginConsentUM, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val topPadding = contentPadding.calculateTopPadding()
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = topPadding)
                    .heightIn(min = maxHeight - topPadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    ProviderBadges(modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x5))
                    SpacerH(TangemTheme.dimens2.x8)
                    Column(modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x6)) {
                        Text(
                            text = state.title.resolveAnnotatedReference(),
                            style = TangemTheme.typography3.heading.medium,
                            color = TangemTheme.colors3.text.primary,
                        )
                        SpacerH(TangemTheme.dimens2.x2)
                        Text(
                            text = state.description.resolveAnnotatedReference(),
                            style = TangemTheme.typography3.subheading.medium,
                            color = TangemTheme.colors3.text.secondary,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(horizontal = TangemTheme.dimens2.x4)
                        .padding(top = TangemTheme.dimens2.x6),
                    verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x3),
                ) {
                    ConsentRow(
                        checked = state.isEmailSharingAllowed,
                        onCheckedChange = state.onEmailSharingChange,
                        text = resourceReference(CoreResR.string.social_login_consent_email_sharing),
                    )
                    ConsentRow(
                        checked = state.isAcknowledged,
                        onCheckedChange = state.onAcknowledgedChange,
                        text = state.acknowledgement,
                        isError = state.isAcknowledgementMissing,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x3),
        ) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                size = TangemButton.Size.X12,
                text = resourceReference(CoreResR.string.social_agreement_action_create),
                onClick = state.onConfirmClick,
            )
        }
    }
}

@Composable
private fun ProviderBadges(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(-TangemTheme.dimens2.x4),
    ) {
        Badge {
            Image(
                modifier = Modifier.size(TangemTheme.dimens2.x8),
                painter = painterResource(CoreUiR.drawable.img_google_32),
                contentDescription = null,
            )
        }
        Badge(
            modifier = Modifier.notchedByPrecedingBadge(
                badgeSize = TangemTheme.dimens2.x18,
                overlap = TangemTheme.dimens2.x4,
                notch = TangemTheme.dimens2.x0_5,
            ),
        ) {
            Icon(
                modifier = Modifier.size(34.dp),
                imageVector = Icons.ic_logo_tangem_24,
                tint = TangemTheme.colors3.text.primary,
                contentDescription = null,
            )
        }
    }
}

private fun Modifier.notchedByPrecedingBadge(badgeSize: Dp, overlap: Dp, notch: Dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawCircle(
                color = Color.Black,
                radius = (badgeSize / 2 + notch).toPx(),
                center = Offset(x = (overlap - badgeSize / 2).toPx(), y = size.height / 2),
                blendMode = BlendMode.Clear,
            )
        }

@Composable
private fun Badge(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens2.x18)
            .background(color = TangemTheme.colors3.bg.opaque.primary, shape = CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: TextReference,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemCheckbox(checked = checked, onCheckedChange = null)
        Text(
            modifier = Modifier.padding(start = TangemTheme.dimens2.x3),
            text = text.resolveAnnotatedReference(),
            style = TangemTheme.typography3.caption.medium,
            color = if (isError) TangemTheme.colors3.text.status.error else TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
private fun PreviewConsent(
    initialAcknowledged: Boolean,
    initialEmailSharing: Boolean,
    initialConfirmAttempted: Boolean,
) {
    var isAcknowledged by remember { mutableStateOf(initialAcknowledged) }
    var isEmailSharingAllowed by remember { mutableStateOf(initialEmailSharing) }
    var isConfirmAttempted by remember { mutableStateOf(initialConfirmAttempted) }

    TangemThemePreviewRedesign(isDark = true) {
        SocialLoginConsentScreen(
            state = SocialLoginConsentUM(
                title = stringReference("Create a wallet with Google account"),
                description = stringReference(
                    "Tangem generates the keys on this phone and stores an encrypted copy in your Google " +
                        "account. Signing in there is how you get the wallet back on any device. Learn more",
                ),
                acknowledgement = stringReference(
                    "I understand that losing my Google account means losing this wallet",
                ),
                isAcknowledged = isAcknowledged,
                isAcknowledgementMissing = isConfirmAttempted && !isAcknowledged,
                isEmailSharingAllowed = isEmailSharingAllowed,
                onAcknowledgedChange = { isAcknowledged = it },
                onEmailSharingChange = { isEmailSharingAllowed = it },
                onConfirmClick = { isConfirmAttempted = true },
                onCloseClick = {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSocialLoginConsent() {
    PreviewConsent(initialAcknowledged = false, initialEmailSharing = false, initialConfirmAttempted = false)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun PreviewSocialLoginConsentMissingAcknowledgement() {
    PreviewConsent(initialAcknowledged = false, initialEmailSharing = false, initialConfirmAttempted = true)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun PreviewSocialLoginConsentChecked() {
    PreviewConsent(initialAcknowledged = true, initialEmailSharing = true, initialConfirmAttempted = false)
}