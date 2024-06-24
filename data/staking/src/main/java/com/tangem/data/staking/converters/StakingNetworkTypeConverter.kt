package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.domain.staking.model.NetworkType
import com.tangem.utils.converter.Converter

@Suppress("CyclomaticComplexMethod", "LongMethod")
class StakingNetworkTypeConverter : Converter<NetworkTypeDTO, NetworkType> {

    override fun convert(value: NetworkTypeDTO): NetworkType {
        return when (value) {
            NetworkTypeDTO.AVALANCHE_C -> NetworkType.AVALANCHE_C
            NetworkTypeDTO.AVALANCHE_ATOMIC -> NetworkType.AVALANCHE_ATOMIC
            NetworkTypeDTO.AVALANCHE_P -> NetworkType.AVALANCHE_P
            NetworkTypeDTO.ARBITRUM -> NetworkType.ARBITRUM
            NetworkTypeDTO.BINANCE -> NetworkType.BINANCE
            NetworkTypeDTO.CELO -> NetworkType.CELO
            NetworkTypeDTO.ETHEREUM -> NetworkType.ETHEREUM
            NetworkTypeDTO.ETHEREUM_GOERLI -> NetworkType.ETHEREUM_GOERLI
            NetworkTypeDTO.ETHEREUM_HOLESKY -> NetworkType.ETHEREUM_HOLESKY
            NetworkTypeDTO.FANTOM -> NetworkType.FANTOM
            NetworkTypeDTO.HARMONY -> NetworkType.HARMONY
            NetworkTypeDTO.OPTIMISM -> NetworkType.OPTIMISM
            NetworkTypeDTO.POLYGON -> NetworkType.POLYGON
            NetworkTypeDTO.GNOSIS -> NetworkType.GNOSIS
            NetworkTypeDTO.MOONRIVER -> NetworkType.MOONRIVER
            NetworkTypeDTO.OKC -> NetworkType.OKC
            NetworkTypeDTO.ZKSYNC -> NetworkType.ZKSYNC
            NetworkTypeDTO.VICTION -> NetworkType.VICTION
            NetworkTypeDTO.AGORIC -> NetworkType.AGORIC
            NetworkTypeDTO.AKASH -> NetworkType.AKASH
            NetworkTypeDTO.AXELAR -> NetworkType.AXELAR
            NetworkTypeDTO.BAND_PROTOCOL -> NetworkType.BAND_PROTOCOL
            NetworkTypeDTO.BITSONG -> NetworkType.BITSONG
            NetworkTypeDTO.CANTO -> NetworkType.CANTO
            NetworkTypeDTO.CHIHUAHUA -> NetworkType.CHIHUAHUA
            NetworkTypeDTO.COMDEX -> NetworkType.COMDEX
            NetworkTypeDTO.COREUM -> NetworkType.COREUM
            NetworkTypeDTO.COSMOS -> NetworkType.COSMOS
            NetworkTypeDTO.CRESCENT -> NetworkType.CRESCENT
            NetworkTypeDTO.CRONOS -> NetworkType.CRONOS
            NetworkTypeDTO.CUDOS -> NetworkType.CUDOS
            NetworkTypeDTO.DESMOS -> NetworkType.DESMOS
            NetworkTypeDTO.DYDX -> NetworkType.DYDX
            NetworkTypeDTO.EVMOS -> NetworkType.EVMOS
            NetworkTypeDTO.FETCH_AI -> NetworkType.FETCH_AI
            NetworkTypeDTO.GRAVITY_BRIDGE -> NetworkType.GRAVITY_BRIDGE
            NetworkTypeDTO.INJECTIVE -> NetworkType.INJECTIVE
            NetworkTypeDTO.IRISNET -> NetworkType.IRISNET
            NetworkTypeDTO.JUNO -> NetworkType.JUNO
            NetworkTypeDTO.KAVA -> NetworkType.KAVA
            NetworkTypeDTO.KI_NETWORK -> NetworkType.KI_NETWORK
            NetworkTypeDTO.MARS_PROTOCOL -> NetworkType.MARS_PROTOCOL
            NetworkTypeDTO.NYM -> NetworkType.NYM
            NetworkTypeDTO.OKEX_CHAIN -> NetworkType.OKEX_CHAIN
            NetworkTypeDTO.ONOMY -> NetworkType.ONOMY
            NetworkTypeDTO.OSMOSIS -> NetworkType.OSMOSIS
            NetworkTypeDTO.PERSISTENCE -> NetworkType.PERSISTENCE
            NetworkTypeDTO.QUICKSILVER -> NetworkType.QUICKSILVER
            NetworkTypeDTO.REGEN -> NetworkType.REGEN
            NetworkTypeDTO.SECRET -> NetworkType.SECRET
            NetworkTypeDTO.SENTINEL -> NetworkType.SENTINEL
            NetworkTypeDTO.SOMMELIER -> NetworkType.SOMMELIER
            NetworkTypeDTO.STAFI -> NetworkType.STAFI
            NetworkTypeDTO.STARGAZE -> NetworkType.STARGAZE
            NetworkTypeDTO.STRIDE -> NetworkType.STRIDE
            NetworkTypeDTO.TERITORI -> NetworkType.TERITORI
            NetworkTypeDTO.TGRADE -> NetworkType.TGRADE
            NetworkTypeDTO.UMEE -> NetworkType.UMEE
            NetworkTypeDTO.POLKADOT -> NetworkType.POLKADOT
            NetworkTypeDTO.KUSAMA -> NetworkType.KUSAMA
            NetworkTypeDTO.WESTEND -> NetworkType.WESTEND
            NetworkTypeDTO.BINANCEBEACON -> NetworkType.BINANCEBEACON
            NetworkTypeDTO.NEAR -> NetworkType.NEAR
            NetworkTypeDTO.SOLANA -> NetworkType.SOLANA
            NetworkTypeDTO.TEZOS -> NetworkType.TEZOS
            NetworkTypeDTO.TRON -> NetworkType.TRON
            else -> NetworkType.UNKNOWN
        }
    }
}