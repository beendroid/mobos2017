package droidzilla.com.beendroid;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.support.wearable.complications.ProviderUpdateRequester;

/**
 * Created by ac on 07/01/17.
 */
public class UpdateComplicationDataService extends IntentService {

    public static final String ACTION_UPDATE_COMPLICATION = "com.droidzilla.beendroid.weight.UPDATE_COMPLICATION";
    public static final String EXTRA_COMPLICATION_ID = "com.droidzilla.beendroid.weight.COMPLICATION_ID";

    public UpdateComplicationDataService() {
        super("UpdateComplicationDataService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {

            final String action = intent.getAction();

            if (ACTION_UPDATE_COMPLICATION.equals(action)) {

                int complicationId = intent.getIntExtra(EXTRA_COMPLICATION_ID, -1);
                handleActionUpdateComplicationData(complicationId);
            }
        }
    }

    private void handleActionUpdateComplicationData(int complicationId) {

        ComponentName componentName = new ComponentName(getApplicationContext(), HiveWeightComplicationProviderService.class);
        ProviderUpdateRequester providerUpdateRequester = new ProviderUpdateRequester(getApplicationContext(), componentName);

        if (complicationId > 0) {
            providerUpdateRequester.requestUpdate(complicationId);
        }
    }
}