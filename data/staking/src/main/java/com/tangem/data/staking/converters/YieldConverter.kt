package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.AddressArgumentDTO
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
    private fun convertNetworkType(networkTypeDTO: TokenDTO.NetworkTypeDTO): Token.NetworkType {
        return when (networkTypeDTO) {
            TokenDTO.NetworkTypeDTO.AVALANCHE_C -> Token.NetworkType.AVALANCHE_C
            TokenDTO.NetworkTypeDTO.AVALANCHE_ATOMIC -> Token.NetworkType.AVALANCHE_ATOMIC
            TokenDTO.NetworkTypeDTO.AVALANCHE_P -> Token.NetworkType.AVALANCHE_P
            TokenDTO.NetworkTypeDTO.ARBITRUM -> Token.NetworkType.ARBITRUM
            TokenDTO.NetworkTypeDTO.BINANCE -> Token.NetworkType.BINANCE
            TokenDTO.NetworkTypeDTO.CELO -> Token.NetworkType.CELO
            TokenDTO.NetworkTypeDTO.ETHEREUM -> Token.NetworkType.ETHEREUM
            TokenDTO.NetworkTypeDTO.ETHEREUM_GOERLI -> Token.NetworkType.ETHEREUM_GOERLI
            TokenDTO.NetworkTypeDTO.ETHEREUM_HOLESKY -> Token.NetworkType.ETHEREUM_HOLESKY
            TokenDTO.NetworkTypeDTO.FANTOM -> Token.NetworkType.FANTOM
            TokenDTO.NetworkTypeDTO.HARMONY -> Token.NetworkType.HARMONY
            TokenDTO.NetworkTypeDTO.OPTIMISM -> Token.NetworkType.OPTIMISM
            TokenDTO.NetworkTypeDTO.POLYGON -> Token.NetworkType.POLYGON
            TokenDTO.NetworkTypeDTO.GNOSIS -> Token.NetworkType.GNOSIS
            TokenDTO.NetworkTypeDTO.MOONRIVER -> Token.NetworkType.MOONRIVER
            TokenDTO.NetworkTypeDTO.OKC -> Token.NetworkType.OKC
            TokenDTO.NetworkTypeDTO.ZKSYNC -> Token.NetworkType.ZKSYNC
            TokenDTO.NetworkTypeDTO.VICTION -> Token.NetworkType.VICTION
            TokenDTO.NetworkTypeDTO.AGORIC -> Token.NetworkType.AGORIC
            TokenDTO.NetworkTypeDTO.AKASH -> Token.NetworkType.AKASH
            TokenDTO.NetworkTypeDTO.AXELAR -> Token.NetworkType.AXELAR
            TokenDTO.NetworkTypeDTO.BAND_PROTOCOL -> Token.NetworkType.BAND_PROTOCOL
            TokenDTO.NetworkTypeDTO.BITSONG -> Token.NetworkType.BITSONG
            TokenDTO.NetworkTypeDTO.CANTO -> Token.NetworkType.CANTO
            TokenDTO.NetworkTypeDTO.CHIHUAHUA -> Token.NetworkType.CHIHUAHUA
            TokenDTO.NetworkTypeDTO.COMDEX -> Token.NetworkType.COMDEX
            TokenDTO.NetworkTypeDTO.COREUM -> Token.NetworkType.COREUM
            TokenDTO.NetworkTypeDTO.COSMOS -> Token.NetworkType.COSMOS
            TokenDTO.NetworkTypeDTO.CRESCENT -> Token.NetworkType.CRESCENT
            TokenDTO.NetworkTypeDTO.CRONOS -> Token.NetworkType.CRONOS
            TokenDTO.NetworkTypeDTO.CUDOS -> Token.NetworkType.CUDOS
            TokenDTO.NetworkTypeDTO.DESMOS -> Token.NetworkType.DESMOS
            TokenDTO.NetworkTypeDTO.DYDX -> Token.NetworkType.DYDX
            TokenDTO.NetworkTypeDTO.EVMOS -> Token.NetworkType.EVMOS
            TokenDTO.NetworkTypeDTO.FETCH_AI -> Token.NetworkType.FETCH_AI
            TokenDTO.NetworkTypeDTO.GRAVITY_BRIDGE -> Token.NetworkType.GRAVITY_BRIDGE
            TokenDTO.NetworkTypeDTO.INJECTIVE -> Token.NetworkType.INJECTIVE
            TokenDTO.NetworkTypeDTO.IRISNET -> Token.NetworkType.IRISNET
            TokenDTO.NetworkTypeDTO.JUNO -> Token.NetworkType.JUNO
            TokenDTO.NetworkTypeDTO.KAVA -> Token.NetworkType.KAVA
            TokenDTO.NetworkTypeDTO.KI_NETWORK -> Token.NetworkType.KI_NETWORK
            TokenDTO.NetworkTypeDTO.MARS_PROTOCOL -> Token.NetworkType.MARS_PROTOCOL
            TokenDTO.NetworkTypeDTO.NYM -> Token.NetworkType.NYM
            TokenDTO.NetworkTypeDTO.OKEX_CHAIN -> Token.NetworkType.OKEX_CHAIN
            TokenDTO.NetworkTypeDTO.ONOMY -> Token.NetworkType.ONOMY
            TokenDTO.NetworkTypeDTO.OSMOSIS -> Token.NetworkType.OSMOSIS
            TokenDTO.NetworkTypeDTO.PERSISTENCE -> Token.NetworkType.PERSISTENCE
            TokenDTO.NetworkTypeDTO.QUICKSILVER -> Token.NetworkType.QUICKSILVER
            TokenDTO.NetworkTypeDTO.REGEN -> Token.NetworkType.REGEN
            TokenDTO.NetworkTypeDTO.SECRET -> Token.NetworkType.SECRET
            TokenDTO.NetworkTypeDTO.SENTINEL -> Token.NetworkType.SENTINEL
            TokenDTO.NetworkTypeDTO.SOMMELIER -> Token.NetworkType.SOMMELIER
            TokenDTO.NetworkTypeDTO.STAFI -> Token.NetworkType.STAFI
            TokenDTO.NetworkTypeDTO.STARGAZE -> Token.NetworkType.STARGAZE
            TokenDTO.NetworkTypeDTO.STRIDE -> Token.NetworkType.STRIDE
            TokenDTO.NetworkTypeDTO.TERITORI -> Token.NetworkType.TERITORI
            TokenDTO.NetworkTypeDTO.TGRADE -> Token.NetworkType.TGRADE
            TokenDTO.NetworkTypeDTO.UMEE -> Token.NetworkType.UMEE
            TokenDTO.NetworkTypeDTO.POLKADOT -> Token.NetworkType.POLKADOT
            TokenDTO.NetworkTypeDTO.KUSAMA -> Token.NetworkType.KUSAMA
            TokenDTO.NetworkTypeDTO.WESTEND -> Token.NetworkType.WESTEND
            TokenDTO.NetworkTypeDTO.BINANCEBEACON -> Token.NetworkType.BINANCEBEACON
            TokenDTO.NetworkTypeDTO.NEAR -> Token.NetworkType.NEAR
            TokenDTO.NetworkTypeDTO.SOLANA -> Token.NetworkType.SOLANA
            TokenDTO.NetworkTypeDTO.TEZOS -> Token.NetworkType.TEZOS
            TokenDTO.NetworkTypeDTO.TRON -> Token.NetworkType.TRON
            else -> Token.NetworkType.UNKNOWN
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