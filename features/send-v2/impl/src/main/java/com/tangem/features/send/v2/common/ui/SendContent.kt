package com.tangem.features.send.v2.common.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.common.ui.footers.SendingText
import com.tangem.common.ui.navigationButtons.NavigationButtonsBlockV2
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.ui.components.Fade
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.ui.state.ConfirmUM

@Composable
internal fun SendContent(
    navigationUM: NavigationUM,
    confirmUM: ConfirmUM,
    stackState: ChildStack<CommonSendRoute, ComposableContentComponent>,
) {
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SendAppBar(navigationUM = navigationUM)
        Children(
            stack = stackState,
            animation = stackAnimation { child ->
                when (child.configuration) {
                    is CommonSendRoute.ConfirmSuccess -> fade(minAlpha = 1.0f)
                    is CommonSendRoute.Confirm -> fade()
                    else -> slide()
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Box(modifier = Modifier.fillMaxHeight()) {
                it.instance.Content(Modifier.fillMaxSize(1f))

                if (stackState.active.configuration != CommonSendRoute.ConfirmSuccess) {
                    Fade(
                        backgroundColor = TangemTheme.colors.background.tertiary,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
        if (stackState.active.configuration != CommonSendRoute.ConfirmSuccess) {
            Column {
                AnimatedVisibility(
                    visible = stackState.active.configuration == CommonSendRoute.Confirm,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                ) {
                    SendingText(footerText = (confirmUM as? ConfirmUM.Content)?.sendingFooter ?: TextReference.EMPTY)
                }
                NavigationButtonsBlockV2(
                    navigationUM = navigationUM,
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
private fun SendAppBar(navigationUM: NavigationUM) {
    val navigationUMContent = navigationUM as? NavigationUM.Content ?: return
    AppBarWithBackButtonAndIcon(
        text = navigationUMContent.title.resolveReference(),
        subtitle = navigationUMContent.subtitle?.resolveReference(),
        onBackClick = navigationUMContent.backIconClick,
        onIconClick = navigationUMContent.additionalIconClick,
        backIconRes = navigationUMContent.backIconRes,
        iconRes = navigationUMContent.additionalIconRes,
        backgroundColor = TangemTheme.colors.background.tertiary,
        modifier = Modifier.height(TangemTheme.dimens.size56),
    )
}