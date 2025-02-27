/*
 * Copyright (C) 2015-2022 Emanuel Moecklin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onegravity.rteditor.demo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class RTEditorBaseActivity extends AppCompatActivity {

    protected static final String PARAM_SUBJECT = "subject";
    protected static final String PARAM_MESSAGE = "message";
    protected static final String PARAM_SIGNATURE = "signature";
    protected static final String PARAM_DARK_THEME = "useDarkTheme";
    protected static final String PARAM_SPLIT_TOOLBAR = "splitToolbar";
    private static final String PARAM_REQUEST_IN_PROCESS = "requestPermissionsInProcess";

    private static final int REQUEST_PERMISSION = 3;
    private static final String PREFERENCE_PERMISSION_DENIED = "PREFERENCE_PERMISSION_DENIED";

    private AtomicBoolean mRequestPermissionsInProcess = new AtomicBoolean();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            boolean tmp = savedInstanceState.getBoolean(PARAM_REQUEST_IN_PROCESS, false);
            mRequestPermissionsInProcess.set(tmp);
        }

        checkPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE});
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(PARAM_REQUEST_IN_PROCESS, mRequestPermissionsInProcess.get());
    }

    private void checkPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissionInternal(permissions);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean checkPermissionInternal(String[] permissions) {
        ArrayList<String> requestPerms = new ArrayList<String>();
        for (String permission : permissions) {
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED && !userDeniedPermissionAfterRationale(permission)) {
                requestPerms.add(permission);
            }
        }
        if (requestPerms.size() > 0 && ! mRequestPermissionsInProcess.getAndSet(true)) {
            //  We do not have this essential permission, ask for it
            requestPermissions(requestPerms.toArray(new String[requestPerms.size()]), REQUEST_PERMISSION);
            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            for (int i = 0, len = permissions.length; i < len; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                        showRationale(permission, R.string.permission_denied_storage);
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showRationale(final String permission, int promptResId) {
        if (shouldShowRequestPermissionRationale(permission) && !userDeniedPermissionAfterRationale(permission)) {

            //  Notify the user of the reduction in functionality and possibly exit (app dependent)
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_denied)
                    .setMessage(promptResId)
                    .setPositiveButton(R.string.permission_deny, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try { dialog.dismiss(); } catch (Exception ignore) { }
                            setUserDeniedPermissionAfterRationale(permission);
                            mRequestPermissionsInProcess.set(false);
                        }
                    })
                    .setNegativeButton(R.string.permission_retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try { dialog.dismiss(); } catch (Exception ignore) { }
                            mRequestPermissionsInProcess.set(false);
                            checkPermissions(new String[]{permission});
                        }
                    })
                    .show();
        }
        else {
            mRequestPermissionsInProcess.set(false);
        }
    }

    private boolean userDeniedPermissionAfterRationale(String permission) {
        SharedPreferences sharedPrefs = getSharedPreferences(getClass().getSimpleName(), Context.MODE_PRIVATE);
        return sharedPrefs.getBoolean(PREFERENCE_PERMISSION_DENIED + permission, false);
    }

    private void setUserDeniedPermissionAfterRationale(String permission) {
        SharedPreferences.Editor editor = getSharedPreferences(getClass().getSimpleName(), Context.MODE_PRIVATE).edit();
        editor.putBoolean(PREFERENCE_PERMISSION_DENIED + permission, true).commit();
    }

    protected String getStringExtra(Intent intent, String key) {
        String s = intent.getStringExtra(key);
        return s == null ? "" : s;
    }

}
