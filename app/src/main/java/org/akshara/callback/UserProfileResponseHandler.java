package org.akshara.callback;

import org.ekstep.genieservices.sdks.response.GenieResponse;
import org.ekstep.genieservices.sdks.response.IResponseHandler;

public class UserProfileResponseHandler implements IResponseHandler {
    private IUserProfile mIUserProfile = null;

    public UserProfileResponseHandler(IUserProfile userProfile) {
        mIUserProfile = userProfile;
    }

    @Override
    public void onSuccess(GenieResponse genieResponse) {
        // Code to handle success scenario
        mIUserProfile.onSuccessUserProfile(genieResponse);
    }

    @Override
    public void onFailure(GenieResponse genieResponse) {
        // Code to handle error scenario
        mIUserProfile.onFailureUserprofile(genieResponse);
    }
}

