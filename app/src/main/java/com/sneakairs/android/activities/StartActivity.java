package com.sneakairs.android.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ListViewAutoScrollHelper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.Manifest;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.clans.fab.*;

import com.google.gson.Gson;
import com.sneakairs.android.App;
import com.sneakairs.android.R;
import com.sneakairs.android.activities.reminders.ReminderActivity_;
import com.sneakairs.android.adaptors.RemindersListAdaptor;
import com.sneakairs.android.models.ReminderGeoPoint;
import com.sneakairs.android.models.ReminderGeoPointList;
import com.sneakairs.android.services.BluetoothService;
import com.sneakairs.android.services.MusicService;
import com.sneakairs.android.services.ReminderService;
import com.sneakairs.android.utils.Constants;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

@EActivity(R.layout.activity_start)
public class StartActivity extends AppCompatActivity {

    private static final String TAG = "StartActivity";

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 100;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 200;

    @ViewById(R.id.fab_menu) FloatingActionMenu fabMenu;
    @ViewById(R.id.reminders_recycler_view) RecyclerView recyclerView;
    @ViewById(R.id.linearLayout_start) LinearLayout linearLayout;
    @ViewById(R.id.empty_reminders_message) TextView emptyMessage;

    BroadcastReceiver broadcastReceiver;
    BroadcastReceiver reminderBroadcastReceiver;

    RemindersListAdaptor adaptor;

    Realm realm;

    @Override
    protected void onResume() {
        super.onResume();
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    Log.d(TAG, "BroadCast received " + Constants.REMINDER_UPDATE_INTENT_FILTER);

                    List<ReminderGeoPoint> buzzReminders = new Gson().fromJson(intent.getStringExtra("buzzReminders"), ReminderGeoPointList.class);

                    Log.d(TAG, "buzzReminders.size() = " + String.valueOf(buzzReminders.size()));
                    if (buzzReminders.size() > 0) {
                        List<String> messages = new ArrayList<>();
                        for (ReminderGeoPoint reminderGeoPoint : buzzReminders) {
                            messages.add(reminderGeoPoint.getMessage());

                            Log.d(TAG, "buzzReminders | " + reminderGeoPoint.getMessage());
                        }

                        adaptor = new RemindersListAdaptor(buzzReminders, context);
                        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
                        recyclerView.setAdapter(adaptor);
                        setEmptyView(false);
                    } else {
                        setEmptyView(true);
                    }
                }
            };
            registerReceiver(broadcastReceiver, new IntentFilter(Constants.REMINDER_UPDATE_INTENT_FILTER));
        }

        if (reminderBroadcastReceiver == null) {
            reminderBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "BroadCast received " + Constants.REMINDERS_LIST_UPDATED_INTENT_FILTER);

                    if (adaptor != null) {
                        adaptor.setList(App.buzzRemindersList);
                        adaptor.notifyDataSetChanged();

                        if (App.buzzRemindersList.size() > 0) setEmptyView(false);
                        else setEmptyView(true);
                    }
                }
            };
            registerReceiver(reminderBroadcastReceiver, new IntentFilter(Constants.REMINDERS_LIST_UPDATED_INTENT_FILTER));
        }
    }

    @AfterViews
    protected void afterViews() {
        askPermissions();

        Log.d(TAG, "Count = " + App.buzzRemindersList.size());
        if (App.buzzRemindersList.size() > 0) {
            adaptor = new RemindersListAdaptor(App.buzzRemindersList, this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            recyclerView.setAdapter(adaptor);
            setEmptyView(false);
        } else setEmptyView(true);

        if (!App.isReminderServiceRunning && App.remindersList != null && App.remindersList.size() > 0) {
            startService(new Intent(this, ReminderService.class));
        }

        if (!App.isBluetoothServiceRunning)
            startService(new Intent(this, BluetoothService.class));

        if (!App.isMusicServiceRunning)
            startService(new Intent(this, MusicService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);

        if (reminderBroadcastReceiver != null)
            unregisterReceiver(reminderBroadcastReceiver);

    }

    private void setEmptyView(boolean isEmptyView) {
        Log.d(TAG, "SetEmptyView called with " + isEmptyView);
        if (isEmptyView) {
            linearLayout.setVisibility(View.GONE);
            emptyMessage.setVisibility(View.VISIBLE);
        } else {
            linearLayout.setVisibility(View.VISIBLE);
            emptyMessage.setVisibility(View.GONE);
        }

    }


    @TargetApi(Build.VERSION_CODES.M)
    private void askPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_BLUETOOTH: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Bluetooth permission was granted. Now ask for Location
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                } else {
                    showPermissionsDialog();
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location Permission granted

                } else
                    showPermissionsDialog();
            }
        }

    }

    @Click(R.id.fab_music)
    protected void goToMusicActivity() {
        Intent intent = new Intent(this, MusicActivity_.class);
        startActivity(intent);
        fabMenu.close(true);
    }

    @Click(R.id.fab_directions)
    protected void goToDirectionsActivity() {
        Intent intent = new Intent(this, DirectionsActivity_.class);
        startActivity(intent);
        fabMenu.close(true);
    }

    @Click(R.id.fab_reminder)
    protected void goToReminderActivity() {
        Intent intent = new Intent(this, ReminderActivity_.class);
        startActivity(intent);
        fabMenu.close(true);
    }

    private void showPermissionsDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(StartActivity.this).create();
        alertDialog.setTitle("Permission Required");
        alertDialog.setMessage("SneakAirs requires Bluetooth and Location permissions for the best experience.");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}
