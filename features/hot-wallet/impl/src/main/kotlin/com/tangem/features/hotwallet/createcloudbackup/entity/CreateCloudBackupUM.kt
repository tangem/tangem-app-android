package com.tangem.features.hotwallet.createcloudbackup.entity

import com.tangem.domain.cloudbackup.password.PasswordStrength
import com.tangem.domain.cloudbackup.password.PasswordStrengthHint

internal sealed interface CreateCloudBackupUM {

    val onBackClick: () -> Unit

    data class SetPassword(
        override val onBackClick: () -> Unit,
        val password: String,
        val isPasswordVisible: Boolean,
        val strength: PasswordStrength?,
        val hint: PasswordStrengthHint,
        val onPasswordChange: (String) -> Unit,
        val onToggleVisibility: () -> Unit,
        val onContinueClick: () -> Unit,
    ) : CreateCloudBackupUM {

        val isContinueEnabled: Boolean = strength?.isAcceptable == true

        override fun toString(): String = "SetPassword(password=***, strength=$strength, hint=$hint)"
    }

    data class ConfirmPassword(
        override val onBackClick: () -> Unit,
        val confirmPassword: String,
        val isPasswordVisible: Boolean,
        val isMismatch: Boolean,
        val isConsentChecked: Boolean,
        val isLoading: Boolean,
        val onPasswordChange: (String) -> Unit,
        val onToggleVisibility: () -> Unit,
        val onConsentChange: (Boolean) -> Unit,
        val onConfirmClick: () -> Unit,
    ) : CreateCloudBackupUM {

        val isConfirmEnabled: Boolean = !isMismatch && isConsentChecked && confirmPassword.isNotEmpty()

        override fun toString(): String =
            "ConfirmPassword(confirmPassword=***, isMismatch=$isMismatch, isLoading=$isLoading)"
    }

    data class Completed(
        val onFinishClick: () -> Unit,
    ) : CreateCloudBackupUM {
        override val onBackClick: () -> Unit = {}
    }
}