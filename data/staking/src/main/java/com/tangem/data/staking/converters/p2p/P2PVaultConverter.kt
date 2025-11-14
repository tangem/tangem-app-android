package com.tangem.data.staking.converters.p2p

import com.tangem.datasource.api.p2p.models.response.P2PVaultDTO
import com.tangem.domain.staking.model.p2p.P2PVault
import com.tangem.utils.converter.Converter

/**
 * Converter from P2P Vault DTO to Domain model
 */
internal object P2PVaultConverter : Converter<P2PVaultDTO, P2PVault> {

    override fun convert(value: P2PVaultDTO): P2PVault {
        return P2PVault(
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