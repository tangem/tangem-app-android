package com.tangem.data.staking.store

import com.tangem.common.test.data.staking.MockP2PEthPoolAccountResponseFactory
import com.tangem.data.staking.converters.ethpool.P2PEthPoolStakingAccountConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.test.mock.staking.MockStaking
import java.math.BigDecimal

internal object P2PEthPoolBalancesStoreFixtures {

    val userWalletId = UserWalletId(stringValue = "011")
    val otherWalletId = UserWalletId(stringValue = "022")

    const val ADDR_A = MockStaking.DELEGATOR_ADDRESS
    const val ADDR_B = "0xdelegator2"
    const val VAULT_1 = MockP2PEthPoolAccountResponseFactory.defaultVaultAddress
    const val VAULT_2 = "0xvault2"

    val idA = StakingID(integrationId = MockStaking.P2P_ID, address = ADDR_A)
    val idB = StakingID(integrationId = MockStaking.P2P_ID, address = ADDR_B)

    fun response(id: StakingID = idA, vault: String = VAULT_1, staked: BigDecimal = BigDecimal("1.5")) =
        MockP2PEthPoolAccountResponseFactory.createWithBalance(
            stakingId = id,
            vaultAddress = vault,
            stakedAmount = staked,
        )

    fun p2pData(id: StakingID, source: StatusSource, vararg responses: P2PEthPoolAccountResponse) =
        StakingBalance.Data.P2PEthPool(
            stakingId = id,
            source = source,
            accounts = responses.map(P2PEthPoolStakingAccountConverter::convert),
        )
}