package com.vinodpatildev.gallarycameraimagepickerjava;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.Manifest;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.vinodpatildev.gallarycameraimagepickerjava.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String IMAGE_DIRECTORY = "ImagePickerFolder";
    private ActivityMainBinding binding = null;

    private Uri mPickedImageUri = null;

    private ActivityResultLauncher cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result != null && result.getResultCode() == RESULT_OK){
                        Bitmap image = result.getData().getExtras().getParcelable("data");
                        binding.ivPickedImage.setImageBitmap(image);
                        mPickedImageUri = saveImageToInternalStorage(image);
                        Toast.makeText(MainActivity.this,"path :" + mPickedImageUri.getPath().toString(),Toast.LENGTH_LONG).show();

                    }
                }
            }
    );
    private ActivityResultLauncher gallaryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result != null && result.getResultCode() == RESULT_OK){
                        Uri imageUri = result.getData().getData();

                        try {
                            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(),imageUri);
                            binding.ivPickedImage.setImageBitmap(imageBitmap);
                            mPickedImageUri = saveImageToInternalStorage(imageBitmap);
                            Toast.makeText(MainActivity.this,"path :" + mPickedImageUri.getPath().toString(),Toast.LENGTH_LONG).show();

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                choosePhotoFromCamera();
            }
        });
        binding.btnGallary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                choosePhotoFromGallery();
            }
        });
    }
    private void choosePhotoFromCamera() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        } else {
            Dexter.withContext(this)
                    .withPermission(
                            Manifest.permission.CAMERA
                    )
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                            Toast.makeText(MainActivity.this,"Permission granted for CAMERA.", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            cameraLauncher.launch(intent);
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                            Toast.makeText(MainActivity.this,"Permission denied for CAMERA.", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                            String message = "It looks like you have turned off CAMERA permission required for this feature. It can be enabled under the Application Settings.";
                            Toast.makeText(MainActivity.this,message,Toast.LENGTH_LONG).show();
                        }
                    })
                    .check();
        }
    }
    private void choosePhotoFromGallery() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            gallaryLauncher.launch(intent);
        } else {
            Dexter.withContext(this)
                    .withPermissions(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                            if(multiplePermissionsReport != null && multiplePermissionsReport.areAllPermissionsGranted()){
                                String message = "Storage READ/WRITE permissions are granted.";
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                            String message = "It looks like you have turned off permission required for this feature. It can be enabled under the Application Settings.";
                            Toast.makeText(MainActivity.this,message, Toast.LENGTH_LONG).show();
                        }
                    })
                    .check();

        }
    }
    private Uri saveImageToInternalStorage(Bitmap bitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File dir = new File(root +"/Pictures/"+ IMAGE_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
            Toast.makeText(MainActivity.this,"directory created.",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(MainActivity.this,"directory already exists.",Toast.LENGTH_LONG).show();
        }
        String fileName = "picked_image"+UUID.randomUUID()+".jpg";
        File file = new File(dir,fileName);
        if (file.exists()){
            file.delete();
        }
        try {
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress( Bitmap.CompressFormat.JPEG, 90, stream );
            stream.flush();
            stream.close();
        }catch (IOException e ){
            e.printStackTrace();
        }

//        MediaScannerConnection.scanFile(this, new String[] { file.toString() }, new String[]{file.getName()},
//                new MediaScannerConnection.OnScanCompletedListener() {
//                    public void onScanCompleted(String path, Uri uri) {
//                        Log.i("ExternalStorage", "Scanned " + path);
//                        Log.i("ExternalStorage", "uri=" + uri);
//                    }
//                });
        return Uri.parse( file.getAbsolutePath() );
    }

}