/**
 * Copyright (c) Health Data Platform
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Based on Asim Malik android source code, copyright (c) 2015
 * Develped by Epsilon Software Inc
 **/

package com.reactnative.googlefit;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.Scopes;

import com.google.android.gms.tasks.*;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.lang.InterruptedException;


public class SleepHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;

    private static final String TAG = "SleepHistory";

    public SleepHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public ReadableArray readByDate(long startTime, long endTime) {
        SessionReadRequest readRequest = new SessionReadRequest.Builder()
        .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
        .enableServerQueries()
        .readSessionsFromAllApps()
        .read(DataType.TYPE_ACTIVITY_SEGMENT)
        .build();

        GoogleSignInAccount gsa = GoogleSignIn.getAccountForScopes(this.mReactContext, new Scope(Scopes.FITNESS_ACTIVITY_READ));
        Task<SessionReadResponse> response = Fitness.getSessionsClient(this.mReactContext, gsa).readSession(readRequest);
        WritableArray map = Arguments.createArray();

        try {
            SessionReadResponse sessionReadResponse = Tasks.await(response, 1, TimeUnit.MINUTES);
            List<Session> sessions = sessionReadResponse.getSessions();
            for (Session session : sessions) {
                Log.i(TAG, "Session start");
                processDataSet(session,map);
            }
        } catch (ExecutionException e) {
            Log.i(TAG, e.toString());
        } catch (InterruptedException e) {
            Log.i(TAG, e.toString());
        } catch (TimeoutException e) {
            Log.i(TAG, e.toString());
        }

        return map;
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
