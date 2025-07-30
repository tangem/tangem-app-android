package com.tangem.domain.notifications

import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import javax.inject.Inject

class GetIsHuaweiDeviceWithoutGoogleServicesUseCase @Inject constructor(
    private val appInfoProvider: AppInfoProvider,
    private val pushNotificationsTokenProvider: PushNotificationsTokenProvider,
) {

    suspend operator fun invoke(): Boolean {
        return appInfoProvider.isHuaweiDevice && pushNotificationsTokenProvider.getToken().isEmpty()
    }
}