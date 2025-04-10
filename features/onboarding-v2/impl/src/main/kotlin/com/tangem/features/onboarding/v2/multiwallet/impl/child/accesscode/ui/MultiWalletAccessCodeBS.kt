package com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemAnimations
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui.state.MultiWalletAccessCodeUM
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui.state.MultiWalletAccessCodeUM.Step.*

@Composable
internal fun MultiWalletAccessCodeBS(
    config: TangemBottomSheetConfig,
    state: MultiWalletAccessCodeUM,
    onBack: () -> Unit,
) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(config = config, onBack = onBack) { _ ->
        Content(state)
    }
}

@Composable
private fun Content(state: MultiWalletAccessCodeUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = state.step,
            transitionSpec = TangemAnimations.AnimatedContent
                .slide { initial, target -> target.ordinal > initial.ordinal },
            label = "AnimatedContent",
        ) { step ->
            when (step) {
                Intro -> MultiWalletAccessCodeIntro(
                    modifier = Modifier.padding(top = 56.dp),
                )
                AccessCode -> MultiWalletAccessCodeEnter(
                    modifier = Modifier.padding(top = 16.dp),
                    state = state,
                    reEnterAccessCodeState = false,
                )
                ConfirmAccessCode -> MultiWalletAccessCodeEnter(
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