package com.joshuapinter;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.joshuapinter.RxContacts.Contact;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import com.google.gson.Gson;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ContactResult implements Parcelable {

    private String mId;
    private String mDisplayName;
    private String mOrganization;
    private boolean mStarred;
    private Uri mPhoto;
    private Uri mThumbnail;
    private List<String> mEmails = new ArrayList<>();
    private List<String> mPhoneNumbers = new ArrayList<>();

    public String getId() {
        return mId;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public boolean isStarred() {
        return mStarred;
    }

    public Uri getPhoto() {
        return mPhoto;
    }

    public Uri getThumbnail() {
        return mThumbnail;
    }

    public List<String> getEmails() {
        return mEmails;
    }

    public List<String> getPhoneNumbers() {
        return mPhoneNumbers;
    }

    public ContactResult(Contact contact){

        Gson g = new Gson();
        Log.d("Test", g.toJson(contact));

        this.mId = contact.getId() + "";
        this.mDisplayName = contact.getDisplayName();
        this.mOrganization = contact.getOrganization();
        this.mStarred = contact.isStarred();
        this.mPhoto = contact.getPhoto();
        this.mThumbnail = contact.getThumbnail();
        this.mEmails.clear(); this.mEmails.addAll(contact.getEmails());
        this.mPhoneNumbers.clear(); this.mPhoneNumbers.addAll(contact.getPhoneNumbers());
    }

    protected ContactResult(Parcel in) {
        this.mId = in.readString();
        this.mDisplayName = in.readString();
        this.mOrganization = in.readString();
        this.mStarred = in.readByte() != 0;
        this.mPhoto = in.readParcelable(Uri.class.getClassLoader());
        this.mThumbnail = in.readParcelable(Uri.class.getClassLoader());
        this.mEmails = in.createStringArrayList();
        this.mPhoneNumbers = in.createStringArrayList();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mId);
        dest.writeString(this.mDisplayName);
        dest.writeString(this.mOrganization);
        dest.writeByte(this.mStarred ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.mPhoto, flags);
        dest.writeParcelable(this.mThumbnail, flags);
        dest.writeStringList(this.mEmails);
        dest.writeStringList(this.mPhoneNumbers);
    }

    @SuppressWarnings("unused")
    public static final Creator<ContactResult> CREATOR = new Creator<ContactResult>() {
        @Override
        public ContactResult createFromParcel(Parcel in) {
            return new ContactResult(in);
        }

        @Override
        public ContactResult[] newArray(int size) {
            return new ContactResult[size];
        }
    };
}
