package com.tangem.features.swap.v2.impl.sendviaswap.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.common.ui.footers.SendingText
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.ui.components.Fade
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute
import com.tangem.utils.StringsSigns

@Composable
internal fun SendWithSwapContent(
    navigationUM: NavigationUM,
    confirmUM: ConfirmUM,
    stackState: ChildStack<SendWithSwapRoute, ComposableContentComponent>,
    onLinkClick: (String) -> Unit,
) {
    val navigationUMContent = navigationUM as? NavigationUM.Content ?: return

    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBarWithBackButton(
            text = navigationUMContent.title.resolveReference(),
            onBackClick = navigationUMContent.backIconClick,
            iconRes = navigationUMContent.additionalIconRes,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )
        Children(
            stack = stackState,
            animation = stackAnimation { child ->
                when (child.configuration) {
                    SendWithSwapRoute.Confirm -> fade()
                    SendWithSwapRoute.Success -> slide(orientation = Orientation.Vertical) + fade()
                    else -> slide()
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Box(modifier = Modifier.fillMaxHeight()) {
                it.instance.Content(Modifier.fillMaxSize(1f))

                if (stackState.active.configuration != SendWithSwapRoute.Success) {
                    Fade(
                        backgroundColor = TangemTheme.colors.background.tertiary,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
        if (stackState.active.configuration != SendWithSwapRoute.Success) {
            Column {
                AnimatedVisibility(
                    visible = stackState.active.configuration == SendWithSwapRoute.Confirm,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                ) {
                    val confirmContentUM = confirmUM as? ConfirmUM.Content
                    val sendFooter = confirmContentUM?.sendingFooter ?: TextReference.EMPTY
                    val legalFooter = getAnnotatedStringForLegals(confirmContentUM?.tosUM, onClick = onLinkClick)
                    SendingText(
                        footerText = if (sendFooter != TextReference.EMPTY || legalFooter != TextReference.EMPTY) {
                            combinedReference(sendFooter, legalFooter)
                        } else {
                            TextReference.EMPTY
                        },
                    )
                }
                NavigationPrimaryButton(
                    navigationUMContent.primaryButton,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun getAnnotatedStringForLegals(tosUM: ConfirmUM.Content.TosUM?, onClick: (String) -> Unit): TextReference {
    if (tosUM == null) return TextReference.EMPTY
    val tos = tosUM.tosLink
    val policy = tosUM.policyLink
    return if (tos != null && policy != null) {
        val tosTitle = tos.title.resolveReference()
        val policyTitle = policy.title.resolveReference()
        val fullString = stringResourceSafe(id = R.string.express_legal_two_placeholders, tosTitle, policyTitle)
        val tosIndex = fullString.indexOf(tosTitle)
        val policyIndex = fullString.indexOf(policyTitle)

        annotatedReference {
            append(StringsSigns.POINT_SIGN)
            appendSpace()
            append(fullString.substring(0, tosIndex))
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "TOS_TAG",
                    linkInteractionListener = { onClick(tos.link) },
                ),
                block = {
                    appendColored(
                        text = fullString.substring(tosIndex, tosIndex + tosTitle.length),
                        color = TangemTheme.colors.text.accent,
                    )
                },
            )
            append(fullString.substring(tosIndex + tosTitle.length, policyIndex))
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "POLICY_TAG",
                    linkInteractionListener = { onClick(policy.link) },
                ),
                block = {
                    appendColored(
                        text = fullString.substring(policyIndex, policyIndex + policyTitle.length),
                        color = TangemTheme.colors.text.accent,
                    )
                },
            )
        }
    } else {
        val legal = requireNotNull(tos ?: policy) { "tos or policy must not be null" }
        val legalTitle = legal.title.resolveReference()
        val fullString = stringResourceSafe(id = R.string.express_legal_one_placeholder, legalTitle)
        val legalIndex = fullString.indexOf(legalTitle)

        annotatedReference {
            append(fullString.substring(0, legalIndex))
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "LEGAL_TAG",
                    linkInteractionListener = { onClick(legal.link) },
                ),
                block = {
                    appendColored(
                        text = fullString.substring(legalIndex, legalIndex + legalTitle.length),
                        color = TangemTheme.colors.text.accent,
                    )
                },
            )
        }
    }
}