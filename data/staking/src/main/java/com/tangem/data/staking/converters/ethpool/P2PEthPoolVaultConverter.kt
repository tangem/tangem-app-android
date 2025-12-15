package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolVaultDTO
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.utils.converter.Converter

/**
 * Converter from P2P Vault DTO to Domain model
 */
internal object P2PEthPoolVaultConverter : Converter<P2PEthPoolVaultDTO, P2PEthPoolVault> {

    override fun convert(value: P2PEthPoolVaultDTO): P2PEthPoolVault {
        return P2PEthPoolVault(
            vaultAddress = value.vaultAddress,
            displayName = value.displayName,
            apy = value.apy.toBigDecimal(),
            baseApy = value.baseApy.toBigDecimal(),
            capacity = value.capacity.toBigDecimal(),
            totalAssets = value.totalAssets.toBigDecimal(),
            feePercent = value.feePercent.toBigDecimal(),
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