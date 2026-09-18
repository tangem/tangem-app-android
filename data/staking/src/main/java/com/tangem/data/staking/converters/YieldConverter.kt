package com.tangem.data.staking.converters

import com.tangem.grow.datasource.stakekit.models.response.model.AddressArgumentDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.MetadataDTO.RewardScheduleDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.ValidatorDTO.ValidatorStatusDTO
import com.tangem.datasource.local.token.converter.YieldTokenConverter
import com.tangem.datasource.local.txhistory.db.entity.staking.StakingValidatorEntity
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.stakekit.AddressArgument
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.Yield.Metadata.RewardSchedule
import com.tangem.domain.staking.model.stakekit.Yield.Validator.ValidatorStatus
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode

internal object YieldConverter : Converter<YieldDTO, Yield> {

    private val PARTNERS = listOf(
        "cosmosvaloper1wrx0x9m9ykdhw9sg04v7uljme53wuj03aa5d4f",
        "H2tJNyMHnRF6ahCQLQ1sSycM4FGchymuzyYzUqKEuydk",
    )

    private val PARTNERS_NAMES = listOf("Meria")

    private const val DIVIDE_SCALE = 8

    private const val NO_NAME = "No name"

    override fun convert(value: YieldDTO): Yield {
        val rewardType = convertRewardType(value.rewardType.asMandatory("rewardType"))
        return Yield(
            id = value.id.asMandatory("id"),
            token = YieldTokenConverter.convert(value.token.asMandatory("token")),
            tokens = value.tokens.asMandatory("tokens").map(YieldTokenConverter::convert),
            args = convertArgs(value.args.asMandatory("args")),
            status = convertStatus(value.status.asMandatory("status")),
            apy = value.apy.asMandatory("apy"),
            rewardRate = value.rewardRate.asMandatory("rewardRate"),
            rewardType = rewardType,
            metadata = convertMetadata(value.metadata.asMandatory("metadata")),
            validators = value.validators.asMandatory("validators")
                .asSequence()
                .distinctBy { it.address }
                .filter { it.status == ValidatorStatusDTO.ACTIVE }
                .map { convertValidator(validatorDTO = it, rewardType = rewardType) }
                .sortedByDescending { it.rewardInfo?.rate?.orZero() }
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
            gasFeeToken = YieldTokenConverter.convert(metadataDTO.gasFeeTokenDTO.asMandatory("gasFeeTokenDTO")),
            token = YieldTokenConverter.convert(metadataDTO.tokenDTO.asMandatory("tokenDTO")),
            tokens = metadataDTO.tokensDTO.asMandatory("tokensDTO").map(YieldTokenConverter::convert),
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
            seconds = periodDTO.seconds,
        )
    }

    private fun convertEnabled(enabledDTO: YieldDTO.MetadataDTO.EnabledDTO): Yield.Metadata.Enabled {
        return Yield.Metadata.Enabled(
            enabled = enabledDTO.enabled.asMandatory("enabled"),
        )
    }

    private fun convertValidator(validatorDTO: YieldDTO.ValidatorDTO, rewardType: RewardType): Yield.Validator {
        val address = validatorDTO.address.asMandatory("address")
        // StakeKit returns validators without metadata (hundreds of Cardano pools), the same fallback as on iOS
        val name = validatorDTO.name ?: NO_NAME

        return Yield.Validator(
            address = address,
            status = convertValidatorStatus(validatorDTO.status ?: ValidatorStatusDTO.UNKNOWN),
            name = name,
            image = validatorDTO.image,
            website = validatorDTO.website,
            rewardInfo = createRewardInfo(apr = validatorDTO.apr, commission = validatorDTO.commission, rewardType),
            commission = validatorDTO.commission,
            stakedBalance = validatorDTO.stakedBalance,
            votingPower = validatorDTO.votingPower,
            preferred = validatorDTO.preferred == true,
            isStrategicPartner = isStrategicPartner(address, name),
        )
    }

    /**
     * Maps a persisted [StakingValidatorEntity] back into a domain [Yield.Validator], for validators resolved from
     * the database instead of a live `yields/enabled` response (e.g. historical validators no longer returned by
     * StakeKit). The entity has no [RewardType] of its own (that's a property of the parent yield, not the
     * validator), so [RewardInfo.type] falls back to [RewardType.UNKNOWN].
     */
    fun convertFromEntity(entity: StakingValidatorEntity): Yield.Validator {
        val name = entity.name ?: entity.address

        return Yield.Validator(
            address = entity.address,
            status = convertValidatorStatus(entity.status),
            name = name,
            image = entity.image,
            website = entity.website,
            rewardInfo = createRewardInfo(
                apr = entity.apr?.toBigDecimalOrNull(),
                commission = entity.commission,
                rewardType = RewardType.UNKNOWN,
            ),
            commission = entity.commission,
            stakedBalance = entity.stakedBalance,
            votingPower = entity.votingPower,
            preferred = entity.isPreferred == true,
            isStrategicPartner = isStrategicPartner(entity.address, name),
        )
    }

    private fun createRewardInfo(apr: BigDecimal?, commission: Double?, rewardType: RewardType): RewardInfo? {
        // gross = net / (1 - commission)
        return try {
            val netApy = apr
            val grossAprOrApy = if (netApy != null && commission != null) {
                val commissionFraction = commission.toBigDecimal()
                if (commissionFraction < 1.toBigDecimal()) {
                    netApy.divide(
                        1.toBigDecimal() - commissionFraction,
                        DIVIDE_SCALE,
                        RoundingMode.HALF_UP,
                    )
                } else {
                    netApy
                }
            } else {
                netApy
            }
            grossAprOrApy?.let { RewardInfo(rate = it, type = rewardType) }
        } catch (_: Exception) {
            apr?.let { RewardInfo(rate = it, type = rewardType) }
        }
    }

    private fun convertRewardType(rewardTypeDTO: YieldDTO.RewardTypeDTO): RewardType {
        return when (rewardTypeDTO) {
            YieldDTO.RewardTypeDTO.APY -> RewardType.APY
            YieldDTO.RewardTypeDTO.APR -> RewardType.APR
            else -> RewardType.UNKNOWN
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

    private fun convertValidatorStatus(statusName: String?): ValidatorStatus {
        return ValidatorStatus.entries.find { it.name == statusName } ?: ValidatorStatus.UNKNOWN
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