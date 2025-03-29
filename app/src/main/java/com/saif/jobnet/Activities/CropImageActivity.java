package com.saif.jobnet.Activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static java.lang.String.*;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.canhub.cropper.CropImageView;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityCropImageBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CropImageActivity extends AppCompatActivity {

    ActivityCropImageBinding binding;
    private CropImageView cropImageView;
    private static final double MAX_SIZE_MB = 5.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCropImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cropImageView = findViewById(R.id.cropImageView);
        Uri imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
        if (imageUri != null) {
            binding.cropImageView.setImageUriAsync(imageUri);
        }
        cropImageView.setImageUriAsync(imageUri);

        binding.saveBtn.setOnClickListener(v -> {
            cropImage();
        });

        cropImageView.setOnCropWindowChangedListener(() -> updateImageSize());


        binding.cropImageView.setOnCropWindowChangedListener(new CropImageView.OnSetCropWindowChangeListener() {
            @Override
            public void onCropWindowChanged() {
                binding.imageSizeText.setTextColor(ContextCompat.getColor(CropImageActivity.this, R.color.black));
                binding.imageSizeText.setVisibility(GONE);
            }
        });
        binding.rotateLeft.setOnClickListener(v -> rotateImageLeft());
        binding.rotateRight.setOnClickListener(v -> rotateImageRight());
        binding.cropRectangular.setOnClickListener(v -> cropImageView.setCropShape(CropImageView.CropShape.RECTANGLE));
        binding.resetButton.setOnClickListener(v -> resetImageCropping());
        binding.backBtn.setOnClickListener(v->finish());
    }

    private void resetImageCropping() {
        cropImageView.resetCropRect();
    }

    private void cropImage() {

//        Toast.makeText(this, "cropImage is calling", Toast.LENGTH_SHORT).show();

        cropImageView.setOnCropImageCompleteListener((view, result) -> {
//            Toast.makeText(this, "cropImage completion listener is calling", Toast.LENGTH_SHORT).show();
            if (result.isSuccessful()) {
//                Toast.makeText(this, "result is successful", Toast.LENGTH_SHORT).show();
                Uri croppedImageUri = result.getUriContent();
                if (croppedImageUri != null) {
//                    Toast.makeText(this, "image is cropped and sent", Toast.LENGTH_SHORT).show();
//                    Log.e("cropped image", "img uri : " + croppedImageUri);

//                    long fileSizeInBytes = getFileSize(croppedImageUri);
//                    double fileSizeInMB = fileSizeInBytes / (1024.0 * 1024.0);
//                    binding.imageSizeText.setText(String.format("Size: %.2f MB", fileSizeInMB));
//                    binding.imageSizeText.setVisibility(VISIBLE);

//                    if (fileSizeInMB > 5) {
//                        Toast.makeText(this, "Image size exceeds 5MB limit!", Toast.LENGTH_SHORT).show();
//                        binding.imageSizeText.setTextColor(ContextCompat.getColor(this, R.color.red));
//                        return;
//                    }
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("croppedImageUri", croppedImageUri.toString());
                    resultIntent.setData(croppedImageUri);
                    resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant access
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();

                    finish();
                } else {
                    Toast.makeText(this, "Failed to crop image", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Cropping failed", Toast.LENGTH_SHORT).show();
            }
        });

        // This should start the crop operation and trigger the listener
        cropImageView.croppedImageAsync(
                Bitmap.CompressFormat.PNG,   // You can use other formats like JPEG or WEBP
                90,                          // Quality of the compressed image
                0,                           // Width for resizing, 0 means no resizing
                0,                           // Height for resizing, 0 means no resizing
                CropImageView.RequestSizeOptions.RESIZE_FIT,  // Options for resizing
                null                         // Uri to save the cropped image, null means default
        );
    }

    private void rotateImageLeft() {
        cropImageView.rotateImage(90);
    }
    private void rotateImageRight() {
        cropImageView.rotateImage(-90);
    }

    private void updateImageSize() {
        Bitmap croppedBitmap = cropImageView.getCroppedImage();

        if (croppedBitmap != null) {
            // Convert bitmap to byte array
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            // Calculate image size
            double sizeMB = byteArray.length / (1024.0 * 1024.0); // Convert bytes to MB
            double sizeKB = byteArray.length / 1024.0; // Convert bytes to KB

            // Format size for display
            String formattedSize = sizeMB >= 1 ? String.format("%.2f MB", sizeMB) : String.format("%.2f KB", sizeKB);

            // Update text color based on size limit
            if (sizeMB > 5) {
                binding.imageSizeText.setTextColor(Color.RED);
                binding.saveBtn.setEnabled(false);
            } else {
                binding.imageSizeText.setTextColor(Color.BLUE);
                binding.saveBtn.setEnabled(true);
            }

            // Set size text
            binding.imageSizeText.setText(formattedSize);
        }else {
            Log.e("cropped image", "croppedBitmap is null");
        }
    }

}