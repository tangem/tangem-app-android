package com.tangem.features.send.v2.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.ui.state.NavigationUM

@Composable
internal fun SendContent(
    navigationUM: NavigationUM,
    stackState: ChildStack<CommonSendRoute, ComposableContentComponent>,
) {
    Column(
        modifier = Modifier.Companion
            .background(color = TangemTheme.colors.background.tertiary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
    ) {
        SendAppBar(navigationUM = navigationUM)
        Children(
            stack = stackState,
            animation = stackAnimation(slide()),
            modifier = Modifier.weight(1f),
        ) {
            it.instance.Content(Modifier.weight(1f))
        }
        SendNavigationButtons(navigationUM = navigationUM)
    }
}

@Composable
private fun SendAppBar(navigationUM: NavigationUM) {
    val navigationUM = navigationUM as? NavigationUM.Content ?: return
    AppBarWithBackButtonAndIcon(
        text = navigationUM.title.resolveReference(),
        subtitle = navigationUM.subtitle?.resolveReference(),
        onBackClick = navigationUM.backIconClick,
        onIconClick = navigationUM.additionalIconClick,
        backIconRes = navigationUM.backIconRes,
        iconRes = navigationUM.additionalIconRes,
        backgroundColor = TangemTheme.colors.background.tertiary,
        modifier = Modifier.height(TangemTheme.dimens.size56),
    )
}