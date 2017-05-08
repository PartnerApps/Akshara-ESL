package org.akshara.callback;

import org.ekstep.genieservices.sdks.response.GenieResponse;

/**
 * Created by Jaya on 10/5/2015.
 */
public interface IPartnerData {

    public void onSuccessPartner(GenieResponse genieResponse);

    public void onFailurePartner(GenieResponse genieResponse);
}
