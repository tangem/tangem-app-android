package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.domain.staking.model.stakekit.NetworkType
import com.tangem.utils.converter.TwoWayConverter

@Suppress("CyclomaticComplexMethod", "LongMethod")
class StakingNetworkTypeConverter : TwoWayConverter<NetworkTypeDTO, NetworkType> {

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
            NetworkTypeDTO.TON -> NetworkType.TON
            else -> NetworkType.UNKNOWN
        }
    }

    override fun convertBack(value: NetworkType): NetworkTypeDTO {
        return when (value) {
            NetworkType.AVALANCHE_C -> NetworkTypeDTO.AVALANCHE_C
            NetworkType.AVALANCHE_ATOMIC -> NetworkTypeDTO.AVALANCHE_ATOMIC
            NetworkType.AVALANCHE_P -> NetworkTypeDTO.AVALANCHE_P
            NetworkType.ARBITRUM -> NetworkTypeDTO.ARBITRUM
            NetworkType.BINANCE -> NetworkTypeDTO.BINANCE
            NetworkType.CELO -> NetworkTypeDTO.CELO
            NetworkType.ETHEREUM -> NetworkTypeDTO.ETHEREUM
            NetworkType.ETHEREUM_GOERLI -> NetworkTypeDTO.ETHEREUM_GOERLI
            NetworkType.ETHEREUM_HOLESKY -> NetworkTypeDTO.ETHEREUM_HOLESKY
            NetworkType.FANTOM -> NetworkTypeDTO.FANTOM
            NetworkType.HARMONY -> NetworkTypeDTO.HARMONY
            NetworkType.OPTIMISM -> NetworkTypeDTO.OPTIMISM
            NetworkType.POLYGON -> NetworkTypeDTO.POLYGON
            NetworkType.GNOSIS -> NetworkTypeDTO.GNOSIS
            NetworkType.MOONRIVER -> NetworkTypeDTO.MOONRIVER
            NetworkType.OKC -> NetworkTypeDTO.OKC
            NetworkType.ZKSYNC -> NetworkTypeDTO.ZKSYNC
            NetworkType.VICTION -> NetworkTypeDTO.VICTION
            NetworkType.AGORIC -> NetworkTypeDTO.AGORIC
            NetworkType.AKASH -> NetworkTypeDTO.AKASH
            NetworkType.AXELAR -> NetworkTypeDTO.AXELAR
            NetworkType.BAND_PROTOCOL -> NetworkTypeDTO.BAND_PROTOCOL
            NetworkType.BITSONG -> NetworkTypeDTO.BITSONG
            NetworkType.CANTO -> NetworkTypeDTO.CANTO
            NetworkType.CHIHUAHUA -> NetworkTypeDTO.CHIHUAHUA
            NetworkType.COMDEX -> NetworkTypeDTO.COMDEX
            NetworkType.COREUM -> NetworkTypeDTO.COREUM
            NetworkType.COSMOS -> NetworkTypeDTO.COSMOS
            NetworkType.CRESCENT -> NetworkTypeDTO.CRESCENT
            NetworkType.CRONOS -> NetworkTypeDTO.CRONOS
            NetworkType.CUDOS -> NetworkTypeDTO.CUDOS
            NetworkType.DESMOS -> NetworkTypeDTO.DESMOS
            NetworkType.DYDX -> NetworkTypeDTO.DYDX
            NetworkType.EVMOS -> NetworkTypeDTO.EVMOS
            NetworkType.FETCH_AI -> NetworkTypeDTO.FETCH_AI
            NetworkType.GRAVITY_BRIDGE -> NetworkTypeDTO.GRAVITY_BRIDGE
            NetworkType.INJECTIVE -> NetworkTypeDTO.INJECTIVE
            NetworkType.IRISNET -> NetworkTypeDTO.IRISNET
            NetworkType.JUNO -> NetworkTypeDTO.JUNO
            NetworkType.KAVA -> NetworkTypeDTO.KAVA
            NetworkType.KI_NETWORK -> NetworkTypeDTO.KI_NETWORK
            NetworkType.MARS_PROTOCOL -> NetworkTypeDTO.MARS_PROTOCOL
            NetworkType.NYM -> NetworkTypeDTO.NYM
            NetworkType.OKEX_CHAIN -> NetworkTypeDTO.OKEX_CHAIN
            NetworkType.ONOMY -> NetworkTypeDTO.ONOMY
            NetworkType.OSMOSIS -> NetworkTypeDTO.OSMOSIS
            NetworkType.PERSISTENCE -> NetworkTypeDTO.PERSISTENCE
            NetworkType.QUICKSILVER -> NetworkTypeDTO.QUICKSILVER
            NetworkType.REGEN -> NetworkTypeDTO.REGEN
            NetworkType.SECRET -> NetworkTypeDTO.SECRET
            NetworkType.SENTINEL -> NetworkTypeDTO.SENTINEL
            NetworkType.SOMMELIER -> NetworkTypeDTO.SOMMELIER
            NetworkType.STAFI -> NetworkTypeDTO.STAFI
            NetworkType.STARGAZE -> NetworkTypeDTO.STARGAZE
            NetworkType.STRIDE -> NetworkTypeDTO.STRIDE
            NetworkType.TERITORI -> NetworkTypeDTO.TERITORI
            NetworkType.TGRADE -> NetworkTypeDTO.TGRADE
            NetworkType.UMEE -> NetworkTypeDTO.UMEE
            NetworkType.POLKADOT -> NetworkTypeDTO.POLKADOT
            NetworkType.KUSAMA -> NetworkTypeDTO.KUSAMA
            NetworkType.WESTEND -> NetworkTypeDTO.WESTEND
            NetworkType.BINANCEBEACON -> NetworkTypeDTO.BINANCEBEACON
            NetworkType.NEAR -> NetworkTypeDTO.NEAR
            NetworkType.SOLANA -> NetworkTypeDTO.SOLANA
            NetworkType.TEZOS -> NetworkTypeDTO.TEZOS
            NetworkType.TRON -> NetworkTypeDTO.TRON
            NetworkType.TON -> NetworkTypeDTO.TON
            else -> NetworkTypeDTO.UNKNOWN
        }
    }
}