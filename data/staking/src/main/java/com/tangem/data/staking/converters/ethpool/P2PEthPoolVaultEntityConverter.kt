package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolVaultDTO
import com.tangem.datasource.local.txhistory.db.entity.staking.P2PEthPoolVaultEntity

/**
 * Converts a raw [P2PEthPoolVaultDTO] into its persisted [P2PEthPoolVaultEntity] representation, unchanged
 * (unlike [P2PEthPoolVaultConverter], which rescales apy/baseApy for domain use).
 */
internal object P2PEthPoolVaultEntityConverter {

    fun convert(value: P2PEthPoolVaultDTO): P2PEthPoolVaultEntity {
        return P2PEthPoolVaultEntity(
            vaultAddress = value.vaultAddress,
            displayName = value.displayName,
            apy = value.apy.toPlainString(),
            baseApy = value.baseApy.toPlainString(),
            capacity = value.capacity.toPlainString(),
            totalAssets = value.totalAssets.toPlainString(),
            feePercent = value.feePercent.toPlainString(),
            isPrivate = value.isPrivate,
            isGenesis = value.isGenesis,
            isSmoothingPool = value.isSmoothingPool,
            isErc20 = value.isErc20,
            tokenName = value.tokenName,
            tokenSymbol = value.tokenSymbol,
            createdAt = value.createdAt,
        )
    }
}