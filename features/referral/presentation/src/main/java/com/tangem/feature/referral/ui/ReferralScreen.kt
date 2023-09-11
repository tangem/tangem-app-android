package com.tangem.feature.referral.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.referral.domain.models.ExpectedAward
import com.tangem.feature.referral.domain.models.ExpectedAwards
import com.tangem.feature.referral.models.DemoModeException
import com.tangem.feature.referral.models.ReferralStateHolder
import com.tangem.feature.referral.models.ReferralStateHolder.*
import com.tangem.feature.referral.presentation.R
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch

/**
 * Referral program screen for participant and non-participant
 *
 * @param stateHolder state holder
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ReferralScreen(stateHolder: ReferralStateHolder, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed),
    )

    BottomSheetScaffold(
        modifier = modifier,
        sheetContent = {
            AgreementBottomSheetContent(
                url = when (val state = stateHolder.referralInfoState) {
                    is ReferralInfoState.NonParticipantContent -> state.url
                    is ReferralInfoState.ParticipantContent -> state.url
                    is ReferralInfoState.Loading -> ""
                },
            )
        },
        scaffoldState = bottomSheetScaffoldState,
        sheetShape = RoundedCornerShape(
            topStart = TangemTheme.dimens.radius16,
            topEnd = TangemTheme.dimens.radius16,
        ),
        sheetElevation = TangemTheme.dimens.elevation24,
        sheetPeekHeight = TangemTheme.dimens.size0,
        content = {
            ReferralContent(
                stateHolder = stateHolder,
                onAgreementClick = {
                    stateHolder.analytics.onAgreementClicked.invoke()
                    coroutineScope.launch {
                        if (bottomSheetScaffoldState.bottomSheetState.isCollapsed) {
                            bottomSheetScaffoldState.bottomSheetState.expand()
                        } else {
                            bottomSheetScaffoldState.bottomSheetState.collapse()
                        }
                    }
                },
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReferralContent(stateHolder: ReferralStateHolder, onAgreementClick: () -> Unit) {
    val isCopyButtonPressed = remember { mutableStateOf(false) }

    Box {
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyColumn(
                modifier = Modifier
                    .background(color = TangemTheme.colors.background.secondary)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                stickyHeader {
                    AppBarWithBackButton(
                        text = stringResource(R.string.details_referral_title),
                        onBackClick = stateHolder.headerState.onBackClicked,
                    )
                }
                item { Header() }
                item {
                    ReferralInfo(
                        stateHolder = stateHolder,
                        onAgreementClick = onAgreementClick,
                        onShowCopySnackbar = { isCopyButtonPressed.value = true },
                    )
                }
            }
        }

        ErrorSnackbarHost(errorSnackbar = stateHolder.errorSnackbar)
        CopySnackbarHost(isCopyButtonPressed = isCopyButtonPressed)
    }
}

@Composable
private fun Header() {
    Column {
        Image(
            painter = painterResource(R.drawable.ill_businessman_3d),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing32)
                .fillMaxWidth()
                .height(TangemTheme.dimens.size200),
        )
        SpacerH24()
        Text(
            text = stringResource(id = R.string.referral_title),
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing50),
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = TangemTheme.typography.h2,
        )
        SpacerH16()
    }
}

@Composable
private fun ReferralInfo(
    stateHolder: ReferralStateHolder,
    onAgreementClick: () -> Unit,
    onShowCopySnackbar: () -> Unit,
) {
    when (val state = stateHolder.referralInfoState) {
        is ReferralInfoState.ParticipantContent -> {
            Conditions(state = state)
            ParticipateBottomBlock(
                purchasedWalletCount = state.purchasedWalletCount,
                code = state.code,
                shareLink = state.shareLink,
                expectedAwards = state.expectedAwards,
                onAgreementClick = onAgreementClick,
                onShowCopySnackbar = onShowCopySnackbar,
                onCopyClick = stateHolder.analytics.onCopyClicked,
                onShareClick = stateHolder.analytics.onShareClicked,
            )
        }

        is ReferralInfoState.NonParticipantContent -> {
            Conditions(state = state)
            Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing44))
            NonParticipateBottomBlock(
                onAgreementClick = onAgreementClick,
                onParticipateClick = state.onParticipateClicked,
            )
        }

        is ReferralInfoState.Loading -> {
            LoadingCondition(iconResId = R.drawable.ic_tether_28)
            SpacerH32()
            LoadingCondition(iconResId = R.drawable.ic_discount_28)
        }
    }
}

@Composable
private fun Conditions(state: ReferralInfoContentState) {
    ConditionForYou(state = state)
    SpacerH32()
    ConditionForYourFriend(discount = state.discount)
}

@Composable
private fun ConditionForYou(state: ReferralInfoContentState) {
    Condition(iconResId = R.drawable.ic_tether_28) {
        when (state) {
            is ReferralInfoState.ParticipantContent -> InfoForYou(
                award = state.award,
                networkName = state.networkName,
                address = state.address,
            )

            is ReferralInfoState.NonParticipantContent -> InfoForYou(
                award = state.award,
                networkName = state.networkName,
            )
        }
    }
}

@Composable
private fun ConditionForYourFriend(discount: String) {
    Condition(iconResId = R.drawable.ic_discount_28) {
        InfoForYourFriend(discount = discount)
    }
}

@Composable
private fun LoadingCondition(@DrawableRes iconResId: Int) {
    Condition(iconResId = iconResId) {
        ShimmerInfo()
    }
}

@Composable
private fun Condition(@DrawableRes iconResId: Int, infoBlock: @Composable () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.button.secondary,
                    shape = RoundedCornerShape(TangemTheme.dimens.radius16),
                )
                .size(TangemTheme.dimens.size56),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(TangemTheme.dimens.size28),
                tint = TangemTheme.colors.icon.primary1,
            )
        }
        infoBlock()
    }
}

@Composable
private fun InfoForYou(award: String, networkName: String, address: String? = null) {
    ConditionInfo(title = stringResource(id = R.string.referral_point_currencies_title)) {
        Text(
            formatAwardConditionsString(
                quantity = award,
                network = networkName,
                address = if (!address.isNullOrBlank()) " $address" else "",
            ),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
        )
    }
}

@Composable
private fun formatAwardConditionsString(quantity: String, network: String, address: String): AnnotatedString {
    val rawString = stringResource(R.string.referral_point_currencies_description, quantity, network, address)

    val pattern = Regex("\\^\\^(.*?)\\^\\^")
    var startIndex = 0
    val annotatedString = buildAnnotatedString {
        pattern.findAll(rawString).forEach { matchResult ->
            val index = matchResult.range.first
            val matchedValue = matchResult.groups[1]?.value ?: ""

            // appends unformatted part
            append(rawString.substring(startIndex, index))

            // applies style on ^^-wrapped parts
            withStyle(SpanStyle(color = TangemTheme.colors.text.primary1)) {
                append(matchedValue)
            }

            // goes to next part
            startIndex = matchResult.range.last + 1
        }

        // appends remaining ending if exists
        append(rawString.substring(startIndex))
    }

    return annotatedString
}

@Composable
private fun InfoForYourFriend(discount: String) {
    ConditionInfo(title = stringResource(id = R.string.referral_point_discount_title)) {
        Text(
            text = buildAnnotatedString {
                append(stringResource(id = R.string.referral_point_discount_description_prefix))
                withStyle(SpanStyle(color = TangemTheme.colors.text.primary1)) {
                    append(
                        String.format(
                            stringResource(id = R.string.referral_point_discount_description_value),
                            " $discount",
                        ),
                    )
                }
                append(" ")
                append(stringResource(id = R.string.referral_point_discount_description_suffix))
            },
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
        )
    }
}

@Composable
private fun ConditionInfo(title: String, subtitleContent: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2)) {
        Text(
            text = title,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle1,
        )
        subtitleContent()
    }
}

@Composable
private fun ShimmerInfo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TangemTheme.dimens.spacing4)
            .shimmer(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing10),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius6))
                .width(TangemTheme.dimens.size102)
                .height(TangemTheme.dimens.size16)
                .background(TangemColorPalette.White),
            // .background(Color(0xFFF8F8F8)),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius6))
                .width(TangemTheme.dimens.size40)
                .height(TangemTheme.dimens.size12)
                .background(TangemColorPalette.White),
        )
    }
}

// TODO() Replace component with component from ds
@Composable
private fun BoxScope.ErrorSnackbarHost(errorSnackbar: ErrorSnackbar?) {
    if (errorSnackbar != null) {
        val snackbarHostState by remember { mutableStateOf(SnackbarHostState()) }
        val coroutineScope = rememberCoroutineScope()

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = {
                Snackbar(
                    snackbarData = it,
                    modifier = Modifier.fillMaxWidth(),
                    actionOnNewLine = true,
                    shape = RoundedCornerShape(size = TangemTheme.dimens.radius8),
                    backgroundColor = TangemColorPalette.Black,
                    contentColor = TangemTheme.colors.text.primary2,
                    actionColor = TangemTheme.colors.text.primary2,
                    elevation = TangemTheme.dimens.elevation3,
                )
            },
        )

        val actionLabel = stringResource(id = R.string.warning_button_ok)
        val message = getMessageForErrorSnackbar(errorSnackbar)
        SideEffect {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = SnackbarDuration.Indefinite,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    errorSnackbar.onOkClicked()
                }
            }
        }
    }
}

// TODO() Replace component with component from ds
@Composable
private fun BoxScope.CopySnackbarHost(isCopyButtonPressed: MutableState<Boolean>) {
    if (isCopyButtonPressed.value) {
        val snackbarHostState by remember { mutableStateOf(SnackbarHostState()) }
        val coroutineScope = rememberCoroutineScope()

        var snackbarSize by remember { mutableStateOf(0) }
        val width = LocalConfiguration.current.screenWidthDp.dp
        val snackbarWidth = with(LocalDensity.current) { snackbarSize.toDp() }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = (width - snackbarWidth).div(2), bottom = TangemTheme.dimens.spacing12)
                .fillMaxWidth(),
            snackbar = {
                Box(
                    modifier = Modifier
                        .onSizeChanged { snackbarSize = it.width }
                        .background(TangemColorPalette.Black, RoundedCornerShape(size = TangemTheme.dimens.radius8))
                        .shadow(
                            TangemTheme.dimens.elevation3,
                            RoundedCornerShape(size = TangemTheme.dimens.radius8),
                        )
                        .padding(
                            horizontal = TangemTheme.dimens.spacing16,
                            vertical = TangemTheme.dimens.spacing14,
                        ),
                ) {
                    Text(
                        text = it.message,
                        color = TangemTheme.colors.text.primary2,
                        style = TangemTheme.typography.body2,
                    )
                }
            },
        )

        val message = stringResource(id = R.string.referral_promo_code_copied)
        SideEffect {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short,
                )
                isCopyButtonPressed.value = false
            }
        }
    }
}

@Composable
private fun getMessageForErrorSnackbar(errorSnackbar: ErrorSnackbar): String {
    return when (errorSnackbar.throwable) {
        is DemoModeException -> {
            stringResource(id = R.string.alert_demo_feature_disabled)
        }

        else -> {
            if (errorSnackbar.throwable.cause != null) {
                String.format(
                    format = stringResource(id = R.string.referral_error_failed_to_load_info_with_reason),
                    errorSnackbar.throwable.cause,
                )
            } else {
                stringResource(id = R.string.referral_error_failed_to_load_info)
            }
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview_ReferralScreen_Participant_InLightTheme() {
    TangemTheme(isDark = false) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.ParticipantContent(
                    award = "10 USDT",
                    networkName = "Tron",
                    address = "ma80...zk8q2",
                    discount = "10%",
                    purchasedWalletCount = 3,
                    code = "x4JdK",
                    shareLink = "",
                    url = "",
                    expectedAwards = null,
                ),
                errorSnackbar = null,
                analytics = Analytics(
                    onAgreementClicked = {},
                    onCopyClicked = {},
                    onShareClicked = {},
                ),
            ),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview_ReferralScreen_Participant_InDarkTheme() {
    TangemTheme(isDark = true) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.ParticipantContent(
                    award = "10 USDT",
                    networkName = "Tron",
                    address = "ma80...zk8q2",
                    discount = "10%",
                    purchasedWalletCount = 3,
                    code = "x4JdK",
                    shareLink = "",
                    url = "",
                    expectedAwards = null,
                ),
                errorSnackbar = null,
                analytics = Analytics(
                    onAgreementClicked = {},
                    onCopyClicked = {},
                    onShareClicked = {},
                ),
            ),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview_ReferralScreen_Participant_With_Referrals_InLightTheme() {
    TangemTheme(isDark = false) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.ParticipantContent(
                    award = "10 USDT",
                    networkName = "Tron",
                    address = "ma80...zk8q2",
                    discount = "10%",
                    purchasedWalletCount = 3,
                    code = "x4JdK",
                    shareLink = "",
                    url = "",
                    expectedAwards = ExpectedAwards(
                        numberOfWallets = 5,
                        expectedAwards = listOf(
                            ExpectedAward(
                                amount = "10 USDT",
                                paymentDate = "Today",
                            ),
                            ExpectedAward(
                                amount = "20 USDT",
                                paymentDate = "6 Aug 2023",
                            ),
                            ExpectedAward(
                                amount = "30 USDT",
                                paymentDate = "10 Aug 2023",
                            ),
                        ),
                    ),
                ),
                errorSnackbar = null,
                analytics = Analytics(
                    onAgreementClicked = {},
                    onCopyClicked = {},
                    onShareClicked = {},
                ),
            ),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview_ReferralScreen_NonParticipant_InLightTheme() {
    TangemTheme(isDark = false) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.NonParticipantContent(
                    award = "10 USDT",
                    networkName = "Tron",
                    discount = "10%",
                    url = "",
                    onParticipateClicked = {},
                ),
                errorSnackbar = null,
                analytics = Analytics(
                    onAgreementClicked = {},
                    onCopyClicked = {},
                    onShareClicked = {},
                ),
            ),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview_ReferralScreen_NonParticipant_InDarkTheme() {
    TangemTheme(isDark = true) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.NonParticipantContent(
                    award = "10 USDT",
                    networkName = "Tron",
                    discount = "10%",
                    url = "",
                    onParticipateClicked = {},
                ),
                errorSnackbar = null,
                analytics = Analytics(
                    onAgreementClicked = {},
                    onCopyClicked = {},
                    onShareClicked = {},
                ),
            ),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview_ReferralScreen_Loading_InLightTheme() {
    TangemTheme(isDark = false) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.Loading,
                errorSnackbar = null,
                analytics = Analytics(
                    onAgreementClicked = {},
                    onCopyClicked = {},
                    onShareClicked = {},
                ),
            ),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview_ReferralScreen_Loading_InDarkTheme() {
    TangemTheme(isDark = true) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.Loading,
                errorSnackbar = null,
                analytics = Analytics(
                    onAgreementClicked = {},
                    onCopyClicked = {},
                    onShareClicked = {},
                ),
            ),
        )
    }
}
