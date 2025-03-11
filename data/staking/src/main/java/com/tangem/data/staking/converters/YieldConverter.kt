package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.AddressArgumentDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO.MetadataDTO.RewardScheduleDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO.ValidatorDTO.ValidatorStatusDTO
import com.tangem.datasource.local.token.converter.TokenConverter
import com.tangem.domain.staking.model.stakekit.AddressArgument
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.Yield.Metadata.RewardSchedule
import com.tangem.domain.staking.model.stakekit.Yield.Validator.ValidatorStatus
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

internal object YieldConverter : Converter<YieldDTO, Yield> {

    private val PARTNERS = listOf(
        "cosmosvaloper1wrx0x9m9ykdhw9sg04v7uljme53wuj03aa5d4f",
        "H2tJNyMHnRF6ahCQLQ1sSycM4FGchymuzyYzUqKEuydk",
    )

    private val PARTNERS_NAMES = listOf("Meria")

    override fun convert(value: YieldDTO): Yield {
        return Yield(
            id = value.id.asMandatory("id"),
            token = TokenConverter.convert(value.token.asMandatory("token")),
            tokens = value.tokens.asMandatory("tokens").map(TokenConverter::convert),
            args = convertArgs(value.args.asMandatory("args")),
            status = convertStatus(value.status.asMandatory("status")),
            apy = value.apy.asMandatory("apy"),
            rewardRate = value.rewardRate.asMandatory("rewardRate"),
            rewardType = convertRewardType(value.rewardType.asMandatory("rewardType")),
            metadata = convertMetadata(value.metadata.asMandatory("metadata")),
            validators = value.validators.asMandatory("validators")
                .asSequence()
                .distinctBy { it.address }
                .filter { it.status == ValidatorStatusDTO.ACTIVE }
                .map { convertValidator(it) }
                .sortedByDescending { it.apr }
                .sortedByDescending { it.isStrategicPartner }
                .toImmutableList(),
            isAvailable = value.isAvailable.asMandatory("isAvailable"),
        )
    }

    private fun convertArgs(argsDTO: YieldDTO.ArgsDTO): Yield.Args {
        return Yield.Args(
            enter = convertEnter(argsDTO.enter.asMandatory("enter")),
            exit = argsDTO.exit?.let { convertEnter(it) },
        )
    }

    private fun convertEnter(enterDTO: YieldDTO.ArgsDTO.Enter): Yield.Args.Enter {
        return Yield.Args.Enter(
            addresses = convertAddresses(enterDTO.addresses.asMandatory("addresses")),
            args = enterDTO.args.asMandatory("args")
                .mapKeys { convertArgType(it.key) }
                .mapValues { convertAddressArgument(it.value) },
        )
    }

    private fun convertAddresses(addressesDTO: YieldDTO.ArgsDTO.Enter.Addresses): Yield.Args.Enter.Addresses {
        return Yield.Args.Enter.Addresses(
            address = convertAddressArgument(addressesDTO.address.asMandatory("address")),
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
            enter = statusDTO.enter.asMandatory("enter"),
            exit = statusDTO.exit,
        )
    }

    private fun convertMetadata(metadataDTO: YieldDTO.MetadataDTO): Yield.Metadata {
        return Yield.Metadata(
            name = metadataDTO.name.asMandatory("name"),
            logoUri = metadataDTO.logoUri.asMandatory("logoUri"),
            description = metadataDTO.description.asMandatory("description"),
            documentation = metadataDTO.documentation,
            gasFeeToken = TokenConverter.convert(metadataDTO.gasFeeTokenDTO.asMandatory("gasFeeTokenDTO")),
            token = TokenConverter.convert(metadataDTO.tokenDTO.asMandatory("tokenDTO")),
            tokens = metadataDTO.tokensDTO.asMandatory("tokensDTO").map(TokenConverter::convert),
            type = metadataDTO.type.asMandatory("type"),
            rewardSchedule = convertRewardSchedule(metadataDTO.rewardSchedule.asMandatory("rewardSchedule")),
            cooldownPeriod = metadataDTO.cooldownPeriod?.let { convertPeriod(it) },
            warmupPeriod = convertPeriod(metadataDTO.warmupPeriod.asMandatory("warmupPeriod")),
            rewardClaiming = convertRewardClaiming(metadataDTO.rewardClaiming.asMandatory("rewardClaiming")),
            defaultValidator = metadataDTO.defaultValidator,
            minimumStake = metadataDTO.minimumStake,
            supportsMultipleValidators = metadataDTO.supportsMultipleValidators,
            revshare = convertEnabled(metadataDTO.revshare.asMandatory("revshare")),
            fee = convertEnabled(metadataDTO.fee.asMandatory("fee")),
        )
    }

    private fun convertPeriod(periodDTO: YieldDTO.MetadataDTO.PeriodDTO): Yield.Metadata.Period {
        return Yield.Metadata.Period(
            days = periodDTO.days.asMandatory("days"),
        )
    }

    private fun convertEnabled(enabledDTO: YieldDTO.MetadataDTO.EnabledDTO): Yield.Metadata.Enabled {
        return Yield.Metadata.Enabled(
            enabled = enabledDTO.enabled.asMandatory("enabled"),
        )
    }

    private fun convertValidator(validatorDTO: YieldDTO.ValidatorDTO): Yield.Validator {
        val address = validatorDTO.address.asMandatory("address")
        return Yield.Validator(
            address = address,
            status = convertValidatorStatus(validatorDTO.status.asMandatory("status")),
            name = validatorDTO.name.asMandatory("name"),
            image = validatorDTO.image,
            website = validatorDTO.website,
            apr = validatorDTO.apr,
            commission = validatorDTO.commission,
            stakedBalance = validatorDTO.stakedBalance,
            votingPower = validatorDTO.votingPower,
            preferred = validatorDTO.preferred.asMandatory("preferred"),
            isStrategicPartner = isStrategicPartner(validatorDTO.address, validatorDTO.name.asMandatory("name")),
        )
    }

    private fun convertRewardType(rewardTypeDTO: YieldDTO.RewardTypeDTO): Yield.RewardType {
        return when (rewardTypeDTO) {
            YieldDTO.RewardTypeDTO.APY -> Yield.RewardType.APY
            YieldDTO.RewardTypeDTO.APR -> Yield.RewardType.APR
            else -> Yield.RewardType.UNKNOWN
        }
    }

    private fun convertValidatorStatus(validatorStatusDTO: ValidatorStatusDTO): ValidatorStatus {
        return when (validatorStatusDTO) {
            ValidatorStatusDTO.ACTIVE -> ValidatorStatus.ACTIVE
            ValidatorStatusDTO.DEACTIVATING -> ValidatorStatus.DEACTIVATING
            ValidatorStatusDTO.INACTIVE -> ValidatorStatus.INACTIVE
            ValidatorStatusDTO.JAILED -> ValidatorStatus.JAILED
            ValidatorStatusDTO.FULL -> ValidatorStatus.FULL
            else -> ValidatorStatus.UNKNOWN
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

    private fun isStrategicPartner(validatorAddress: String?, validatorName: String): Boolean {
        return PARTNERS.any { it == validatorAddress } || PARTNERS_NAMES.any { it.equals(validatorName, true) }
    }
}