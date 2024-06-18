package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.AddressArgumentDTO
import com.tangem.datasource.api.stakekit.models.response.model.TokenDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.domain.staking.model.*
import com.tangem.utils.converter.Converter

class YieldConverter(
    private val stakingNetworkTypeConverter: StakingNetworkTypeConverter
) : Converter<YieldDTO, Yield> {

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
            network = stakingNetworkTypeConverter.convert(tokenDTO.network),
            symbol = tokenDTO.symbol,
            decimals = tokenDTO.decimals,
            address = tokenDTO.address,
            coinGeckoId = tokenDTO.coinGeckoId,
            logoURI = tokenDTO.logoURI,
            isPoints = tokenDTO.isPoints,
        )
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
