package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.utils.CardDetailsFormatUtil
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.persistentListOf

private const val CARD_NUMBER_SART_DIGITS_COUNT = 12
private const val DATE_PART_LENGTH = 2
private const val CVV_LENGTH = 3

@Suppress("LongParameterList")
internal class TangemPayDetailsStateFactory(
    private val cardNumberEnd: String,
    private val onBack: () -> Unit,
    private val onRefresh: (ShowRefreshState) -> Unit,
    private val onReceive: () -> Unit,
    private val onReveal: () -> Unit,
    private val onCopy: (String) -> Unit,
    private val onClickChangePin: () -> Unit,
    private val onClickFreezeCard: () -> Unit,
) {

    private val cardStartMasked = maskedBlock(CARD_NUMBER_SART_DIGITS_COUNT)
    private val dateMasked = maskedBlock(DATE_PART_LENGTH)
    private val cvvMasked = maskedBlock(CVV_LENGTH)

    fun getInitialState() = TangemPayDetailsUM(
        topBarConfig = TangemPayDetailsTopBarConfig(
            onBackClick = onBack,
            items = persistentListOf(
                TangemDropdownMenuItem(
                    title = TextReference.Res(R.string.tangempay_card_details_change_pin),
                    textColorProvider = { TangemTheme.colors.text.primary1 },
                    onClick = onClickChangePin,
                ),
                TangemDropdownMenuItem(
                    title = TextReference.Res(R.string.tangempay_card_details_freeze_card),
                    textColorProvider = { TangemTheme.colors.text.primary1 },
                    onClick = onClickFreezeCard,
                ),
            ),
        ),
        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = onRefresh),
        balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
            actionButtons = persistentListOf(
                ActionButtonConfig(
                    text = resourceReference(id = R.string.common_receive),
                    iconResId = R.drawable.ic_arrow_down_24,
                    onClick = onReceive,
                ),
            ),
        ),
        cardDetailsUM = TangemPayCardDetailsUM(
            number = CardDetailsFormatUtil.formatCardNumber(cardNumber = "$cardStartMasked$cardNumberEnd"),
            expiry = CardDetailsFormatUtil.formatDate(month = dateMasked, year = dateMasked),
            cvv = cvvMasked,
            buttonText = TextReference.Res(R.string.tangempay_card_details_reveal_text),
            onClick = onReveal,
            onCopy = onCopy,
            isHidden = true,
        ),
        isBalanceHidden = false,

    )

    private fun maskedBlock(count: Int) = StringsSigns.DOT.repeat(count)
}