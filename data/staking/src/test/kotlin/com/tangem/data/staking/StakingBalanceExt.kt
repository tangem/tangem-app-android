package com.tangem.data.staking

import com.tangem.common.test.data.staking.MockP2PEthPoolAccountResponseFactory
import com.tangem.data.staking.converters.ethpool.P2PEthPoolAccountConverter
import com.tangem.data.staking.converters.ethpool.P2PYieldBalanceConverter
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.token.converter.YieldBalanceConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault

internal fun YieldBalanceWrapperDTO.toDomain(source: StatusSource = StatusSource.CACHE): YieldBalance {
    return YieldBalanceConverter(source = source).convert(this)!!
}

internal fun P2PEthPoolAccountResponse.toDomain(
    vault: P2PEthPoolVault = MockP2PEthPoolAccountResponseFactory.createMockVault(vaultAddress = vaultAddress),
    source: StatusSource = StatusSource.CACHE,
): YieldBalance {
    val account = P2PEthPoolAccountConverter.convert(this)
    return P2PYieldBalanceConverter.convert(
        account = account,
        vault = vault,
        address = account.delegatorAddress,
        source = source,
    )
}