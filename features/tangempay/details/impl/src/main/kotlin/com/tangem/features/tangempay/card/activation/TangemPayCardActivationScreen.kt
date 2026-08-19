package com.tangem.features.tangempay.card.activation

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.domain.pay.model.CARD_ACTIVATION_LAST_DIGITS_LENGTH
import com.tangem.features.tangempay.details.impl.R
import com.tangem.core.ui.R as CoreUiR

private const val TABULAR_FIGURES_FEATURE = "tnum"
private const val LEADING_GROUPS_COUNT = 3

private val CardWidth = 566.dp
private val CardHeight = 364.dp
private val NumberInsetEnd = 32.dp
private val NumberInsetTop = 284.dp
private val NumberToTopBarGap = 128.dp
private val MinCardToFooterGap = 32.dp
private val NumberGroupGap = 16.dp

@Composable
internal fun TangemPayCardActivationScreen(state: TangemPayCardActivationUM, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(
                modifier = Modifier.testTag(TangemPayTestTags.CARD_ACTIVATION_TOP_BAR),
                title = resourceReference(R.string.tangempay_card_details_activate),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = state.onCloseClick,
            )
        },
    ) { contentPadding ->
        val numberStyle = TangemTheme.typography3.display.medium.copy(
            color = TangemTheme.colors3.text.primary,
            fontFeatureSettings = TABULAR_FIGURES_FEATURE,
        )
        ActivationContentLayout(
            topBarHeight = contentPadding.calculateTopPadding(),
            numberBottom = NumberInsetTop + with(LocalDensity.current) { numberStyle.lineHeight.toDp() },
            modifier = Modifier.fillMaxSize(),
            card = {
                CardNumberField(
                    state = state,
                    numberStyle = numberStyle,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            footer = {
                ActivationFooter(
                    state = state,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding(),
                )
            },
        )
    }
}

@Composable
private fun ActivationContentLayout(
    topBarHeight: Dp,
    numberBottom: Dp,
    modifier: Modifier = Modifier,
    card: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            Box { card() }
            Box { footer() }
        },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val cardPlaceable = measurables[0].measure(childConstraints)
        val footerPlaceable = measurables[1].measure(childConstraints)

        val footerTop = constraints.maxHeight - footerPlaceable.height
        val anchoredCardTop = (topBarHeight + NumberToTopBarGap - NumberInsetTop).roundToPx()
        val maxCardTop = footerTop - (MinCardToFooterGap + numberBottom).roundToPx()

        layout(constraints.maxWidth, constraints.maxHeight) {
            cardPlaceable.place(x = 0, y = minOf(anchoredCardTop, maxCardTop))
            footerPlaceable.place(x = 0, y = footerTop)
        }
    }
}

@Composable
private fun CardNumberField(state: TangemPayCardActivationUM, numberStyle: TextStyle, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val enteredColor = TangemTheme.colors3.text.primary
    val maskColor = TangemTheme.colors3.text.tertiary
    val visualTransformation = remember(enteredColor, maskColor) {
        LastDigitsVisualTransformation(enteredColor = enteredColor, maskColor = maskColor)
    }

    BasicTextField(
        value = state.lastDigits,
        onValueChange = state.onLastDigitsChange,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
            )
            .focusRequester(focusRequester)
            .testTag(TangemPayTestTags.CARD_ACTIVATION_INPUT_FIELD),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { state.onContinueClick() }),
        singleLine = true,
        readOnly = state.isLoading,
        textStyle = numberStyle,
        cursorBrush = SolidColor(enteredColor),
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            CardNumberDecoration(
                cardImageUrl = state.cardImageUrl,
                leadingGroups = { LeadingGroups(numberStyle = numberStyle) },
                editableGroup = innerTextField,
            )
        },
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun CardNumberDecoration(
    cardImageUrl: String?,
    modifier: Modifier = Modifier,
    leadingGroups: @Composable () -> Unit,
    editableGroup: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clipToBounds(),
        content = {
            CardArtwork(cardImageUrl = cardImageUrl)
            leadingGroups()
            editableGroup()
        },
    ) { measurables, constraints ->
        val cardPlaceable = measurables[0].measure(
            Constraints.fixed(width = CardWidth.roundToPx(), height = CardHeight.roundToPx()),
        )
        val leadingPlaceable = measurables[1].measure(Constraints())
        val gap = NumberGroupGap.roundToPx()
        val groupWidth = (leadingPlaceable.width - gap * (LEADING_GROUPS_COUNT - 1)) / LEADING_GROUPS_COUNT
        val editablePlaceable = measurables[2].measure(Constraints.fixedWidth(groupWidth))

        val editableX = (constraints.maxWidth - editablePlaceable.width) / 2
        val editableY = NumberInsetTop.roundToPx()
        val leadingX = editableX - gap - leadingPlaceable.width
        val leadingY = editableY + (editablePlaceable.height - leadingPlaceable.height) / 2

        layout(constraints.maxWidth, CardHeight.roundToPx()) {
            cardPlaceable.place(
                x = editableX + editablePlaceable.width + NumberInsetEnd.roundToPx() - cardPlaceable.width,
                y = editableY - NumberInsetTop.roundToPx(),
            )
            leadingPlaceable.place(x = leadingX, y = leadingY)
            editablePlaceable.place(x = editableX, y = editableY)
        }
    }
}

@Composable
private fun LeadingGroups(numberStyle: TextStyle, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(NumberGroupGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(LEADING_GROUPS_COUNT) {
            Text(
                text = MASK_DIGIT.repeat(CARD_ACTIVATION_LAST_DIGITS_LENGTH),
                style = numberStyle,
                color = TangemTheme.colors3.text.tertiary,
            )
        }
    }
}

@Composable
private fun CardArtwork(cardImageUrl: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(LocalContext.current)
            .data(cardImageUrl)
            .crossfade(true)
            .build(),
        placeholder = painterResource(R.drawable.img_tangem_pay_card_placeholder),
        error = painterResource(R.drawable.img_tangem_pay_card_placeholder),
        fallback = painterResource(R.drawable.img_tangem_pay_card_placeholder),
        contentScale = ContentScale.FillBounds,
        contentDescription = null,
    )
}

@Composable
private fun ActivationFooter(state: TangemPayCardActivationUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TangemPayTestTags.CARD_ACTIVATION_HINT),
            text = state.hint.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = if (state.isHintError) {
                TangemTheme.colors3.text.status.error
            } else {
                TangemTheme.colors3.text.secondary
            },
            textAlign = TextAlign.Center,
        )
        SpacerH(12.dp)
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TangemPayTestTags.CARD_ACTIVATION_CONTINUE_BUTTON),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(CoreUiR.string.common_continue),
            isEnabled = state.isContinueEnabled,
            isLoading = state.isLoading,
            onClick = state.onContinueClick,
        )
    }
}

private fun previewActivationState(lastDigits: String = "", isHintError: Boolean = false, isLoading: Boolean = false) =
    TangemPayCardActivationUM(
        lastDigits = lastDigits,
        cardImageUrl = null,
        hint = when {
            isLoading -> resourceReference(R.string.tangempay_card_activation_in_progress)
            isHintError -> resourceReference(R.string.tangempay_card_activation_error)
            else -> resourceReference(R.string.tangempay_card_activation_description)
        },
        isHintError = isHintError,
        isLoading = isLoading,
        onLastDigitsChange = {},
        onContinueClick = {},
        onCloseClick = {},
    )

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Light")
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark",
)
@Composable
private fun TangemPayCardActivationScreenPreview(
    @PreviewParameter(CardActivationPreviewProvider::class) state: TangemPayCardActivationUM,
) {
    TangemThemePreviewRedesign {
        TangemPayCardActivationScreen(state = state)
    }
}

private class CardActivationPreviewProvider :
    CollectionPreviewParameterProvider<TangemPayCardActivationUM>(
        collection = listOf(
            previewActivationState(),
            previewActivationState(lastDigits = "82"),
            previewActivationState(lastDigits = "8252"),
            previewActivationState(isHintError = true),
            previewActivationState(lastDigits = "8252", isLoading = true),
        ),
    )