package com.motorgimbalconsole.help;

//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.motorgimbalconsole.BuildConfig;
import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;


/**
*   @description: just an about screen
*   @author: boris.dureau@neuf.fr
**/
public class AboutActivity extends AppCompatActivity {

    Button btnDismiss;
    TextView txtViewVersion;
    ConsoleApplication myBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        myBT = (ConsoleApplication) getApplication();

        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        setContentView(R.layout.activity_about);

        btnDismiss = (Button)findViewById(R.id.butDismiss);
        txtViewVersion = (TextView) findViewById(R.id.txtViewVersion);
        txtViewVersion.setText(BuildConfig.VERSION_NAME);
        btnDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();      //exit the about activity
            }
        });


    }


}
