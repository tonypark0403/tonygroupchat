package com.example.tonypark.tonygroupchat;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class InitActivity extends AppCompatActivity {

    public static FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    private RelativeLayout relativeLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        relativeLayout = (RelativeLayout) findViewById(R.id.initactivity_relativelayout_main);

        firebaseRemoteConfig.setDefaults(R.xml.default_parameter_config);

        firebaseRemoteConfig.fetch(0)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(InitActivity.this, "Fetch Succeeded",
                                    Toast.LENGTH_SHORT).show();

                            // After config data is successfully fetched, it must be activated before newly fetched
                            // values are returned.
                            firebaseRemoteConfig.activateFetched();
                        } else {
                            Toast.makeText(InitActivity.this, "Fetch Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                        remoteConfigAction();
                    }
                });
    }

    private void remoteConfigAction() {
//        relativeLayout.setBackgroundColor(Color.parseColor(firebaseRemoteConfig.getString("remote_color")));
        String remoteMessage = firebaseRemoteConfig.getString("remote_message");
        if (firebaseRemoteConfig.getBoolean("is_construction")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(remoteMessage).setPositiveButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.create().show();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
