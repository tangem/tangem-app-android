package com.tangem.features.send.v2.subcomponents.fee.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.components.inputrow.InputRowEnter
import com.tangem.core.ui.components.inputrow.InputRowEnterAmount
import com.tangem.core.ui.components.inputrow.InputRowEnterInfoAmount
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.api.entity.CustomFeeFieldUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongParameterList")
@Composable
internal fun SendCustomFee(
    customValues: ImmutableList<CustomFeeFieldUM>,
    selectedFee: FeeType,
    hasNotifications: Boolean,
    onValueChange: (Int, String) -> Unit,
    onNonceChange: (String) -> Unit,
    displayNonceInput: Boolean,
    nonce: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = selectedFee == FeeType.Custom && customValues.isNotEmpty(),
        label = "Custom Fee Selected Animation",
        enter = expandVertically().plus(fadeIn()),
        exit = shrinkVertically().plus(fadeOut()),
    ) {
        val bottomPadding = if (hasNotifications) 12.dp else 0.dp
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.padding(bottom = bottomPadding),
        ) {
            repeat(customValues.size) { index ->
                val value = customValues[index]
                FooterContainer(
                    footer = value.footer,
                ) {
                    if (value.label != null) {
                        InputRowEnterInfoAmount(
                            text = value.value,
                            decimals = value.decimals,
                            symbol = value.symbol,
                            title = value.title,
                            info = value.label,
                            keyboardOptions = value.keyboardOptions,
                            keyboardActions = value.keyboardActions,
                            onValueChange = { onValueChange(index, it) },
                            showDivider = false,
                            isReadOnly = value.isReadonly,
                            modifier = Modifier
                                .background(
                                    color = TangemTheme.colors.background.action,
                                    shape = TangemTheme.shapes.roundedCornersXMedium,
                                ),
                        )
                    } else {
                        InputRowEnterAmount(
                            text = value.value,
                            decimals = value.decimals,
                            title = value.title,
                            symbol = value.symbol,
                            onValueChange = { onValueChange(index, it) },
                            keyboardOptions = value.keyboardOptions,
                            keyboardActions = value.keyboardActions,
                            showDivider = false,
                            modifier = Modifier
                                .background(
                                    color = TangemTheme.colors.background.action,
                                    shape = TangemTheme.shapes.roundedCornersXMedium,
                                ),
                        )
                    }
                }
            }
            if (displayNonceInput) {
                Nonce(
                    onNonceChange = onNonceChange,
                    nonce = nonce,
                )
            }
        }
    }
}

@Composable
private fun Nonce(onNonceChange: (String) -> Unit, nonce: String?) {
    FooterContainer(
        footer = resourceReference(R.string.send_nonce_footer),
        modifier = Modifier.padding(bottom = 12.dp),
    ) {
        InputRowEnter(
            text = nonce.orEmpty(),
            title = resourceReference(R.string.send_nonce),
            onValueChange = onNonceChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = resourceReference(R.string.send_nonce_hint),
            showDivider = false,
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                ),
        )
    }
}
