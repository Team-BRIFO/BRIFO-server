package com.brifo.server.ap.exception

import com.brifo.server.ap.code.ApErrorCode

class AttendanceRewardAlreadyClaimedException(
    message: String = ApErrorCode.ATTENDANCE_REWARD_ALREADY_CLAIMED.message,
) : ApException(
    errorCode = ApErrorCode.ATTENDANCE_REWARD_ALREADY_CLAIMED,
    message = message,
)
