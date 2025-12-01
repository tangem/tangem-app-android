package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayCustomerInfoError
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import timber.log.Timber

private const val TAG = "TangemPayMainScreenCustomerInfoUseCase"

/**
 * Returns tangem pay customer info for the main screen banner
 * Works only if the user already authorised at least once (won't emit anything otherwise)
 */
class TangemPayMainScreenCustomerInfoUseCase(
    private val repository: OnboardingRepository,
    private val customerOrderRepository: CustomerOrderRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
    ): Either<TangemPayCustomerInfoError, MainScreenCustomerInfo> = catch(
        block = {
            repository.checkCustomerWallet(userWalletId)
                .fold(
                    ifLeft = { TangemPayCustomerInfoError.UnknownError.left() },
                    ifRight = { hasTangemPay ->
                        if (hasTangemPay) {
                            proceedWithPaeraCustomerResult(userWalletId)
                        } else {
                            TangemPayCustomerInfoError.UnknownError.left() // ignore if there's no TangemPay
                        }
                    },
                )
        },
        catch = { error ->
            Timber.tag(TAG).e(error)
            TangemPayCustomerInfoError.UnknownError.left()
        },
    )

    private suspend fun proceedWithPaeraCustomerResult(
        userWalletId: UserWalletId,
    ): Either<TangemPayCustomerInfoError, MainScreenCustomerInfo> {
        val orderId = repository.getOrderId(userWalletId)
        return if (orderId != null) {
            proceedWithOrderId(userWalletId = userWalletId, orderId = orderId)
        } else {
            proceedWithoutOrder(userWalletId = userWalletId)
        }
    }

    private suspend fun proceedWithoutOrder(
        userWalletId: UserWalletId,
    ): Either<TangemPayCustomerInfoError, MainScreenCustomerInfo> {
        return repository.getCustomerInfo(userWalletId)
            .mapLeft { error -> error.mapErrorForCustomer() }
            .map { customerInfo ->
                if (customerInfo.cardInfo == null && customerInfo.isKycApproved) {
                    // If order id wasn't saved -> start order creation and get customer info
                    repository.createOrder(userWalletId)
                }
                MainScreenCustomerInfo(info = customerInfo, orderStatus = OrderStatus.UNKNOWN)
            }
    }

    private suspend fun proceedWithOrderId(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<TangemPayCustomerInfoError, MainScreenCustomerInfo> {
        return customerOrderRepository.getOrderStatus(userWalletId, orderId = orderId)
            .fold(
                ifLeft = { error ->
                    error.mapErrorForCustomer().left()
                },
                ifRight = { orderStatus ->
                    when (orderStatus) {
                        // Kyc is passed and user waits for order creation -> no need to get customer info
                        OrderStatus.NEW,
                        OrderStatus.PROCESSING,
                        -> MainScreenCustomerInfo(
                            info = CustomerInfo(productInstance = null, isKycApproved = true, cardInfo = null),
                            orderStatus = orderStatus,
                        ).right()

                        // Order was created/cancelled -> clear order id and get customer info
                        OrderStatus.COMPLETED,
                        OrderStatus.CANCELED,
                        OrderStatus.UNKNOWN,
                        -> {
                            repository.clearOrderId(userWalletId)
                            // If order was cancelled -> start order creation
                            if (orderStatus == OrderStatus.CANCELED) repository.createOrder(userWalletId)
                            repository.getCustomerInfo(userWalletId = userWalletId)
                                .mapLeft { it.mapErrorForCustomer() }
                                .map { customerInfo ->
                                    MainScreenCustomerInfo(info = customerInfo, orderStatus = orderStatus)
                                }
                        }
                    }
                },
            )
    }

    private fun VisaApiError.mapErrorForCustomer(): TangemPayCustomerInfoError {
        return if (this !is VisaApiError.NotPaeraCustomer) {
            TangemPayCustomerInfoError.UnavailableError
        } else {
            TangemPayCustomerInfoError.UnknownError
        }
    }
}