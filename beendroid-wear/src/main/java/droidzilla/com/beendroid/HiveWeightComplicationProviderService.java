package droidzilla.com.beendroid;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.ProviderChooserIntent;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by ac on 07/01/17.
 */

public class HiveWeightComplicationProviderService extends ComplicationProviderService {

    private double weight;

    @Override
    public void onComplicationUpdate(final int complicationId, final int dataType, final ComplicationManager complicationManager) {

        SharedPreferences sharedPref = getSharedPreferences("com.beendroid.hive.PREFERENCE_FILE", Context.MODE_PRIVATE);
        String hiveId = sharedPref.getString("hive-id", "123");
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference databaseReference = database.getReference("hive-log-" + hiveId);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                try {
                    JSONObject jsonObject = new JSONObject(value);
                    weight = jsonObject.getDouble("weight");
//                    temperature = jsonObject.getDouble("temperature");
//                    humidity = jsonObject.getDouble("humidity");
                    String hiveWeightText = String.format(Locale.getDefault(), "%.1f", weight);

                    Intent updateIntent = new Intent(getApplicationContext(), UpdateComplicationDataService.class);
                    updateIntent.setAction(UpdateComplicationDataService.ACTION_UPDATE_COMPLICATION);
                    updateIntent.putExtra(UpdateComplicationDataService.EXTRA_COMPLICATION_ID, complicationId);

                    PendingIntent pendingIntent = PendingIntent.getService(
                            HiveWeightComplicationProviderService.this,
                            complicationId,
                            updateIntent,
                            0);

                    ComplicationData complicationData = null;

                    switch (dataType) {
                        case ComplicationData.TYPE_RANGED_VALUE:
                            complicationData = new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                                    .setValue((float) weight)
                                    .setMinValue(0)
                                    .setMaxValue(100)
                                    .setShortText(ComplicationText.plainText(hiveWeightText))
                                    .setTapAction(pendingIntent)
                                    .setIcon(Icon.createWithResource(HiveWeightComplicationProviderService.this, R.drawable.greutate))
                                    .build();
                            break;
                        case ComplicationData.TYPE_SHORT_TEXT:
                            complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                    .setShortText(ComplicationText.plainText(hiveWeightText))
                                    .setTapAction(pendingIntent)
                                    .setIcon(Icon.createWithResource(HiveWeightComplicationProviderService.this, R.drawable.greutate))
                                    .build();
                            break;
                        case ComplicationData.TYPE_LONG_TEXT:
                            complicationData = new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                    .setLongText(ComplicationText.plainText("Hive Weight: " + hiveWeightText))
                                    .setTapAction(pendingIntent)
                                    .setIcon(Icon.createWithResource(HiveWeightComplicationProviderService.this, R.drawable.greutate))
                                    .build();
                            break;
                        default:
                    }

                    if (complicationData != null) {
                        complicationManager.updateComplicationData(complicationId, complicationData);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.e("wf", "Failed to read value.", error.toException());
            }
        });

    }


}
