package com.tangem.common.ui.amountScreen.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.currency.fiaticon.FiatIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.rememberDecimalFormat
import kotlinx.coroutines.delay

private const val ROTATED_DEGREE = 180f
private const val INITIAL_DEGREE = 0f

@Composable
fun AmountFieldV2(
    amountUM: AmountState,
    onValueChange: (String) -> Unit,
    onValuePastedTriggerDismiss: () -> Unit,
    onCurrencyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val decimalFormat = rememberDecimalFormat()

    val requester = remember { FocusRequester() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        if (amountUM is AmountState.Empty) {
            TextShimmer(
                style = TangemTheme.typography.head,
                modifier = Modifier.width(150.dp),
            )
        } else if (amountUM is AmountState.Data) {
            AnimatedContent(
                targetState = amountUM.amountTextField.isFiatValue,
                transitionSpec = { primaryFieldCurrencyAnimation() },
                label = "Primary field change animation",
            ) { isFiatValue ->
                val currencyCode = if (isFiatValue) amountUM.appCurrency.code else null
                val (primaryAmount, primaryValue) = if (isFiatValue) {
                    amountUM.amountTextField.fiatAmount to amountUM.amountTextField.fiatValue
                } else {
                    amountUM.amountTextField.cryptoAmount to amountUM.amountTextField.value
                }
                AmountTextField(
                    value = primaryValue,
                    decimals = primaryAmount.decimals,
                    visualTransformation = AmountVisualTransformation(
                        decimals = primaryAmount.decimals,
                        symbol = primaryAmount.currencySymbol,
                        currencyCode = currencyCode,
                        decimalFormat = decimalFormat,
                        symbolColor = TangemTheme.colors.text.disabled,
                    ),
                    onValueChange = onValueChange,
                    keyboardOptions = amountUM.amountTextField.keyboardOptions,
                    keyboardActions = amountUM.amountTextField.keyboardActions,
                    textStyle = TangemTheme.typography.head.copy(
                        textAlign = TextAlign.Center,
                    ),
                    isAutoResize = true,
                    isValuePasted = amountUM.amountTextField.isValuePasted,
                    onValuePastedTriggerDismiss = onValuePastedTriggerDismiss,
                    modifier = Modifier
                        .focusRequester(requester)
                        .requiredHeightIn(min = 44.dp),
                )
                LaunchedEffect(key1 = Unit) {
                    delay(timeMillis = 200)
                    requester.requestFocus()
                }
            }
        }
        AmountSecondary(
            amountUM = amountUM,
            onCurrencyChange = onCurrencyChange,
        )
    }
}

@Composable
private fun AmountSecondary(amountUM: AmountState, onCurrencyChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(top = 4.dp),
    ) {
        if (amountUM is AmountState.Empty) {
            TextShimmer(
                style = TangemTheme.typography.caption2,
                modifier = Modifier
                    .width(90.dp)
                    .padding(bottom = 20.dp)
                    .align(TopCenter),
            )
        } else if (amountUM is AmountState.Data) {
            AmountFieldCurrencyInfo(
                amountUM = amountUM,
                onCurrencyChange = onCurrencyChange,
            )
            AmountFieldError(
                isError = amountUM.amountTextField.isError,
                isWarning = amountUM.amountTextField.isWarning,
                error = amountUM.amountTextField.error,
                modifier = Modifier
                    .align(BottomCenter)
                    .padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun BoxScope.AmountFieldCurrencyInfo(amountUM: AmountState.Data, onCurrencyChange: (Boolean) -> Unit) {
    val isFiatAvailable = amountUM.amountTextField.fiatAmount.value != null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .align(TopCenter)
            .padding(bottom = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCurrencyChange(!amountUM.amountTextField.isFiatValue) },
            )
            .alpha(if (isFiatAvailable) 1f else 0f)
            .padding(4.dp),
    ) {
        val iconRotateState by animateFloatAsState(
            targetValue = if (amountUM.amountTextField.isFiatValue) ROTATED_DEGREE else INITIAL_DEGREE,
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing,
            ),
            label = "Currency change icon animation",
        )
        Icon(
            painter = rememberVectorPainter(
                ImageVector.vectorResource(R.drawable.ic_exchange_vertical_24),
            ),
            tint = TangemTheme.colors.icon.accent,
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    rotationY = iconRotateState
                },
        )
        AmountFieldCurrencyIcon(
            amountUM = amountUM,
        )
    }
}

@Composable
private fun AmountFieldCurrencyIcon(amountUM: AmountState.Data) {
    AnimatedContent(
        targetState = amountUM.amountTextField.isFiatValue,
        transitionSpec = { secondaryFieldCurrencyAnimation() },
        label = "Secondary field change animation",
    ) { isFiatValue ->
        val secondaryAmount =
            if (isFiatValue) amountUM.amountTextField.cryptoAmount else amountUM.amountTextField.fiatAmount
        val text = secondaryAmount.value.format {
            if (isFiatValue) {
                crypto(secondaryAmount.currencySymbol, secondaryAmount.decimals)
            } else {
                fiat(
                    fiatCurrencySymbol = amountUM.appCurrency.symbol,
                    fiatCurrencyCode = amountUM.appCurrency.code,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = text,
                style = TangemTheme.typography.caption2.copy(textDirection = TextDirection.ContentOrLtr),
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            if (isFiatValue) {
                CurrencyIcon(
                    state = amountUM.tokenIconState,
                    shouldDisplayNetwork = false,
                    modifier = Modifier.size(12.dp),
                )
            } else {
                FiatIcon(
                    url = amountUM.appCurrency.iconSmallUrl,
                    size = 12.dp,
                    isGrayscale = false,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun AmountFieldError(
    isError: Boolean,
    isWarning: Boolean,
    error: TextReference,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isError || isWarning,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
        label = "Error field appearance animation",
    ) {
        val errorText = remember(this, error) { error }
        val color = if (isError) TangemTheme.colors.text.warning else TangemTheme.colors.text.attention
        Text(
            text = errorText.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}

private fun <S> AnimatedContentTransitionScope<S>.secondaryFieldCurrencyAnimation(): ContentTransform {
    return (
        fadeIn(
            animationSpec = tween(durationMillis = 300),
        ) + scaleIn(
            animationSpec = tween(durationMillis = 300),
            initialScale = 0.9f,
        )
        ).togetherWith(
        fadeOut(
            animationSpec = tween(durationMillis = 300),
        ) + scaleOut(
            animationSpec = tween(durationMillis = 300),
            targetScale = 0.9f,
        ),
    )
}

private fun <S> AnimatedContentTransitionScope<S>.primaryFieldCurrencyAnimation(): ContentTransform {
    return (
        fadeIn(
            animationSpec = tween(durationMillis = 300),
        ) + scaleIn(
            animationSpec = tween(durationMillis = 300),
            initialScale = 0.9f,
        ) + slideInVertically(initialOffsetY = { it / 2 })
        )
        .togetherWith(
            fadeOut(
                animationSpec = tween(durationMillis = 300),
            ) + scaleOut(
                animationSpec = tween(durationMillis = 300),
                targetScale = 0.9f,
            ) + slideOutVertically(targetOffsetY = { it / 2 }),
        )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AmountFieldV2_Preview(@PreviewParameter(AmountFieldV2PreviewProvider::class) params: AmountState) {
    TangemThemePreview {
        AmountFieldV2(
            amountUM = params,
            onValueChange = {},
            onValuePastedTriggerDismiss = { },
            onCurrencyChange = {},
            modifier = Modifier.background(TangemTheme.colors.background.action),
        )
    }
}

private class AmountFieldV2PreviewProvider : PreviewParameterProvider<AmountState> {
    override val values: Sequence<AmountState>
        get() = sequenceOf(
            AmountStatePreviewData.emptyState,
            AmountStatePreviewData.amountState,
            AmountStatePreviewData.amountErrorState,
            AmountStatePreviewData.amountStateV2WithoutRates,
        )
}
// endregion