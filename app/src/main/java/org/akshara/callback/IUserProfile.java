package org.akshara.callback;

import org.ekstep.genieservices.sdks.response.GenieResponse;

/**
 * Created by Jaya on 10/5/2015.
 */
public interface IUserProfile {

    public void onSuccessUserProfile(GenieResponse genieResponse);

    public void onFailureUserprofile(GenieResponse genieResponse);
}
