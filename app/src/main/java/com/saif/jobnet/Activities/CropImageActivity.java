package com.saif.jobnet.Activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static java.lang.String.*;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.canhub.cropper.CropImageView;
import com.saif.jobnet.R;
import com.saif.jobnet.databinding.ActivityCropImageBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CropImageActivity extends AppCompatActivity {

    ActivityCropImageBinding binding;
    private CropImageView cropImageView;
    private static final double MAX_SIZE_MB = 5.0;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCropImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cropImageView = findViewById(R.id.cropImageView);
        Uri imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
        if (imageUri != null) {
            cropImageView.setImageUriAsync(imageUri);
            cropImageView.setOnSetImageUriCompleteListener((view, uri, error) -> {
                if (error == null) {
                    updateCroppedImageSize(cropImageView.getCroppedImage());
                } else {
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                }
            });
        }

        binding.saveBtn.setOnClickListener(v -> {
            cropImage();
        });

        cropImageView.setOnSetCropOverlayReleasedListener(new CropImageView.OnSetCropOverlayReleasedListener() {
            @Override
            public void onCropOverlayReleased(@Nullable Rect rect) {
                // Get cropped bitmap
                Bitmap croppedBitmap = cropImageView.getCroppedImage();
                if (croppedBitmap != null) {
                    updateCroppedImageSize(croppedBitmap);
                } else {
                    Log.e("CropImageActivity", "Cropped bitmap is null");
                }
            }
        });


        binding.rotationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float rotationAngle = progress - 180; // Centering around 0 degrees
                    cropImageView.setRotatedDegrees((int) rotationAngle);
                    binding.rotationAngleText.setText(String.format("Rotation: %.0f°", rotationAngle));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                binding.rotationAngleText.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                binding.rotationAngleText.setVisibility(GONE);
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
            if (result.isSuccessful()) {
                Uri croppedImageUri = result.getUriContent();
                if (croppedImageUri != null) {
                    //Update image size after cropping

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("croppedImageUri", croppedImageUri.toString());
                    Log.d("CropImageActivity", "Cropped image URI sent in intent: " + croppedImageUri);
                    resultIntent.setData(croppedImageUri);
                    Bitmap croppedBitmap = result.getBitmap();
                    updateCroppedImageSize(croppedBitmap);
                    resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    setResult(Activity.RESULT_OK, resultIntent);
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

    private void updateCroppedImageSize(final Bitmap bitmap) {
        executorService.execute(() -> {
            try {
                // Save image to cache
                File file = new File(getCacheDir(), "temp_image.jpg");
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos); // No compression for exact size
                fos.flush();
                fos.close();

                // Get file size in MB
                double fileSizeMB = file.length() / (1024.0 * 1024.0);

                // Switch back to UI thread to update TextView
                new Handler(Looper.getMainLooper()).post(() -> {
                    binding.imageSizeText.setText(String.format("Size: %.2f MB", fileSizeMB));
                    System.out.println("in Cropped Image Activity file size: "+fileSizeMB);

                    // Change text color based on file size
                    if (fileSizeMB > 5.0) {
                        binding.imageSizeText.setTextColor(Color.RED);
                    } else {
                        binding.imageSizeText.setTextColor(Color.BLUE);
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}