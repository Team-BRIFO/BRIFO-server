package com.brifo.server.ap.exception

import com.brifo.server.ap.code.ApErrorCode

class TutorialRewardAlreadyClaimedException(
    message: String = ApErrorCode.TUTORIAL_REWARD_ALREADY_CLAIMED.message,
) : ApException(
    errorCode = ApErrorCode.TUTORIAL_REWARD_ALREADY_CLAIMED,
    message = message,
)
