package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolVaultDTO
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Converter from P2PEthPool Vault DTO to Domain model
 */
internal object P2PEthPoolVaultConverter : Converter<P2PEthPoolVaultDTO, P2PEthPoolVault> {

    private val HUNDRED = BigDecimal(100)
    private const val DIVIDE_SCALE = 8

    override fun convert(value: P2PEthPoolVaultDTO): P2PEthPoolVault {
        return P2PEthPoolVault(
            vaultAddress = value.vaultAddress,
            displayName = value.displayName,
            apy = value.apy.divide(HUNDRED, DIVIDE_SCALE, RoundingMode.HALF_UP),
            baseApy = value.baseApy.divide(HUNDRED, DIVIDE_SCALE, RoundingMode.HALF_UP),
            capacity = value.capacity,
            totalAssets = value.totalAssets,
            feePercent = value.feePercent,
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