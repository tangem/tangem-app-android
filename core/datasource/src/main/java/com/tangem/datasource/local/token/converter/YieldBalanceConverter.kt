package com.tangem.datasource.local.token.converter

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.utils.converter.Converter
import kotlinx.datetime.Instant

class YieldBalanceConverter(
    private val source: StatusSource,
) : Converter<YieldBalanceWrapperDTO, YieldBalance?> {

    constructor(isCached: Boolean) : this(source = if (isCached) StatusSource.CACHE else StatusSource.ACTUAL)

    override fun convert(value: YieldBalanceWrapperDTO): YieldBalance? {
        val stakingId = StakingID(
            integrationId = value.integrationId ?: return null,
            address = value.addresses.address,
        )

        return if (value.balances.isEmpty()) {
            YieldBalance.Empty(stakingId = stakingId, source = source)
        } else {
            YieldBalance.Data(
                stakingId = stakingId,
                balance = YieldBalanceItem(
                    items = value.balances
                        .map { item -> item.toBalanceItem() }
                        .sortedWith(comparator = compareBy({ it.type }, { it.amount })),
                    integrationId = value.integrationId,
                ),
                source = source,
            )
        }
    }

    private fun BalanceDTO.toBalanceItem(): BalanceItem {
        val item = this

        return BalanceItem(
            groupId = item.groupId,
            token = YieldTokenConverter.convert(item.tokenDTO),
            type = BalanceTypeConverter.convert(item.type),
            amount = item.amount,
            rawCurrencyId = item.tokenDTO.coinGeckoId,
            // tron-specific. operates validatorAddresses instead of validatorAddress
            validatorAddress = item.validatorAddress ?: item.validatorAddresses?.get(0),
            date = item.date?.toString()?.let { Instant.parse(it) },
            pendingActions = PendingActionConverter
                .convertList(item.pendingActions)
                .sortedBy { it.passthrough },
            pendingActionsConstraints = PendingActionConstraintsConverter
                .convertList(item.pendingActionConstraints.orEmpty()),
            isPending = false,
        )
    }
}