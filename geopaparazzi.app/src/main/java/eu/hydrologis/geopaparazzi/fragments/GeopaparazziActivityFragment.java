// MainActivityFragment.java
// Contains the Flag Quiz logic
package eu.hydrologis.geopaparazzi.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.GPLogPreferencesHandler;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.sensors.OrientationSensor;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.ColorUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TextAndBooleanRunnable;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.activities.AboutActivity;
import eu.hydrologis.geopaparazzi.activities.PanicActivity;
import eu.hydrologis.geopaparazzi.activities.SettingsActivity;
import eu.hydrologis.geopaparazzi.core.ApplicationChangeListener;
import eu.hydrologis.geopaparazzi.database.DaoMetadata;
import eu.hydrologis.geopaparazzi.database.TableDescriptions;
import eu.hydrologis.geopaparazzi.dialogs.ColorDialogFragment;
import eu.hydrologis.geopaparazzi.dialogs.GpsInfoDialogFragment;
import eu.hydrologis.geopaparazzi.dialogs.LineWidthDialogFragment;
import eu.hydrologis.geopaparazzi.dialogs.NewProjectDialogFragment;
import eu.hydrologis.geopaparazzi.providers.ProviderTestActivity;
import eu.hydrologis.geopaparazzi.utilities.Constants;

import static eu.geopaparazzi.library.util.LibraryConstants.MAPSFORGE_EXTRACTED_DB_NAME;

/**
 * The fragment of the main geopap view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopaparazziActivityFragment extends Fragment implements View.OnLongClickListener, View.OnClickListener {

    private ImageButton mNotesButton;
    private ImageButton mMetadataButton;
    private ImageButton mMapviewButton;
    private ImageButton mGpslogButton;
    private ImageButton mExportButton;

    private ImageButton mImportButton;

    private MenuItem mGpsMenuItem;
    private OrientationSensor mOrientationSensor;

    private BroadcastReceiver mGpsServiceBroadcastReceiver;
    private static boolean sCheckedGps = false;
    private GpsServiceStatus mLastGpsServiceStatus;
    private int[] mLastGpsStatusExtras;
    private GpsLoggingStatus mLastGpsLoggingStatus = GpsLoggingStatus.GPS_DATABASELOGGING_OFF;
    private double[] lastGpsPosition;
    private FloatingActionButton panicFAB;
    private ResourcesManager resourcesManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_geopaparazzi, container, false);

        // this fragment adds to the menu
        setHasOptionsMenu(true);

        // start gps service
        GpsServiceUtilities.startGpsService(getActivity());

        return v; // return the fragment's view for display
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNotesButton = (ImageButton) view.findViewById(R.id.dashboardButtonNotes);
        mNotesButton.setOnClickListener(this);
        mNotesButton.setOnLongClickListener(this);

        mMetadataButton = (ImageButton) view.findViewById(R.id.dashboardButtonMetadata);
        mMetadataButton.setOnClickListener(this);
        mMetadataButton.setOnLongClickListener(this);

        mMapviewButton = (ImageButton) view.findViewById(R.id.dashboardButtonMapview);
        mMapviewButton.setOnClickListener(this);
        mMapviewButton.setOnLongClickListener(this);

        mGpslogButton = (ImageButton) view.findViewById(R.id.dashboardButtonGpslog);
        mGpslogButton.setOnClickListener(this);
        mGpslogButton.setOnLongClickListener(this);

        mImportButton = (ImageButton) view.findViewById(R.id.dashboardButtonImport);
        mImportButton.setOnClickListener(this);
        mImportButton.setOnLongClickListener(this);

        mExportButton = (ImageButton) view.findViewById(R.id.dashboardButtonExport);
        mExportButton.setOnClickListener(this);
        mExportButton.setOnLongClickListener(this);

        panicFAB = (FloatingActionButton) view.findViewById(R.id.panicActionButton);
        panicFAB.setOnClickListener(this);
        enablePanic(false);
    }


    @Override
    public void onResume() {
        super.onResume();

        GpsServiceUtilities.triggerBroadcast(getActivity());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (mOrientationSensor == null) {
            SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            mOrientationSensor = new OrientationSensor(sensorManager, null);
        }
        mOrientationSensor.register(getActivity(), SensorManager.SENSOR_DELAY_NORMAL);

        if (mGpsServiceBroadcastReceiver == null) {
            mGpsServiceBroadcastReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    onGpsServiceUpdate(intent);
                    checkFirstTimeGps(context);
                }
            };
        }
        GpsServiceUtilities.registerForBroadcasts(getActivity(), mGpsServiceBroadcastReceiver);

    }

    // remove SourceUrlsFragmentListener when Fragment detached
    @Override
    public void onDetach() {
        super.onDetach();

        mOrientationSensor.unregister();
        GpsServiceUtilities.unregisterFromBroadcasts(getActivity(), mGpsServiceBroadcastReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);

        mGpsMenuItem = menu.getItem(3);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tilesource: {

            }
            case R.id.action_new: {
                NewProjectDialogFragment newProjectDialogFragment = new NewProjectDialogFragment();
                newProjectDialogFragment.show(getFragmentManager(), "new project dialog");
                return true;
            }
            case R.id.action_load: {

            }
            case R.id.action_gps: {
                GpsInfoDialogFragment gpsInfoDialogFragment = new GpsInfoDialogFragment();
                gpsInfoDialogFragment.show(getFragmentManager(), "gpsinfo dialog");
                return true;
            }
            case R.id.action_gpsstatus: {
                // open gps status app
                AppsUtilities.checkAndOpenGpsStatus(getActivity());
                return true;
            }
            case R.id.action_settings: {
                Intent preferencesIntent = new Intent(this.getActivity(), SettingsActivity.class);
                startActivity(preferencesIntent);
                return true;
            }
            case R.id.action_about: {
                Intent intent = new Intent(getActivity(), AboutActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_exit: {
                getActivity().finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onLongClick(View v) {
        if (v instanceof ImageButton) {
            ImageButton imageButton = (ImageButton) v;

            String tooltip = imageButton.getContentDescription().toString();
            Snackbar.make(v, tooltip, Snackbar.LENGTH_SHORT).show();
            return true;
        }


        if (v == mNotesButton) {
            String tooltip = "Available providers:";
            for (PackageInfo pack : getActivity().getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                ProviderInfo[] providers = pack.providers;
                if (providers != null) {
                    for (ProviderInfo provider : providers) {
                        Log.d("Example", "provider: " + provider.authority);
                        tooltip = tooltip + "\n" + provider.authority;
                    }
                }
            }
            Snackbar.make(v, tooltip, Snackbar.LENGTH_SHORT).show();
        }


        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == mMetadataButton) {
            LineWidthDialogFragment widthDialog =
                    new LineWidthDialogFragment();
            widthDialog.show(getFragmentManager(), "line width dialog");
        } else if (v == mMapviewButton) {
            ColorDialogFragment colorDialog = new ColorDialogFragment();
            colorDialog.show(getFragmentManager(), "color dialog");
        } else if (v == mGpslogButton) {
            handleGpsLogAction();
        } else if (v == mImportButton) {
            Intent providerIntent = new Intent(getActivity(), ProviderTestActivity.class);
            startActivity(providerIntent);
        } else if (v == panicFAB) {
            if (lastGpsPosition == null) {
                return;
            }

            Intent panicIntent = new Intent(getActivity(), PanicActivity.class);
            double lon = lastGpsPosition[0];
            double lat = lastGpsPosition[1];
            panicIntent.putExtra(LibraryConstants.LATITUDE, lat);
            panicIntent.putExtra(LibraryConstants.LONGITUDE, lon);
            startActivity(panicIntent);
        }

    }

    private void onGpsServiceUpdate(Intent intent) {
        mLastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
        mLastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);
        mLastGpsStatusExtras = GpsServiceUtilities.getGpsStatusExtras(intent);
        lastGpsPosition = GpsServiceUtilities.getPosition(intent);
//        lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
//        lastPositiontime = GpsServiceUtilities.getPositionTime(intent);


        boolean doLog = GPLog.LOG_HEAVY;
        if (doLog && mLastGpsStatusExtras != null) {
            int satCount = mLastGpsStatusExtras[1];
            int satForFixCount = mLastGpsStatusExtras[2];
            GPLog.addLogEntry(this, "satellites: " + satCount + " of which for fix: " + satForFixCount);
        }

        if (mGpsMenuItem != null)
            if (mLastGpsServiceStatus != GpsServiceStatus.GPS_OFF) {
                if (doLog)
                    GPLog.addLogEntry(this, "GPS seems to be on");
                if (mLastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
                    if (doLog)
                        GPLog.addLogEntry(this, "GPS seems to be also logging");
                    mGpsMenuItem.setIcon(R.drawable.actionbar_gps_logging);
                    enablePanic(true);
                } else {
                    if (mLastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                        if (doLog) {
                            GPLog.addLogEntry(this, "GPS has fix");
                        }
                        mGpsMenuItem.setIcon(R.drawable.actionbar_gps_fix_nologging);
                        enablePanic(true);
                    } else {
                        if (doLog) {
                            GPLog.addLogEntry(this, "GPS doesn't have a fix");
                        }
                        mGpsMenuItem.setIcon(R.drawable.actionbar_gps_nofix);
                        enablePanic(false);
                    }
                }
            } else {
                if (doLog)
                    GPLog.addLogEntry(this, "GPS seems to be off");
                mGpsMenuItem.setIcon(R.drawable.actionbar_gps_off);
                enablePanic(false);
            }
    }

    private void checkFirstTimeGps(Context context) {
        if (!sCheckedGps) {
            sCheckedGps = true;
            if (mLastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
                String prompt = getResources().getString(R.string.prompt_gpsenable);
                Utilities.yesNoMessageDialog(context, prompt, new Runnable() {
                    public void run() {
                        Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(gpsOptionsIntent);
                    }
                }, null);
            }
        }
    }


    private void enablePanic(boolean enable) {
        if (enable) {
            panicFAB.show();
        } else {
            panicFAB.hide();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        /*
         * avoid oncreate call when rotating device
         * we don't want data to be reloaded
         */
        super.onConfigurationChanged(newConfig);
    }

    private void initializeResourcesManager() throws Exception {
        ResourcesManager.resetManager();
        resourcesManager = ResourcesManager.getInstance(getContext());

        if (resourcesManager == null) {
            Utilities.yesNoMessageDialog(getActivity(), getString(eu.hydrologis.geopaparazzi.R.string.no_sdcard_use_internal_memory),
                    new Runnable() {
                        public void run() {
                            ResourcesManager.setUseInternalMemory(true);
                            try {
                                resourcesManager = ResourcesManager.getInstance(getContext());
                                initIfOk();
                            } catch (Exception e) {
                                GPLog.error(this, null, e); //$NON-NLS-1$
                            }
                        }
                    }, new Runnable() {
                        public void run() {
                            getActivity().finish();
                        }
                    }
            );
        } else {
            // create the default mapsforge data extraction db
            File mapsDir = resourcesManager.getMapsDir();
            File newDbFile = new File(mapsDir, MAPSFORGE_EXTRACTED_DB_NAME);
            if (!newDbFile.exists()) {
                AssetManager assetManager = getActivity().getAssets();
                InputStream inputStream = assetManager.open(MAPSFORGE_EXTRACTED_DB_NAME);
                FileUtilities.copyFile(inputStream, new FileOutputStream(newDbFile));
            }
            // initialize rest of resources
            initIfOk();
        }
    }


    private void initIfOk() {
        if (resourcesManager == null) {
            Utilities.messageDialog(getActivity(), R.string.sdcard_notexist, new Runnable() {
                public void run() {
                    getActivity().finish();
                }
            });
            return;
        }

        /*
         * check the logging system
         */
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        GPLogPreferencesHandler.checkLog(preferences);
        GPLogPreferencesHandler.checkLogHeavy(preferences);
        GPLogPreferencesHandler.checkLogAbsurd(preferences);

        checkLogButton();

        // check for screen on
        boolean keepScreenOn = preferences.getBoolean(Constants.PREFS_KEY_SCREEN_ON, false);
        if (keepScreenOn) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        try {
            GeopaparazziApplication.getInstance().getDatabase();

            // Set the project name in the metadata, if not already available
            HashMap<String, String> projectMetadata = DaoMetadata.getProjectMetadata();
            String projectName = projectMetadata.get(TableDescriptions.MetadataTableFields.KEY_NAME.getFieldName());
            if (projectName.length() == 0) {
                File dbFile = resourcesManager.getDatabaseFile();
                String dbName = FileUtilities.getNameWithoutExtention(dbFile);
                DaoMetadata.setValue(TableDescriptions.MetadataTableFields.KEY_NAME.getFieldName(), dbName);
            }

//            initMapsDirManager();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getLocalizedMessage(), e);
            Utilities.toast(getActivity(), R.string.databaseError, Toast.LENGTH_LONG);
        }
    }

    private void checkLogButton() {
        if (mLastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
            mGpslogButton.setBackgroundColor(ColorUtilities.getAccentColor(getContext()));
        } else {
            mGpslogButton.setBackgroundColor(ColorUtilities.getPrimaryColor(getContext()));
        }
    }

    private void handleGpsLogAction() {
        final GPApplication appContext = GeopaparazziApplication.getInstance();
        if (mLastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
            Utilities.yesNoMessageDialog(getActivity(), getString(R.string.do_you_want_to_stop_logging),
                    new Runnable() {
                        public void run() {
                            getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    // stop logging
                                    GpsServiceUtilities.stopDatabaseLogging(appContext);
                                    mGpslogButton.setBackgroundColor(ColorUtilities.getPrimaryColor(getContext()));
                                    GpsServiceUtilities.triggerBroadcast(getActivity());
                                }
                            });
                        }
                    }, null
            );

        } else {
            // start logging
            if (mLastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                final String defaultLogName = "log_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()); //$NON-NLS-1$

                Utilities.inputMessageAndCheckboxDialog(getActivity(), getString(R.string.gps_log_name),
                        defaultLogName, getString(R.string.continue_last_log), false, new TextAndBooleanRunnable() {
                            public void run() {
                                getActivity().runOnUiThread(new Runnable() {
                                    public void run() {
                                        String newName = theTextToRunOn;
                                        if (newName == null || newName.length() < 1) {
                                            newName = defaultLogName;
                                        }

                                        mGpslogButton.setBackgroundColor(ColorUtilities.getAccentColor(getContext()));
                                        GpsServiceUtilities.startDatabaseLogging(appContext, newName, theBooleanToRunOn,
                                                DefaultHelperClasses.GPSLOG_HELPER_CLASS);
                                        GpsServiceUtilities.triggerBroadcast(getActivity());
                                    }
                                });
                            }
                        }
                );

            } else {
                Utilities.messageDialog(getActivity(), R.string.gpslogging_only, null);
            }
        }
    }

    public BroadcastReceiver getGpsServiceBroadcastReceiver() {
        return mGpsServiceBroadcastReceiver;
    }
}
