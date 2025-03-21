package com.saif.jobnet.Activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.canhub.cropper.CropImageView;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityCropImageBinding;

public class CropImageActivity extends AppCompatActivity {

    ActivityCropImageBinding binding;
    private CropImageView cropImageView;

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
}