package com.tangem.domain.balancehiding

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import com.tangem.domain.balancehiding.error.HideBalancesError
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest

class ListenToFlipsUseCase(
    private val flipDetector: DeviceFlipDetector,
    private val balanceHidingRepository: BalanceHidingRepository,
) {

    var isEnabled: Boolean = true

    operator fun invoke(): Flow<Either<HideBalancesError, Unit>> = channelFlow {
        flipDetector.getDeviceFlipFlow().collectLatest {
            val balanceHidingSettings = catch(
                block = { balanceHidingRepository.getBalanceHidingSettings() },
                catch = {
                    send(HideBalancesError.DataError(it).left())
                    return@collectLatest
                },
            )

            if (balanceHidingSettings.isHidingEnabledInSettings && isEnabled) {
                catch(
                    block = {
                        balanceHidingRepository.storeBalanceHidingSettings(
                            balanceHidingSettings.copy(
                                isBalanceHidden = !balanceHidingSettings.isBalanceHidden,
                            ),
                        )
                    },
                    catch = { send(HideBalancesError.DataError(it).left()) },
                )
            } else {
                send(HideBalancesError.HidingDisabled.left())
            }
        }
    }
}
