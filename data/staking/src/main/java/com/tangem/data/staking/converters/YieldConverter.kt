package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.AddressArgumentDTO
import com.tangem.datasource.api.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.TokenDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.domain.staking.model.*
import com.tangem.utils.converter.Converter

class YieldConverter : Converter<YieldDTO, Yield> {

    override fun convert(value: YieldDTO): Yield {
        return Yield(
            id = value.id,
            token = convertToken(value.token),
            tokens = value.tokens.map { convertToken(it) },
            args = convertArgs(value.args),
            status = convertStatus(value.status),
            apy = value.apy,
            rewardRate = value.rewardRate,
            rewardType = convertRewardType(value.rewardType),
            metadata = convertMetadata(value.metadata),
            validators = value.validators.map { convertValidator(it) },
            isAvailable = value.isAvailable,
        )
    }

    private fun convertToken(tokenDTO: TokenDTO): Token {
        return Token(
            name = tokenDTO.name,
            network = convertNetworkType(tokenDTO.network),
            symbol = tokenDTO.symbol,
            decimals = tokenDTO.decimals,
            address = tokenDTO.address,
            coinGeckoId = tokenDTO.coinGeckoId,
            logoURI = tokenDTO.logoURI,
            isPoints = tokenDTO.isPoints,
        )
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun convertNetworkType(networkTypeDTO: NetworkTypeDTO): NetworkType {
        return when (networkTypeDTO) {
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

    private fun convertArgs(argsDTO: YieldDTO.ArgsDTO): Yield.Args {
        return Yield.Args(
            enter = convertEnter(argsDTO.enter),
            exit = argsDTO.exit?.let { convertEnter(it) },
        )
    }

    private fun convertEnter(enterDTO: YieldDTO.ArgsDTO.Enter): Yield.Args.Enter {
        return Yield.Args.Enter(
            addresses = convertAddresses(enterDTO.addresses),
            args = enterDTO.args.mapValues { convertAddressArgument(it.value) },
        )
    }

    private fun convertAddresses(addressesDTO: YieldDTO.ArgsDTO.Enter.Addresses): Yield.Args.Enter.Addresses {
        return Yield.Args.Enter.Addresses(
            address = convertAddressArgument(addressesDTO.address),
            additionalAddresses = addressesDTO.additionalAddresses?.mapValues { convertAddressArgument(it.value) },
        )
    }

    private fun convertAddressArgument(addressArgumentDTO: AddressArgumentDTO): AddressArgument {
        return AddressArgument(
            required = addressArgumentDTO.required,
            network = addressArgumentDTO.network,
            minimum = addressArgumentDTO.minimum,
            maximum = addressArgumentDTO.maximum,
        )
    }

    private fun convertStatus(statusDTO: YieldDTO.StatusDTO): Yield.Status {
        return Yield.Status(
            enter = statusDTO.enter,
            exit = statusDTO.exit,
        )
    }

    private fun convertMetadata(metadataDTO: YieldDTO.MetadataDTO): Yield.Metadata {
        return Yield.Metadata(
            name = metadataDTO.name,
            logoUri = metadataDTO.logoUri,
            description = metadataDTO.description,
            documentation = metadataDTO.documentation,
            gasFeeToken = convertToken(metadataDTO.gasFeeTokenDTO),
            token = convertToken(metadataDTO.tokenDTO),
            tokens = metadataDTO.tokensDTO.map { convertToken(it) },
            type = metadataDTO.type,
            rewardSchedule = metadataDTO.rewardSchedule,
            cooldownPeriod = convertPeriod(metadataDTO.cooldownPeriod),
            warmupPeriod = convertPeriod(metadataDTO.warmupPeriod),
            rewardClaiming = metadataDTO.rewardClaiming,
            defaultValidator = metadataDTO.defaultValidator,
            minimumStake = metadataDTO.minimumStake,
            supportsMultipleValidators = metadataDTO.supportsMultipleValidators,
            revshare = convertEnabled(metadataDTO.revshare),
            fee = convertEnabled(metadataDTO.fee),
        )
    }

    private fun convertPeriod(periodDTO: YieldDTO.MetadataDTO.PeriodDTO): Yield.Metadata.Period {
        return Yield.Metadata.Period(
            days = periodDTO.days,
        )
    }

    private fun convertEnabled(enabledDTO: YieldDTO.MetadataDTO.EnabledDTO): Yield.Metadata.Enabled {
        return Yield.Metadata.Enabled(
            enabled = enabledDTO.enabled,
        )
    }

    private fun convertValidator(validatorDTO: YieldDTO.ValidatorDTO): Yield.Validator {
        return Yield.Validator(
            address = validatorDTO.address,
            status = validatorDTO.status,
            name = validatorDTO.name,
            image = validatorDTO.image,
            website = validatorDTO.website,
            apr = validatorDTO.apr,
            commission = validatorDTO.commission,
            stakedBalance = validatorDTO.stakedBalance,
            votingPower = validatorDTO.votingPower,
            preferred = validatorDTO.preferred,
        )
    }

    private fun convertRewardType(rewardTypeDTO: YieldDTO.RewardTypeDTO): Yield.RewardType {
        return when (rewardTypeDTO) {
            YieldDTO.RewardTypeDTO.APY -> Yield.RewardType.APY
            YieldDTO.RewardTypeDTO.APR -> Yield.RewardType.APR
            else -> Yield.RewardType.UNKNOWN
        }
    }
}
