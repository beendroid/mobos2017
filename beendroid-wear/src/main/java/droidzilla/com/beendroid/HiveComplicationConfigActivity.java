package droidzilla.com.beendroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by ac on 08/01/17.
 */
public class HiveComplicationConfigActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hive_complication_config);
        final EditText txtHiveId = (EditText) findViewById(R.id.hive_id);
        Button hiveIdBtnOk = (Button) findViewById(R.id.hive_id_btn_ok);
        hiveIdBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = getSharedPreferences("com.beendroid.hive.PREFERENCE_FILE", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("hive-id", txtHiveId.getText().toString());
                editor.commit();
                Intent data = getIntent();
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }
}
