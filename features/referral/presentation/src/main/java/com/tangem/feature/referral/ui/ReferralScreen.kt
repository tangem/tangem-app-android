package com.tangem.feature.referral.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.AppBarWithBackButton
import com.tangem.core.ui.components.VerticalSpacer
import com.tangem.core.ui.res.ButtonColorType
import com.tangem.core.ui.res.IconColorType
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TextColorType
import com.tangem.core.ui.res.White
import com.tangem.core.ui.res.buttonColor
import com.tangem.core.ui.res.iconColor
import com.tangem.core.ui.res.textColor
import com.tangem.feature.referral.models.ReferralStateHolder
import com.tangem.feature.referral.models.ReferralStateHolder.ReferralInfoState
import com.tangem.feature.referral.presentation.R
import com.valentinilk.shimmer.shimmer

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_ReferralScreen_Participant_InLightTheme() {
    TangemTheme(isDarkTheme = false) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = ReferralStateHolder.HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.ParticipantContent(
                    award = "10 USDT",
                    address = "ma80...zk8q2",
                    discount = "10%",
                    onAgreementClicked = {},
                    onParticipateClicked = {},
                ),
                effects = ReferralStateHolder.Effects(showErrorToast = false),
            ),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_ReferralScreen_Participant_InDarkTheme() {
    TangemTheme(isDarkTheme = true) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = ReferralStateHolder.HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.ParticipantContent(
                    award = "10 USDT",
                    address = "ma80...zk8q2",
                    discount = "10%",
                    onAgreementClicked = {},
                    onParticipateClicked = {},
                ),
                effects = ReferralStateHolder.Effects(showErrorToast = false),
            ),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_ReferralScreen_NonParticipant_InLightTheme() {
    TangemTheme(isDarkTheme = false) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = ReferralStateHolder.HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.NonParticipantContent(
                    award = "10 USDT",
                    discount = "10%",
                    purchasedWalletCount = 3,
                    code = "x4JdK",
                    onCopyClicked = {},
                    onShareClicked = {},
                    onAgreementClicked = {},
                ),
                effects = ReferralStateHolder.Effects(showErrorToast = false),
            ),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_ReferralScreen_NonParticipant_InDarkTheme() {
    TangemTheme(isDarkTheme = true) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = ReferralStateHolder.HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.NonParticipantContent(
                    award = "10 USDT",
                    discount = "10%",
                    purchasedWalletCount = 3,
                    code = "x4JdK",
                    onCopyClicked = {},
                    onShareClicked = {},
                    onAgreementClicked = {},
                ),
                effects = ReferralStateHolder.Effects(showErrorToast = false),
            ),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_ReferralScreen_Loading_InLightTheme() {
    TangemTheme(isDarkTheme = false) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = ReferralStateHolder.HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.Loading,
                effects = ReferralStateHolder.Effects(showErrorToast = false),
            ),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_ReferralScreen_Loading_InDarkTheme() {
    TangemTheme(isDarkTheme = true) {
        ReferralScreen(
            stateHolder = ReferralStateHolder(
                headerState = ReferralStateHolder.HeaderState(onBackClicked = {}),
                referralInfoState = ReferralInfoState.Loading,
                effects = ReferralStateHolder.Effects(showErrorToast = false),
            ),
        )
    }
}

/**
 * Referral program screen for participant and non-participant
 *
 * @param stateHolder state holder
 */
@Composable
fun ReferralScreen(stateHolder: ReferralStateHolder) {
    Column(
        modifier = Modifier
            .background(color = MaterialTheme.colors.primary)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header(stateHolder = stateHolder)
        VerticalSpacer(spaceResId = R.dimen.spacing16)
        ReferralInfo(stateHolder = stateHolder)
    }
}

@Composable
private fun ColumnScope.Header(stateHolder: ReferralStateHolder) {
    AppBar(stateHolder = stateHolder)
    Image(
        painter = painterResource(R.drawable.ill_businessman_3d),
        contentDescription = null,
        modifier = Modifier
            .padding(horizontal = dimensionResource(id = R.dimen.spacing32))
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.size200)),
    )
    VerticalSpacer(spaceResId = R.dimen.spacing24)
    Text(
        text = stringResource(id = R.string.referral_title),
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing50)),
        color = MaterialTheme.colors.textColor(type = TextColorType.PRIMARY1),
        textAlign = TextAlign.Center,
        maxLines = 2,
        style = MaterialTheme.typography.h2,
    )
}

@Composable
fun ReferralInfo(stateHolder: ReferralStateHolder) {
    when (val state = stateHolder.referralInfoState) {
        is ReferralInfoState.ParticipantContent,
        is ReferralInfoState.NonParticipantContent -> {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing32))) {
                ConditionForYou(state = state as ReferralStateHolder.ReferralInfoContentState)
                ConditionForYourFriend(state = state)
            }
        }
        is ReferralInfoState.Loading -> {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing32))) {
                LoadingCondition(iconResId = R.drawable.ic_tether)
                LoadingCondition(iconResId = R.drawable.ic_discount)
            }
        }
    }
}

@Composable
private fun AppBar(stateHolder: ReferralStateHolder) {
    AppBarWithBackButton(
        text = stringResource(R.string.details_referral_title),
        onBackClick = stateHolder.headerState.onBackClicked,
    )
}

@Composable
private fun ConditionForYou(state: ReferralStateHolder.ReferralInfoContentState) {
    Condition(iconResId = R.drawable.ic_tether) {
        when (state) {
            is ReferralInfoState.ParticipantContent -> InfoForYou(award = state.award, address = state.address)
            is ReferralInfoState.NonParticipantContent -> InfoForYou(award = state.award)
        }
    }
}

@Composable
private fun ConditionForYourFriend(state: ReferralStateHolder.ReferralInfoContentState) {
    Condition(iconResId = R.drawable.ic_discount) {
        when (state) {
            is ReferralInfoState.ParticipantContent -> InfoForYourFriend(discount = state.discount)
            is ReferralInfoState.NonParticipantContent -> InfoForYourFriend(discount = state.discount)
        }
    }
}

@Composable
private fun LoadingCondition(@DrawableRes iconResId: Int) {
    Condition(iconResId = iconResId) {
        ShimmerCondition()
    }
}

@Composable
private fun Condition(@DrawableRes iconResId: Int, infoBlock: @Composable () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing16)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing12)),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colors.buttonColor(type = ButtonColorType.SECONDARY),
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.radius16)),
                )
                .size(dimensionResource(id = R.dimen.size56)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.size28)),
                tint = MaterialTheme.colors.iconColor(type = IconColorType.PRIMARY1),
            )
        }
        infoBlock()
    }
}

@Composable
private fun InfoForYou(award: String, address: String? = null) {
    ConditionInfo(title = stringResource(id = R.string.referral_point_currencies_title)) {
        Text(
            text = buildAnnotatedString {
                append(stringResource(id = R.string.referral_point_currencies_description_prefix))
                withStyle(SpanStyle(color = MaterialTheme.colors.textColor(type = TextColorType.PRIMARY1))) {
                    append(" $award ")
                }
                append(
                    String.format(
                        stringResource(id = R.string.referral_point_currencies_description_suffix),
                        if (!address.isNullOrBlank()) address else "",
                    ),
                )
            },
            color = MaterialTheme.colors.textColor(type = TextColorType.TERTIARY),
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
private fun InfoForYourFriend(discount: String) {
    ConditionInfo(title = stringResource(id = R.string.referral_point_discount_title)) {
        Text(
            text = buildString {
                append(stringResource(id = R.string.referral_point_discount_description_prefix))
                append(
                    String.format(
                        stringResource(id = R.string.referral_point_discount_description_value),
                        " $discount",
                    ),
                )
                append(" ")
                append(stringResource(id = R.string.referral_point_discount_description_suffix))
            },
            color = MaterialTheme.colors.textColor(type = TextColorType.TERTIARY),
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
private fun ConditionInfo(title: String, subtitleContent: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing2))) {
        Text(
            text = title,
            color = MaterialTheme.colors.textColor(type = TextColorType.PRIMARY1),
            style = MaterialTheme.typography.subtitle1,
        )
        subtitleContent()
    }
}

@Composable
private fun ShimmerCondition() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(id = R.dimen.spacing4))
            .shimmer(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing10)),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.radius6)))
                .width(dimensionResource(id = R.dimen.size102))
                .height(dimensionResource(id = R.dimen.size16))
                .background(White),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.radius6)))
                .width(dimensionResource(id = R.dimen.size40))
                .height(dimensionResource(id = R.dimen.size11))
                .background(White),
        )
    }
}

