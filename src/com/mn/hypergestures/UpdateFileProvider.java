package com.mn.hypergestures;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Grants read access to exactly one app-private, verified APK. */
public final class UpdateFileProvider extends ContentProvider {
    static final Uri URI=Uri.parse("content://com.mn.hypergestures.updates/update.apk");
    private void validate(Uri uri)throws FileNotFoundException{if(!URI.equals(uri))throw new FileNotFoundException();}
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri u){return URI.equals(u)?"application/vnd.android.package-archive":null;}
    @Override public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{
        validate(u);if(!"r".equals(mode))throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(UpdateClient.apk(getContext()),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri u,String[] projection,String selection,String[] args,String sort){
        if(!URI.equals(u))return null;
        String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
        MatrixCursor cursor=new MatrixCursor(cols);Object[] row=new Object[cols.length];
        for(int i=0;i<cols.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(cols[i]))row[i]="HyperGesture-update.apk";else if(OpenableColumns.SIZE.equals(cols[i]))row[i]=UpdateClient.apk(getContext()).length();}
        cursor.addRow(row);return cursor;
    }
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}

