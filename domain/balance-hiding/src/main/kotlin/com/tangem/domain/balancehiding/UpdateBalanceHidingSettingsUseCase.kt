package com.tangem.domain.balancehiding

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.balancehiding.error.HideBalancesError
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository

class UpdateBalanceHidingSettingsUseCase(
    private val balanceHidingRepository: BalanceHidingRepository,
) {

    suspend operator fun invoke(
        update: BalanceHidingSettings.() -> BalanceHidingSettings,
    ): Either<HideBalancesError, Unit> = either {
        val settings = catch({ balanceHidingRepository.getBalanceHidingSettings() }) {
            raise(HideBalancesError.DataError(it))
        }

        if (settings.isHidingEnabledInSettings) {
            catch(
                block = { balanceHidingRepository.storeBalanceHidingSettings(update(settings)) },
                catch = { raise(HideBalancesError.DataError(it)) },
            )
        } else {
            raise(HideBalancesError.HidingDisabled)
        }
    }
}