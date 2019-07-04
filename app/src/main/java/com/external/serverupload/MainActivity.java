package com.external.serverupload;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Handler.Callback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int SELECT_AUDIO = 2;
    private String selectedPath;
    private Handler handler;
    private TextView tvStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.selectFile).setOnClickListener(this);
        findViewById(R.id.uploadFile).setOnClickListener(this);
        tvStatus = findViewById(R.id.tvStatus);
        handler = new Handler(this);
    }

    public void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image "), SELECT_AUDIO);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (checkPermissionREAD_EXTERNAL_STORAGE(this)) {
            // do your stuff..
            if (resultCode == RESULT_OK) {
                if (requestCode == SELECT_AUDIO) {
                    Uri selectedImageUri = data.getData();
                    selectedImageUri = handleImageUri(selectedImageUri);
                    selectedPath = getRealPathFromURI(selectedImageUri);
                    tvStatus.setText("Selected Path :: " + selectedPath);
                    Log.i(TAG, " Path :: " + selectedPath);
                }
            }
        }

    }

    public static Uri handleImageUri(Uri uri) {
        if (uri.getPath().contains("content")) {
            Pattern pattern = Pattern.compile("(content://media/.*\\d)");
            Matcher matcher = pattern.matcher(uri.getPath());
            if (matcher.find())
                return Uri.parse(matcher.group(1));
            else
                throw new IllegalArgumentException("Cannot handle this URI");
        }
        return uri;
    }

    @SuppressLint("NewApi")
    public String getRealPathFromURI(Uri uri) {
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        String[] column = {MediaStore.Images.Media.DATA};

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.selectFile) {
            openGallery();
        }
        if (v.getId() == R.id.uploadFile) {
            if (null != selectedPath && !selectedPath.isEmpty()) {
                tvStatus.setText("Uploading..." + selectedPath);
                FileUploadUtility.doFileUpload(selectedPath, handler);
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.i("File Upload", "Response :: " + msg.obj);
        String success = 1 == msg.arg1 ? "File Upload Success" : "File Upload Error";
        Log.i(TAG, success);
        tvStatus.setText(success);
        return false;
    }

    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    public boolean checkPermissionREAD_EXTERNAL_STORAGE(
            final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (Activity) context,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showDialog("External storage", context,
                            Manifest.permission.READ_EXTERNAL_STORAGE);

                } else {
                    ActivityCompat
                            .requestPermissions(
                                    (Activity) context,
                                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return false;
            } else {
                return true;
            }

        } else {
            return true;
        }
    }

    public void showDialog(final String msg, final Context context,
                           final String permission) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        alertBuilder.setMessage(msg + " permission is necessary");
        alertBuilder.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[] { permission },
                                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // do your stuff
                } else {
                    Toast.makeText(this, "GET_ACCOUNTS Denied",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }
}


