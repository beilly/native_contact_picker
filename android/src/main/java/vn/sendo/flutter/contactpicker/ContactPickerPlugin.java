// Copyright 2017 Michael Goderbauer. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package vn.sendo.flutter.contactpicker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;

import java.util.HashMap;
import java.lang.Exception;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.app.Activity.RESULT_OK;

public class ContactPickerPlugin implements MethodCallHandler, PluginRegistry.ActivityResultListener {
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "native_contact_picker");
    ContactPickerPlugin instance = new ContactPickerPlugin(registrar.activity());
    registrar.addActivityResultListener(instance);
    channel.setMethodCallHandler(instance);
  }

    private ContactPickerPlugin(Activity activity) {
        this.activity = activity;
    }

  private static int PICK_CONTACT = 2015;

  private Activity activity;
  private Result pendingResult;

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("selectContact")) {
      if (pendingResult != null) {
        pendingResult.error("multiple_requests", "Cancelled by a second request.", null);
        pendingResult = null;
      }
      pendingResult = result;

      Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
      activity.startActivityForResult(i, PICK_CONTACT);
    } else if (call.method.equals("openSettings")) {
      openSettings();
      pendingResult.success(true);
      pendingResult = null;
    } else {
      result.notImplemented();
    }
  }
 // private Registrar registrar;

  private void openSettings() {
    //Activity activity = registrar.activity();
    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.getPackageName()));
    intent.addCategory(Intent.CATEGORY_DEFAULT);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    activity.startActivity(intent);
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != PICK_CONTACT) {
      return false;
    }

    if (null == pendingResult){
      return true;
    }

    if (resultCode != RESULT_OK) {
      try {
        pendingResult.success(null);
      } catch (Exception ex) {
        exception.printStackTrace();
      }
      
      pendingResult = null;
      return true;
    }
    try {
      Uri contactUri = data.getData();
      Cursor cursor = activity.getContentResolver().query(contactUri, null, null, null, null);
      cursor.moveToFirst();

      int phoneType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
      String customLabel = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
      String label = (String) ContactsContract.CommonDataKinds.Email.getTypeLabel(activity.getResources(), phoneType, customLabel);
      String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
      String fullName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

      HashMap<String, Object> contact = new HashMap<>();
      contact.put("fullName", fullName);
      contact.put("phoneNumber", number);

      pendingResult.success(contact);
    } catch (Exception exception) {
      exception.printStackTrace();

      try {
        pendingResult.success(null);
      } catch (Exception ex) {
        exception.printStackTrace();
      }
    }

    pendingResult = null;
    return true;
  }
}
