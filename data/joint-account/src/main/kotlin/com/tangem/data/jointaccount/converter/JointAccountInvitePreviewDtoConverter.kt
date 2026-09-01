package com.tangem.data.jointaccount.converter

import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.store.datasource.jointaccount.models.JointAccountInvitePreviewDto
import javax.inject.Inject

/** Network DTO → domain. */
internal class JointAccountInvitePreviewDtoConverter @Inject constructor() {

    fun convert(dto: JointAccountInvitePreviewDto): JointAccountInvitePreview = JointAccountInvitePreview(
        config = JointAccountConfig(
            name = dto.config.name,
            icon = dto.config.icon,
            iconColor = dto.config.iconColor,
            membersCount = dto.config.membersCount,
            threshold = dto.config.threshold,
        ),
        creator = JointAccountInvitePreview.Creator(
            name = dto.creator.name,
            address = dto.creator.address,
        ),
    )
}