package com.tangem.feature.referral.ui

import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.snackbar.CopiedTextSnackbar
import com.tangem.core.ui.components.snackbar.TangemSnackbar
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.referral.domain.models.ExpectedAward
import com.tangem.feature.referral.domain.models.ExpectedAwards
import com.tangem.feature.referral.models.DemoModeException
import com.tangem.feature.referral.models.ReferralStateHolder
import com.tangem.feature.referral.models.ReferralStateHolder.*
import com.tangem.feature.referral.presentation.R
import kotlinx.coroutines.launch

/**
 * Referral program screen for participant and non-participant
 *
 * @param stateHolder state holder
 * @param modifier    modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReferralScreen(stateHolder: ReferralStateHolder) {
    var isBottomSheetVisible by remember { mutableStateOf(value = false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val snackbarHostState = remember(::SnackbarHostState)

    Scaffold(
        topBar = {
            AppBarWithBackButton(
                modifier = Modifier.statusBarsPadding(),
                text = stringResource(R.string.details_referral_title),
                onBackClick = stateHolder.headerState.onBackClicked,
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .padding(bottom = TangemTheme.dimens.spacing108),
            ) {
// [REDACTED_TODO_COMMENT]
                if (stateHolder.errorSnackbar != null) {
                    TangemSnackbar(data = it, actionOnNewLine = true)
                } else {
                    CopiedTextSnackbar(it)
                }
            }
        },
        containerColor = TangemTheme.colors.background.secondary,
    ) {
        ReferralContent(
            stateHolder = stateHolder,
            snackbarHostState = snackbarHostState,
            onAgreementClick = {
                stateHolder.analytics.onAgreementClicked.invoke()
                isBottomSheetVisible = true
            },
            modifier = Modifier.padding(it),
        )
    }

    val errorSnackbar = stateHolder.errorSnackbar
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalContext.current.resources

    SideEffect {
        if (errorSnackbar != null) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = resources.getMessageForErrorSnackbar(errorSnackbar.throwable),
                    actionLabel = resources.getString(R.string.warning_button_ok),
                    duration = SnackbarDuration.Indefinite,
                )

                if (result == SnackbarResult.ActionPerformed) {
                    errorSnackbar.onOkClicked()
                }
            }
        }
    }

    ReferralBottomSheet(
        sheetState = sheetState,
        isVisible = isBottomSheetVisible,
        onDismissRequest = { isBottomSheetVisible = false },
        config = stateHolder.referralInfoState,
    )
}

@Composable
private fun ReferralContent(
    stateHolder: ReferralStateHolder,
    snackbarHostState: SnackbarHostState,
    onAgreementClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { Header() }
            item {
                ReferralInfo(
                    stateHolder = stateHolder,
                    snackbarHostState = snackbarHostState,
                    onAgreementClick = onAgreementClick,
                )
            }
        }
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
    snackbarHostState: SnackbarHostState,
    onAgreementClick: () -> Unit,
) {
    when (val state = stateHolder.referralInfoState) {
        is ReferralInfoState.ParticipantContent -> {
            Conditions(state = state)
            ParticipateBottomBlock(
                purchasedWalletCount = state.purchasedWalletCount,
                code = state.code,
                shareLink = state.shareLink,
                expectedAwards = state.expectedAwards,
                snackbarHostState = snackbarHostState,
                onAgreementClick = onAgreementClick,
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
            .padding(top = TangemTheme.dimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing10),
    ) {
        RectangleShimmer(
            modifier = Modifier
                .width(TangemTheme.dimens.size102)
                .height(TangemTheme.dimens.size16),
        )
        RectangleShimmer(
            modifier = Modifier
                .width(TangemTheme.dimens.size40)
                .height(TangemTheme.dimens.size12),
        )
    }
}

private fun Resources.getMessageForErrorSnackbar(throwable: Throwable): String {
    return when {
        throwable is DemoModeException -> getString(R.string.alert_demo_feature_disabled)
        throwable.cause != null -> getString(R.string.referral_error_failed_to_load_info_with_reason, throwable.cause)
        else -> getString(R.string.referral_error_failed_to_load_info)
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ReferralScreen_Participant() {
    TangemThemePreview {
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
                errorSnackbar = ErrorSnackbar(DemoModeException()) {},
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
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ReferralScreen_Participant_With_Referrals() {
    TangemThemePreview {
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
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ReferralScreen_NonParticipant() {
    TangemThemePreview {
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
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ReferralScreen_Loading() {
    TangemThemePreview {
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
