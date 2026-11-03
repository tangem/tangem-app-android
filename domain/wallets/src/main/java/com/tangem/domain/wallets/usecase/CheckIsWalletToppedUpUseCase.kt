package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.WalletBalanceState
import com.tangem.domain.wallets.repository.WalletBalanceStateRepository
import javax.inject.Inject

/**
 * Check if wallet has been topped up now.
 * */
class CheckIsWalletToppedUpUseCase @Inject constructor(
    private val walletBalanceStateRepository: WalletBalanceStateRepository,
) {

    /**
     * Check if wallet has been topped up now.
     *
     * @param userWalletId User wallet ID.
     * @param balanceState Current wallet balance.
     *
     * @return Either [Throwable] or [Boolean] representing if wallet has been topped up.
     * */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        balanceState: WalletBalanceState,
    ): Either<Throwable, Boolean> = either {
        // A failed load says nothing about the balance, so it is neither stored nor compared
        if (balanceState == WalletBalanceState.Error) return@either false

        // Older versions stored Error too; like a missing state, it does not prove the wallet was ever empty
        val storedBalanceState = getStoredBalanceState(userWalletId)?.takeIf { it != WalletBalanceState.Error }

        when (storedBalanceState) {
            WalletBalanceState.ToppedUp -> false // already topped up
            null -> {
                setBalanceState(userWalletId, balanceState)
                false // the first known balance is not a top-up, even if the wallet was added with funds
            }
            else -> {
                setBalanceState(userWalletId, balanceState)
                balanceState == WalletBalanceState.ToppedUp
            }
        }
    }

    private suspend fun Raise<Throwable>.getStoredBalanceState(userWalletId: UserWalletId) = catch(
        block = { walletBalanceStateRepository.getWalletBalanceState(userWalletId) },
        catch = ::raise,
    )

    private suspend fun Raise<Throwable>.setBalanceState(userWalletId: UserWalletId, balanceState: WalletBalanceState) {
        catch(
            block = { walletBalanceStateRepository.setWalletBalanceState(userWalletId, balanceState) },
            catch = ::raise,
        )
    }
}