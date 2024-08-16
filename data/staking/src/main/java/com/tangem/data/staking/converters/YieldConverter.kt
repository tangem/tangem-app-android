package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.AddressArgumentDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO.MetadataDTO.RewardScheduleDTO
import com.tangem.domain.staking.model.stakekit.AddressArgument
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.Yield.Metadata.RewardSchedule
import com.tangem.utils.converter.Converter

class YieldConverter(
    private val tokenConverter: TokenConverter,
) : Converter<YieldDTO, Yield> {

    override fun convert(value: YieldDTO): Yield {
        return Yield(
            id = value.id,
            token = tokenConverter.convert(value.token),
            tokens = value.tokens.map { tokenConverter.convert(it) },
            args = convertArgs(value.args),
            status = convertStatus(value.status),
            apy = value.apy,
            rewardRate = value.rewardRate,
            rewardType = convertRewardType(value.rewardType),
            metadata = convertMetadata(value.metadata),
            validators = value.validators
                .filter { it.preferred }
                .map { convertValidator(it) }
                .sortedByDescending { it.apr },
            isAvailable = value.isAvailable,
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
            args = enterDTO.args
                .mapKeys { convertArgType(it.key) }
                .mapValues { convertAddressArgument(it.value) },
        )
    }

    private fun convertAddresses(addressesDTO: YieldDTO.ArgsDTO.Enter.Addresses): Yield.Args.Enter.Addresses {
        return Yield.Args.Enter.Addresses(
            address = convertAddressArgument(addressesDTO.address),
            additionalAddresses = addressesDTO.additionalAddresses
                ?.mapKeys { convertArgType(it.key) }
                ?.mapValues { convertAddressArgument(it.value) },
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
            gasFeeToken = tokenConverter.convert(metadataDTO.gasFeeTokenDTO),
            token = tokenConverter.convert(metadataDTO.tokenDTO),
            tokens = metadataDTO.tokensDTO.map { tokenConverter.convert(it) },
            type = metadataDTO.type,
            rewardSchedule = convertRewardSchedule(metadataDTO.rewardSchedule),
            cooldownPeriod = convertPeriod(metadataDTO.cooldownPeriod),
            warmupPeriod = convertPeriod(metadataDTO.warmupPeriod),
            rewardClaiming = convertRewardClaiming(metadataDTO.rewardClaiming),
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

    private fun convertRewardSchedule(rewardTypeDTO: RewardScheduleDTO): RewardSchedule {
        return when (rewardTypeDTO) {
            RewardScheduleDTO.BLOCK -> RewardSchedule.BLOCK
            RewardScheduleDTO.WEEK -> RewardSchedule.WEEK
            RewardScheduleDTO.HOUR -> RewardSchedule.HOUR
            RewardScheduleDTO.DAY -> RewardSchedule.DAY
            RewardScheduleDTO.MONTH -> RewardSchedule.MONTH
            RewardScheduleDTO.ERA -> RewardSchedule.ERA
            RewardScheduleDTO.EPOCH -> RewardSchedule.EPOCH
            else -> RewardSchedule.UNKNOWN
        }
    }

    private fun convertRewardClaiming(
        rewardClaimingDTO: YieldDTO.MetadataDTO.RewardClaimingDTO,
    ): Yield.Metadata.RewardClaiming {
        return when (rewardClaimingDTO) {
            YieldDTO.MetadataDTO.RewardClaimingDTO.AUTO -> Yield.Metadata.RewardClaiming.AUTO
            YieldDTO.MetadataDTO.RewardClaimingDTO.MANUAL -> Yield.Metadata.RewardClaiming.MANUAL
            else -> Yield.Metadata.RewardClaiming.UNKNOWN
        }
    }

    private fun convertArgType(value: String): Yield.Args.ArgType {
        return when (value) {
            "address" -> Yield.Args.ArgType.ADDRESS
            "amount" -> Yield.Args.ArgType.AMOUNT
            else -> Yield.Args.ArgType.UNKNOWN
        }
    }
}
