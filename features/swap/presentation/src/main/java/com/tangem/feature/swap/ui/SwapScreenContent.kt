package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.swap.domain.models.ui.FeeType
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongMethod")
@Composable
internal fun SwapScreenContent(state: SwapStateHolder, modifier: Modifier = Modifier) {
    val keyboard by keyboardAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors.background.secondary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    top = TangemTheme.dimens.spacing8,
                    bottom = TangemTheme.dimens.spacing32,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        ) {
            MainInfo(state)

            ProviderItemBlock(state = state.providerState)

            FeeItemBlock(state = state.fee)

            if (state.notifications.isNotEmpty()) SwapNotifications(notifications = state.notifications)

            MainButton(state = state, onPermissionWarningClick = state.onShowPermissionBottomSheet)

            if (state.tosState != null && state.providerState !is ProviderState.Empty) {
                ProviderTos(
                    tosState = state.tosState,
                    modifier = Modifier
                        .padding(top = TangemTheme.dimens.spacing16),
                )
            }
        }

        if (state.shouldShowMaxAmount && keyboard is Keyboard.Opened) {
            Text(
                text = stringResourceSafe(id = R.string.send_max_amount_label),
                style = TangemTheme.typography.button,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .fillMaxWidth()
                    .background(TangemTheme.colors.button.secondary)
                    .clickable { state.onMaxAmountSelected?.invoke() }
                    .padding(
                        horizontal = TangemTheme.dimens.spacing14,
                        vertical = TangemTheme.dimens.spacing16,
                    ),
                textAlign = TextAlign.Start,
            )
        }

        SwapEventEffect(
            event = state.event,
        )
    }
}

@Composable
private fun MainInfo(state: SwapStateHolder) {
    ConstraintLayout(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val (topCard, bottomCard, button) = createRefs()
        val priceImpact = state.priceImpact
        TransactionCardData(
            priceImpact = priceImpact,
            swapCardState = state.sendCardData,
            modifier = Modifier.constrainAs(topCard) {
                top.linkTo(parent.top)
            },
            onSelectTokenClick = state.onSelectTokenClick,
        )
        val marginCard = TangemTheme.dimens.spacing12
        TransactionCardData(
            priceImpact = priceImpact,
            swapCardState = state.receiveCardData,
            modifier = Modifier.constrainAs(bottomCard) {
                top.linkTo(topCard.bottom, margin = marginCard)
            },
            onSelectTokenClick = state.onSelectTokenClick,
        )
        val marginButton = TangemTheme.dimens.spacing30
        SwapButton(
            state,
            modifier = Modifier.constrainAs(button) {
                bottom.linkTo(topCard.bottom, margin = -marginButton)
                start.linkTo(topCard.start)
                end.linkTo(topCard.end)
            },
        )
    }
}

@Composable
private fun TransactionCardData(
    priceImpact: PriceImpact,
    swapCardState: SwapCardState,
    onSelectTokenClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    when (swapCardState) {
        is SwapCardState.Empty -> {
            TransactionCardEmpty(
                type = swapCardState.type,
                amountEquivalent = swapCardState.amountEquivalent,
                textFieldValue = swapCardState.amountTextFieldValue,
                onChangeTokenClick = if (swapCardState.canSelectAnotherToken) onSelectTokenClick else null,
                modifier = modifier,
            )
        }
        is SwapCardState.SwapCardData -> {
            TransactionCard(
                type = swapCardState.type,
                balance = swapCardState.balance.orMaskWithStars(swapCardState.isBalanceHidden),
                textFieldValue = swapCardState.amountTextFieldValue,
                amountEquivalent = swapCardState.amountEquivalent,
                tokenIconUrl = swapCardState.tokenIconUrl ?: "",
                tokenCurrency = swapCardState.tokenCurrency,
                priceImpact = priceImpact,
                networkIconRes = if (swapCardState.isNotNativeToken) swapCardState.networkIconRes else null,
                iconPlaceholder = swapCardState.coinId?.let {
                    getActiveIconResByCoinId(it)
                },
                onChangeTokenClick = if (swapCardState.canSelectAnotherToken) onSelectTokenClick else null,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ProviderTos(tosState: TosState, modifier: Modifier = Modifier) {
    val tos = tosState.tosLink
    val policy = tosState.policyLink
    if (tos == null && policy == null) return

    val (annotatedString, click) = getAnnotatedStringForLegalsWithClick(tos, policy)

    ClickableText(
        text = annotatedString,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing54),
        style = TangemTheme.typography.caption2.copy(textAlign = TextAlign.Center),
        onClick = click,
    )
}

@Composable
private fun getAnnotatedStringForLegalsWithClick(
    tos: LegalState?,
    policy: LegalState?,
): Pair<AnnotatedString, (Int) -> Unit> {
    return if (tos != null && policy != null) {
        val tosTitle = tos.title.resolveReference()
        val policyTitle = policy.title.resolveReference()
        val fullString = stringResourceSafe(id = R.string.express_legal_two_placeholders, tosTitle, policyTitle)
        val tosIndex = fullString.indexOf(tosTitle)
        val policyIndex = fullString.indexOf(policyTitle)
        val string = buildAnnotatedString {
            withStyle(SpanStyle(color = TangemTheme.colors.text.tertiary)) {
                append(fullString.substring(0, tosIndex))
            }
            withStyle(SpanStyle(color = TangemTheme.colors.text.accent)) {
                append(fullString.substring(tosIndex, tosIndex + tosTitle.length))
            }
            withStyle(SpanStyle(color = TangemTheme.colors.text.tertiary)) {
                append(fullString.substring(tosIndex + tosTitle.length, policyIndex))
            }
            withStyle(SpanStyle(color = TangemTheme.colors.text.accent)) {
                append(fullString.substring(policyIndex, policyIndex + policyTitle.length))
            }
        }
        val click = { i: Int ->
            val tosStyle = requireNotNull(string.spanStyles.getOrNull(1))
            if (i in tosStyle.start..tosStyle.end) {
                tos.onClick(tos.link)
            }
            val policyStyle = requireNotNull(string.spanStyles.lastOrNull())
            if (i in policyStyle.start..policyStyle.end) {
                policy.onClick(policy.link)
            }
        }
        string to click
    } else {
        val legal = requireNotNull(tos ?: policy) { "tos or policy must not be null" }
        val legalTitle = legal.title
            .resolveReference()
        val fullString = stringResourceSafe(id = R.string.express_legal_one_placeholder, legal)
        val legalIndex = fullString.indexOf(legalTitle)
        val string = buildAnnotatedString {
            withStyle(SpanStyle(color = TangemTheme.colors.text.tertiary)) {
                append(fullString.substring(0, legalIndex))
            }
            withStyle(SpanStyle(color = TangemTheme.colors.text.accent)) {
                append(fullString.substring(legalIndex, legalIndex + legalTitle.length))
            }
        }
        val click = { i: Int ->
            val legalStyle = requireNotNull(string.spanStyles.lastOrNull())
            if (i in legalStyle.start..legalStyle.end) {
                legal.onClick(legal.link)
            }
        }
        string to click
    }
}

@Composable
private fun SwapButton(state: SwapStateHolder, modifier: Modifier = Modifier) {
    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 2.dp,
        ),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = TangemTheme.colors.background.action,
            disabledContainerColor = TangemTheme.colors.background.action,
            contentColor = TangemTheme.colors.text.primary1,
            disabledContentColor = TangemTheme.colors.text.primary1,
        ),
        modifier = modifier.size(TangemTheme.dimens.size48),
        onClick = state.onChangeCardsClicked,
        enabled = state.changeCardsButtonState == ChangeCardsButtonState.ENABLED,
    ) {
        when (state.changeCardsButtonState) {
            ChangeCardsButtonState.UPDATE_IN_PROGRESS -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(TangemTheme.dimens.size16)
                        .padding(TangemTheme.dimens.spacing14),
                    color = TangemTheme.colors.icon.primary1,
                    strokeWidth = TangemTheme.dimens.size2,
                )
            }
            ChangeCardsButtonState.ENABLED -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_exchange_vertical_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.text.primary1,
                    modifier = Modifier.padding(TangemTheme.dimens.spacing12),
                )
            }
            ChangeCardsButtonState.DISABLED -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_exchange_vertical_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.text.disabled,
                    modifier = Modifier.padding(TangemTheme.dimens.spacing12),
                )
            }
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun SwapNotifications(notifications: List<NotificationUM>) {
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxWidth(),
    ) {
        notifications.forEach { notification ->
            when (notification) {
                is SwapNotificationUM.Error.ApprovalInProgressWarning,
                is SwapNotificationUM.Error.TransactionInProgressWarning,
                -> {
                    CardWithIcon(
                        title = notification.config.title?.resolveReference().orEmpty(),
                        description = notification.config.subtitle.resolveReference(),
                        icon = {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(TangemTheme.dimens.size16),
                                color = TangemTheme.colors.icon.primary1,
                                strokeWidth = TangemTheme.dimens.size2,
                            )
                        },
                    )
                }
                else -> {
                    Notification(
                        config = notification.config,
                        iconTint = when (notification) {
                            is SwapNotificationUM.Error.UnableToCoverFeeWarning,
                            is NotificationUM.Error.TokenExceedsBalance,
                            is NotificationUM.Error.ExceedsBalance,
                            is NotificationUM.Info,
                            is NotificationUM.Warning,
                            -> null
                            is SwapNotificationUM.Error.GenericError,
                            is NotificationUM.Error,
                            -> TangemTheme.colors.icon.warning
                        },
                    )
                }
            }
            SpacerH8()
        }
    }
}

@Composable
private fun MainButton(state: SwapStateHolder, onPermissionWarningClick: () -> Unit) {
    // order is important
    when {
        state.isInsufficientFunds -> {
            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResourceSafe(id = R.string.swapping_insufficient_funds),
                enabled = false,
                onClick = state.swapButton.onClick,
            )
        }
        state.notifications.any { it is SwapNotificationUM.Info.PermissionNeeded } -> {
            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResourceSafe(id = R.string.give_permission_title),
                enabled = true,
                onClick = onPermissionWarningClick,
            )
        }
        else -> {
            PrimaryButtonIconEnd(
                modifier = Modifier.fillMaxWidth(),
                text = stringResourceSafe(id = R.string.swapping_swap_action),
                iconResId = R.drawable.ic_tangem_24,
                enabled = state.swapButton.enabled,
                onClick = state.swapButton.onClick,
            )
        }
    }
}

// region preview

private val sendCard = SwapCardState.SwapCardData(
    type = TransactionCardType.Inputtable({}, {}, TransactionCardType.InputError.Empty),
    amountTextFieldValue = TextFieldValue(),
    amountEquivalent = "1 000 000",
    tokenIconUrl = "",
    tokenCurrency = "DAI",
    isNotNativeToken = true,
    canSelectAnotherToken = false,
    balance = "123",
    coinId = "",
    token = null,
    networkIconRes = R.drawable.img_polygon_22,
    isBalanceHidden = false,
)

private val receiveCard = SwapCardState.SwapCardData(
    type = TransactionCardType.ReadOnly(),
    amountTextFieldValue = TextFieldValue(),
    amountEquivalent = "1 000 000",
    tokenIconUrl = "",
    tokenCurrency = "DAI",
    isNotNativeToken = true,
    canSelectAnotherToken = true,
    balance = "33333",
    coinId = "",
    token = null,
    networkIconRes = R.drawable.img_polygon_22,
    isBalanceHidden = false,
)

private val state = SwapStateHolder(
    sendCardData = sendCard,
    receiveCardData = receiveCard,
    fee = FeeItemState.Content(
        feeType = FeeType.NORMAL,
        title = stringReference("Fee"),
        amountCrypto = "100",
        symbolCrypto = "1000",
        amountFiatFormatted = "(100)",
        isClickable = true,
        onClick = {},
    ),
    notifications = persistentListOf(
        SwapNotificationUM.Info.PermissionNeeded(
            providerName = "Provider",
            fromTokenSymbol = "POL",
        ),
        SwapNotificationUM.Warning.NoAvailableTokensToSwap("POLYGON"),
    ),
    swapButton = SwapButton(enabled = true, onClick = {}),
    onRefresh = {},
    onBackClicked = {},
    onChangeCardsClicked = {},
    permissionState = GiveTxPermissionState.InProgress,
    blockchainId = "POLYGON",
    providerState = ProviderState.Loading(),
    priceImpact = PriceImpact.Empty(),
    shouldShowMaxAmount = true,
    isInsufficientFunds = false,
    onSuccess = {},
    onSelectTokenClick = {},
    tosState = TosState(
        tosLink = LegalState(
            title = stringReference("Terms of Use"),
            link = "https://tangem.com",
            onClick = {},
        ),
        policyLink = LegalState(
            title = stringReference("Privacy Policy"),
            link = "https://tangem.com",
            onClick = {},
        ),
    ),
)

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SwapScreenContentPreview() {
    TangemThemePreview {
        SwapScreenContent(state = state, modifier = Modifier)
    }
}

// endregion preview