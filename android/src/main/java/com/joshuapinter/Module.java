package com.joshuapinter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
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
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;


public class Module extends ReactContextBaseJavaModule {

    private static final String DURATION_SHORT_KEY = "SHORT";
    private static final String DURATION_LONG_KEY = "LONG";
    private int CONTACT_PICKER_REQUEST = 300;
    Callback _callback;

    public Module(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(mActivityEventListener);
    }

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            if (requestCode == CONTACT_PICKER_REQUEST) {
                if (resultCode == RESULT_OK) {
                    ContactResult  results = MultiContactPicker.obtainContact(intent);
                    Gson g = new Gson();
                    _callback.invoke(g.toJson(results));
                    //Log.d("MyTag", results.get(0).getDisplayName());
                } else if (resultCode == RESULT_CANCELED) {
                    System.out.println("User closed the picker without selecting items.");
                }
            }
        }
    };


    @Override
    public String getName() {
        return "Contacts";
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
        composeBuilder(ids, callback,false);
    }

    @ReactMethod
    public void pickContacts(ReadableArray ids, Callback callback) {
        composeBuilder(ids, callback,true);
    }

    private void composeBuilder(ReadableArray ids, Callback callback,boolean multiSelectEnabled) {
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
    public void userCanAccessContacts(Callback callback){
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke("Activity doesn't exist");
            return;
        }

        int hasPermission = ContextCompat.checkSelfPermission(currentActivity,"");
        if(hasPermission == PackageManager.PERMISSION_GRANTED) {
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

                ContactsProvider contactsProvider = new ContactsProvider(cr);
                WritableArray contacts = contactsProvider.getContacts();

                Log.i("Hello ", "Hello " + contacts);

                callback.invoke(contacts);
            }
        });
    }

    @ReactMethod
    public void openContact(String id) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id);
        intent.setData(uri);
        getReactApplicationContext().startActivity(intent);
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
