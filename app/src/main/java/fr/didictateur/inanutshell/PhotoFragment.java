package fr.didictateur.inanutshell;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PhotoFragment extends Fragment {
    private ImageView imagePreview;
    private Button btnTakePhoto, btnSelectPhoto, btnRemovePhoto;
    private String photoPath = null;
    private String pendingPhotoPath;
    
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_PERMISSIONS = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        
        imagePreview = view.findViewById(R.id.imagePreview);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        btnSelectPhoto = view.findViewById(R.id.btnSelectPhoto);
        btnRemovePhoto = view.findViewById(R.id.btnRemovePhoto);
        
        setupButtons();
        
        // Appliquer les données en attente si elles existent
        if (pendingPhotoPath != null) {
            setPhotoPath(pendingPhotoPath);
            pendingPhotoPath = null;
        }
        
        return view;
    }
    
    private void setupButtons() {
        btnTakePhoto.setOnClickListener(v -> {
            if (checkPermissions()) {
                takePhoto();
            } else {
                requestPermissions();
            }
        });
        
        btnSelectPhoto.setOnClickListener(v -> selectFromGallery());
        
        btnRemovePhoto.setOnClickListener(v -> removePhoto());
    }
    
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestPermissions() {
        ActivityCompat.requestPermissions(getActivity(),
            new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
            REQUEST_PERMISSIONS);
    }
    
    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }
    
    private void selectFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }
    
    private void removePhoto() {
        photoPath = null;
        imagePreview.setImageResource(R.drawable.appicon);
        btnRemovePhoto.setVisibility(View.GONE);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CAMERA:
                    if (data != null && data.getExtras() != null) {
                        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                        savePhotoToInternalStorage(bitmap);
                    }
                    break;
                case REQUEST_GALLERY:
                    if (data != null && data.getData() != null) {
                        Uri selectedImage = data.getData();
                        copyPhotoFromGallery(selectedImage);
                    }
                    break;
            }
        }
    }
    
    private void savePhotoToInternalStorage(Bitmap bitmap) {
        try {
            String filename = "recipe_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getContext().getFilesDir(), filename);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();
            
            photoPath = file.getAbsolutePath();
            displayPhoto();
            
        } catch (IOException e) {
            Toast.makeText(getContext(), "Erreur lors de la sauvegarde de la photo", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void copyPhotoFromGallery(Uri uri) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            String filename = "recipe_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getContext().getFilesDir(), filename);
            FileOutputStream out = new FileOutputStream(file);
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            out.close();
            
            photoPath = file.getAbsolutePath();
            displayPhoto();
            
        } catch (IOException e) {
            Toast.makeText(getContext(), "Erreur lors de l'importation de la photo", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void displayPhoto() {
        if (photoPath != null && new File(photoPath).exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
            imagePreview.setImageBitmap(bitmap);
            btnRemovePhoto.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(getContext(), "Permissions nécessaires pour prendre une photo", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    public String getPhotoPath() {
        return photoPath;
    }
    
    public void setPhotoPath(String path) {
        if (imagePreview != null) {
            this.photoPath = path;
            displayPhoto();
        } else {
            pendingPhotoPath = path;
        }
    }
}
