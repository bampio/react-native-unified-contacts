package com.joshuapinter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.google.gson.Gson;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class Module extends ReactContextBaseJavaModule {

    private static final String DURATION_SHORT_KEY = "SHORT";
    private static final String DURATION_LONG_KEY = "LONG";
    private int CONTACT_PICKER_REQUEST = 300;
    Callback _callback;
    final String IDENTIFIER_KEY = "IDENTIFIER_KEY";
    SharedPreferences sharedpreferences;

    public Module(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(mActivityEventListener);
        sharedpreferences = reactContext.getSharedPreferences("KEYS", Context.MODE_PRIVATE);
    }

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            if (requestCode == CONTACT_PICKER_REQUEST) {
                if (resultCode == RESULT_OK) {
                    ContactResult results = MultiContactPicker.obtainContact(intent);
                    Gson g = new Gson();
                    _callback.invoke(g.toJson(results));
                } else if (resultCode == RESULT_CANCELED) {
                    System.out.println("User closed the picker without selecting items.");
                }
            }
        }
    };


    @Override
    public String getName() {
        return "RNUnifiedContacts";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(DURATION_SHORT_KEY, Toast.LENGTH_SHORT);
        constants.put(DURATION_LONG_KEY, Toast.LENGTH_LONG);
        return constants;
    }

    @ReactMethod
    public void show(String message, int duration) {
        Toast.makeText(getReactApplicationContext(), message, duration).show();
    }

    @ReactMethod
    public void generateHash(String identifier, Callback callback) {
        try {
            callback.invoke(hash256(identifier));
        } catch (Exception ex) {
            callback.invoke(ex.getMessage());
        }
    }

    @ReactMethod
    public void pickContact(ReadableArray ids, Callback callback) {
        composeBuilder(ids, callback, false);
    }

    @ReactMethod
    public void pickContacts(ReadableArray ids, Callback callback) {
        composeBuilder(ids, callback, true);
    }

    private void composeBuilder(ReadableArray ids, Callback callback, boolean multiSelectEnabled) {
        _callback = callback;
        new MultiContactPicker.Builder(getCurrentActivity()) //Activity/fragment context
                //.theme(R.style.MyCustomPickerTheme) //Optional - default: MultiContactPicker.Azure
                .hideScrollbar(false) //Optional - default: false
                .showTrack(true) //Optional - default: true
                .ids(ids.toArrayList())
                .multiSelectEnabled(multiSelectEnabled)
                .searchIconColor(Color.WHITE) //Option - default: White
                //.handleColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary)) //Optional - default: Azure Blue
                //.bubbleColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary)) //Optional - default: Azure Blue
                .bubbleTextColor(Color.WHITE) //Optional - default: White
                .showPickerForResult(CONTACT_PICKER_REQUEST);
    }

    @ReactMethod
    public void userCanAccessContacts(Callback callback) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke("Activity doesn't exist");
            return;
        }

        int hasPermission = ContextCompat.checkSelfPermission(currentActivity, "android.permission.READ_CONTACTS");
        if (hasPermission == PackageManager.PERMISSION_GRANTED) {
            callback.invoke(true);
        } else {
            callback.invoke(false);
        }
    }

    @ReactMethod
    public void getContacts(final Callback callback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Context context = getReactApplicationContext();
                ContentResolver cr = context.getContentResolver();

                ContactsProvider contactsProvider = new ContactsProvider(cr,Module.this);
                WritableArray contacts = contactsProvider.getContacts();

                Log.i("Contacts", "Contacts " + contacts);

                callback.invoke(contacts);
            }
        });
    }

    @ReactMethod
    public void openPrivacySettings() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getCurrentActivity().getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getCurrentActivity().startActivityForResult(myAppSettings, 20000);
    }

    @ReactMethod
    public void openContact(String id) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id);
        intent.setData(uri);
        getReactApplicationContext().startActivity(intent);
    }

    @ReactMethod
    public void getSources(final Callback callback) {
        ArrayList<ContactAccount> uniques = new ArrayList<>();
        ContentResolver resolver = getCurrentActivity().getContentResolver();
        Cursor cursor = null;
        try {
            String[] projection1 = {ContactsContract.RawContacts._ID, ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE};

            ArrayList<ContactAccount> sets = new ArrayList<>();
            String selection = null;

            String loginAccount = "";

            cursor = resolver.query(ContactsContract.RawContacts.CONTENT_URI, projection1, selection, null, null);
            while (cursor != null && cursor.moveToNext()) {
                ContactAccount account = new ContactAccount();

                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts._ID));
                String accountName = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME));
                String accountType = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));

                account.setIdentifier(id);
                account.setTitle(accountName);
                account.setType(accountType);
                if (account.getType().equalsIgnoreCase("com.google")) {
                    loginAccount = accountName;
                }
                account.setLogin(loginAccount);
                sets.add(account);
            }

            Set<String> titles = new HashSet<>();
            for (ContactAccount item : sets) {
                if (titles.add(item.getType())) {
                    uniques.add(item);
                }
            }
            if (cursor != null && cursor.getCount() > 0) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.i(this.getClass().getName(), e.getMessage());
        } finally {
            //cursor.close();
        }
        Gson gson = new Gson();
        String j = gson.toJson(uniques);
        Log.i("json", "json " + j);
        callback.invoke(null, j);
    }


    @ReactMethod
    public void clearActiveSource(final Callback callback) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(IDENTIFIER_KEY, "");
        editor.commit();
        editor.apply();
        callback.invoke(null, "Cleared active source successfully");
    }

    @ReactMethod
    public void setActiveSource(String identifier, final Callback callback) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(IDENTIFIER_KEY, identifier);
        editor.commit();
        editor.apply();
        callback.invoke(null, "Active source created successfully");
    }

    @ReactMethod
    public void getActiveSource(final Callback callback) {
        String identifier = sharedpreferences.getString(IDENTIFIER_KEY, "");
        if (identifier.length() == 0) {
            callback.invoke("Unable to find active source", null);
        } else {
            callback.invoke(null, identifier);
        }
    }

    public String getSourceId(){
        String identifier = sharedpreferences.getString(IDENTIFIER_KEY, "");
        return identifier;
    }

    public static String hash256(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data.getBytes());
        return bytesToHex(md.digest());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte byt : bytes)
            result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
}
