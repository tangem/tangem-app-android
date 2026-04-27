package com.tangem.common.ui.markets.action

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList

@Immutable
sealed class QuickActionUM(
    open val title: TextReference,
    open val description: TextReference,
    @param:DrawableRes open val icon: Int,
    open val isLongClickAvailable: Boolean = false,
) {

    sealed class V1(
        override val title: TextReference,
        override val description: TextReference,
        @param:DrawableRes override val icon: Int,
        override val isLongClickAvailable: Boolean = false,
    ) : QuickActionUM(
        title = title,
        description = description,
        icon = icon,
        isLongClickAvailable = isLongClickAvailable,
    ) {
        data object Buy : V1(
            title = resourceReference(R.string.common_buy),
            description = resourceReference(R.string.buy_token_description),
            icon = R.drawable.ic_plus_24,
        )

        data class Exchange(
            val shouldShowBadge: Boolean,
        ) : V1(
            title = resourceReference(R.string.common_exchange),
            description = resourceReference(R.string.exсhange_token_description),
            icon = R.drawable.ic_exchange_vertical_24,
        )

        data object Receive : V1(
            title = resourceReference(R.string.common_receive),
            description = resourceReference(R.string.receive_token_description),
            icon = R.drawable.ic_arrow_down_24,
            isLongClickAvailable = true,
        )

        data object Stake : V1(
            title = resourceReference(R.string.common_stake),
            description = resourceReference(R.string.stake_token_description),
            icon = R.drawable.ic_staking_24,
        )

        data class YieldMode(
            private val apy: String,
        ) : V1(
            title = resourceReference(R.string.common_yield_mode),
            description = resourceReference(R.string.yield_module_main_screen_promo_banner_message, wrappedList(apy)),
            icon = R.drawable.ic_analytics_up_mini_24,
        )
    }

    sealed class V2(
        override val title: TextReference,
        override val description: TextReference,
        @param:DrawableRes override val icon: Int,
        override val isLongClickAvailable: Boolean = false,
    ) : QuickActionUM(
        title = title,
        description = description,
        icon = icon,
        isLongClickAvailable = isLongClickAvailable,
    ) {
        data object Buy : V2(
            title = resourceReference(R.string.common_buy),
            description = resourceReference(R.string.quick_action_buy_description),
            icon = R.drawable.ic_credit_card_20,
        )

        data class Exchange(
            val shouldShowBadge: Boolean,
        ) : V2(
            title = resourceReference(R.string.common_exchange),
            description = resourceReference(R.string.quick_action_swap_description),
            icon = R.drawable.ic_exchange_mini_24,
        )

        data object Receive : V2(
            title = resourceReference(R.string.common_receive),
            description = resourceReference(R.string.quick_action_receive_description),
            icon = R.drawable.ic_qrcode_new_24,
            isLongClickAvailable = true,
        )

        data object Stake : V2(
            title = resourceReference(R.string.common_stake),
            description = resourceReference(R.string.stake_token_description),
            icon = R.drawable.ic_staking_24,
        )

        data class YieldMode(
            private val apy: String,
        ) : V2(
            title = resourceReference(R.string.common_yield_mode),
            description = resourceReference(R.string.yield_module_main_screen_promo_banner_message, wrappedList(apy)),
            icon = R.drawable.ic_analytics_up_mini_24,
        )
    }
}