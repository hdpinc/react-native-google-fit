/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Based on Asim Malik android source code, copyright (c) 2015
 *
 **/

package com.reactnative.googlefit;

import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class SleepHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;

    private static final String TAG = "RNGoogleFit-Sleep";
    private static final String sleepPermissionsError = "4: The user must be signed in to make this API call.";
    private static final int sleepErrorCode = 4;
    public SleepHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public void getSleepData(double startDate, double endDate, final Callback errorCallback, final Callback successCallback) {
        SessionReadRequest request = new SessionReadRequest.Builder()
                .readSessionsFromAllApps()
                .read(DataType.TYPE_ACTIVITY_SEGMENT)
                .setTimeInterval((long) startDate, (long) endDate, TimeUnit.MILLISECONDS)
                .build();

        final GoogleSignInAccount gsa = GoogleSignIn.getAccountForScopes(this.mReactContext, new Scope(Scopes.FITNESS_ACTIVITY_READ));
        Fitness.getSessionsClient(this.mReactContext, gsa)
                .readSession(request)
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @Override
                    public void onSuccess(SessionReadResponse response) {
                        List<Session> sleepSessions = response.getSessions();
                        WritableArray sleep = Arguments.createArray();

                        for (Session session : sleepSessions) {
                            processDataSet(session, sleep);
                        }
                        successCallback.invoke(sleep);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Failure: " + e.getMessage());
                        if (sleepPermissionsError.equals(e.getMessage())) {
                            requestSleepPermissions(gsa);
                            errorCallback.invoke(sleepPermissionsError);
                        } else {
                            errorCallback.invoke(e.getMessage());
                        }
                    }
                });
    }

    void requestSleepPermissions(GoogleSignInAccount account) {
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
                .build();
        GoogleSignIn.requestPermissions(this.mReactContext.getCurrentActivity(), sleepErrorCode, account, fitnessOptions);
    }

    private void processDataSet(Session session, WritableArray map) {
        DateFormat dateFormat = DateFormat.getDateInstance();
        DateFormat timeFormat = DateFormat.getTimeInstance();
        Format formatter = new SimpleDateFormat("EEE");

        String activity = session.getActivity();
        Log.i(TAG, "\tActivity: " + activity);
        if(activity.equals(FitnessActivities.SLEEP)){
            long startTime = session.getStartTime(TimeUnit.MILLISECONDS);
            long endTime = session.getEndTime(TimeUnit.MILLISECONDS);
            Double duration = new Double((endTime - startTime) / 1000 / 60);
            String day = formatter.format(new Date(startTime));

            Log.i(TAG, "\tData point:");
            Log.i(TAG, "\tType: " + session.getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(startTime) + " " + timeFormat.format(startTime));
            Log.i(TAG, "\tEnd: " + dateFormat.format(endTime) + " " + timeFormat.format(endTime));
            Log.i(TAG, "\tDay: " + day);

            WritableMap sleepMap = Arguments.createMap();
            sleepMap.putString("day", day);
            sleepMap.putDouble("startDate", startTime);
            sleepMap.putDouble("endDate", endTime);
            sleepMap.putDouble("value", duration.intValue());
            map.pushMap(sleepMap);
        }
    }

}
