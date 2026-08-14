package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolVaultDTO
import com.tangem.datasource.local.txhistory.db.entity.staking.P2PEthPoolVaultEntity
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

    /**
     * Maps a persisted [P2PEthPoolVaultEntity] back into a domain [P2PEthPoolVault], for vaults resolved from the
     * database instead of a live vaults-list response (e.g. historical vaults no longer returned by P2P).
     */
    fun convertFromEntity(entity: P2PEthPoolVaultEntity): P2PEthPoolVault {
        return P2PEthPoolVault(
            vaultAddress = entity.vaultAddress,
            displayName = entity.displayName,
            apy = entity.apy.toBigDecimal().divide(HUNDRED, DIVIDE_SCALE, RoundingMode.HALF_UP),
            baseApy = entity.baseApy.toBigDecimal().divide(HUNDRED, DIVIDE_SCALE, RoundingMode.HALF_UP),
            capacity = entity.capacity.toBigDecimal(),
            totalAssets = entity.totalAssets.toBigDecimal(),
            feePercent = entity.feePercent.toBigDecimal(),
            isPrivate = entity.isPrivate,
            isGenesis = entity.isGenesis,
            isSmoothingPool = entity.isSmoothingPool,
            isErc20 = entity.isErc20,
            tokenName = entity.tokenName,
            tokenSymbol = entity.tokenSymbol,
            createdAt = entity.createdAt,
        )
    }
}