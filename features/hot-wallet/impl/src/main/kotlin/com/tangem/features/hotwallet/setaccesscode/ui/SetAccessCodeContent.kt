package com.tangem.features.hotwallet.setaccesscode.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemAnimations
import com.tangem.features.hotwallet.setaccesscode.entity.SetAccessCodeUM
import com.tangem.features.hotwallet.setaccesscode.entity.SetAccessCodeUM.Step.*
import com.tangem.core.res.R

@Composable
internal fun SetAccessCodeContent(state: SetAccessCodeUM, onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = state.step,
            transitionSpec = TangemAnimations.AnimatedContent
                .slide { initial, target -> target.ordinal > initial.ordinal },
            label = "AnimatedContent",
        ) { step ->
            when (step) {
                AccessCode -> SetAccessCodeEnter(
                    modifier = Modifier.padding(top = 16.dp),
                    state = state,
                    reEnterAccessCodeState = false,
                )
                ConfirmAccessCode -> SetAccessCodeEnter(
                    modifier = Modifier.padding(top = 16.dp),
                    state = state,
                    reEnterAccessCodeState = true,
                )
            }
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .imePadding(),
            text = if (state.step == ConfirmAccessCode) {
                stringResourceSafe(R.string.common_confirm)
            } else {
                stringResourceSafe(R.string.common_continue)
            },
            onClick = state.onContinue,
        )
    }
}