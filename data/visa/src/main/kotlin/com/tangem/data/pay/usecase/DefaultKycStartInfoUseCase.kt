package com.tangem.data.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.error.UniversalError
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.KycStartInfo
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.domain.pay.usecase.KycStartInfoUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.visa.error.VisaApiError
import javax.inject.Inject

/**
 * For TangemPay Customer Wallet auth we are using polygon address
 */
private const val POL_VALUE = "coin⟨POLYGON⟩polygon-ecosystem-token"

internal class DefaultKycStartInfoUseCase @Inject constructor(
    private val userWalletsRepository: UserWalletsListRepository,
    private val getCurrencyUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val kycRepository: KycRepository,
) : KycStartInfoUseCase {

    override suspend fun invoke(): Either<UniversalError, KycStartInfo> = either {
        val wallet = userWalletsRepository.userWalletsSync().find { it is UserWallet.Cold } as? UserWallet.Cold
            ?: raise(VisaApiError.UnknownWithoutCode)

        val address = getCurrencyUseCase.invokeMultiWalletSync(wallet.walletId, CryptoCurrency.ID.fromValue(POL_VALUE))
            .getOrNull()?.value?.networkAddress?.defaultAddress?.value ?: raise(VisaApiError.UnknownWithoutCode)

        kycRepository.getKycStartInfo(address, wallet.cardId).bind()
    }
}