package com.tangem.features.onramp.mainv2.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onramp.mainv2.entity.OnrampAmountButtonUM
import com.tangem.features.onramp.mainv2.entity.OnrampOffersBlockUM
import com.tangem.features.onramp.mainv2.entity.OnrampV2AmountButtonUMState
import com.tangem.features.onramp.mainv2.entity.OnrampV2MainComponentUM

@Composable
internal fun BoxScope.OnrampFooterContent(state: OnrampV2MainComponentUM.Content, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        modifier = modifier
            .imePadding()
            .align(Alignment.BottomCenter),
        visible = state.offersBlockState is OnrampOffersBlockUM.Empty,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 300),
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300),
        ),
        label = "Footer block animation",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpacerH(16.dp)
            OnrampAmountButtons(state = state.onrampAmountButtonUMState)
        }
    }
}

@Composable
private fun OnrampAmountButtons(state: OnrampV2AmountButtonUMState) {
    val keyboard by keyboardAsState()

    AnimatedVisibility(
        visible = state is OnrampV2AmountButtonUMState.Loaded,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        when (state) {
            is OnrampV2AmountButtonUMState.Loaded -> {
                if (keyboard is Keyboard.Opened) {
                    LazyRow(
                        modifier = Modifier.background(color = TangemTheme.colors.button.secondary),
                        contentPadding = PaddingValues(
                            vertical = 10.dp,
                            horizontal = 8.dp,
                        ),
                        state = rememberLazyListState(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = state.amountButtons,
                            key = OnrampAmountButtonUM::value,
                        ) {
                            AmountButton(button = it)
                        }
                    }
                }
            }
            OnrampV2AmountButtonUMState.None -> Unit
        }
    }
}

@Composable
private fun AmountButton(button: OnrampAmountButtonUM, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier = modifier
            .sizeIn(minHeight = 24.dp, minWidth = 62.dp)
            .background(
                color = TangemTheme.colors.field.primary,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                onClick = {
                    focusManager.clearFocus()
                    button.onClick()
                },
            )
            .padding(vertical = 4.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${button.value}${button.currency}",
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.primary1,
        )
    }
}