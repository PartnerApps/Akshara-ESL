package org.akshara.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.Gson;

import org.akshara.R;
import org.akshara.Util.Util;
import org.akshara.callback.DelegateAction;
import org.akshara.callback.IRegister;
import org.akshara.callback.RegisterResponseHandler;
import org.akshara.db.DatabaseHandler;
import org.akshara.db.DatabaseHelper;
import org.akshara.services.CSVService;
import org.ekstep.genieservices.sdks.Partner;
import org.ekstep.genieservices.sdks.response.GenieResponse;

import java.util.ArrayList;
import java.util.List;

/*
* Splashscreeen will show logo of Partner App(Akshara)
* Automatic will register to partner
* */
public class Splashscreeen extends Activity implements IRegister, DelegateAction {
    private boolean D = Util.DEBUG;
    private String TAG = Splashscreeen.class.getSimpleName();
    private Context mContext;
    private Partner partner;
    private RegisterResponseHandler responseHandler;

    public static boolean isAppInstalled(String packageName, Context context) {
        PackageManager pm = context.getPackageManager();
        boolean installed = false;
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splashscreen);
        /* CREATE DB*/
        // createDB();
        mContext = Splashscreeen.this;

        isPartnerRegistered();
    }

    private void createDB() {
        if (checkAppPermission()) {
            requestAppPermission();
        } else {
            readFile();
        }

    }

    private void readFile() {
        if (D)
            Log.d(TAG, "readFile---------");
        Intent intent = new Intent(this, CSVService.class);
        CSVService.delegateAction = Splashscreeen.this;
        startService(intent);
    }

    private boolean checkAppPermission() {
        for (int i = 0; i < Util.PERMISSIONS.length; i++) {
            if (ActivityCompat.checkSelfPermission(this, Util.PERMISSIONS[i])
                    != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    private void requestAppPermission() {
        List<String> permissionNeeded = new ArrayList<>();
        for (int i = 0; i < Util.PERMISSIONS.length; i++) {
            String permission = Util.PERMISSIONS[i];
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionNeeded.add(permission);
            }
        }
        if (permissionNeeded.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    permissionNeeded.toArray(new String[permissionNeeded.size()]), 0);
        }
    }

    private void isPartnerRegistered() {
        if (isAppInstalled(Util.PACKAGENAME, mContext)) {
           /* if(new Util(Splashscreeen.this).isRegisterPartner()){
                callNextScreen();
            }else{*/
            partner = new Partner(Splashscreeen.this);
            responseHandler = new RegisterResponseHandler(this);
            partner.register(Util.partnerId, Util.publicKey, responseHandler);
            //}
        } else {
            Toast.makeText(mContext, getString(R.string.Genie_service_Check), Toast.LENGTH_LONG).show();

            Intent openPlayStore = new Intent(Intent.ACTION_VIEW);
            openPlayStore.setData(Uri.parse(
                    "https://play.google.com/store/apps/details?id=org.ekstep.genieservices"));
            startActivity(openPlayStore);
        }


    }

    @Override
    public void onSuccess(GenieResponse genieResponse) {
        String result = new Gson().toJson(genieResponse.getResult());
        new Util(Splashscreeen.this).storePartnerRegistration(Util.partnerId);
        if (D)
            Log.d(TAG, "onSuccess GenieResponse :" + result);
        callNextScreen();

    }

    private void callNextScreen() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent inregis = new Intent(Splashscreeen.this, SimplePinAuthActivity.class);
                startActivity(inregis);
                finish();

            }
        }, 2 * 1000);

    }

    @Override
    public void onFailure(GenieResponse genieResponse) {
        String result = new Gson().toJson(genieResponse.getResult());
        Toast.makeText(mContext, "AKSHARA registration failed.", Toast.LENGTH_LONG).show();
        if (D)
            Log.d(TAG, " onFailureGenieResponse :" + result);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Splashscreeen.this.finish();

            }
        }, 2 * 1000);


    }

    @Override
    protected void onStop() {
        if (D)
            Log.d(TAG, "onStop");
        super.onStop();
        if (partner != null)
            partner.finish();
    }

    @Override
    public void onActionStart() {

    }

    @Override
    public void onActionCompleted(Boolean result) {

        if (D)
            Log.d(TAG, " -->writeDB onActionCompleted  :" + result);
        Log.i("exportDatabse", "getPackageName:" + Util.getPackageName(Splashscreeen.this));

        DatabaseHelper.exportDatabse(DatabaseHandler.DATABASE_NAME);
    }

    @Override
    public void onActionFailed(String failedResult) {
        if (D)
            Log.d(TAG, "onActionFailed :" + failedResult);
    }

}
