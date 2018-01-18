package com.example.vuong.surfaceview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class PreviewActivity extends AppCompatActivity {

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
    }

    @Override
    protected void onResume() {
        super.onResume();

        imageView = (ImageView) findViewById(R.id.imageView);

        Intent intent = getIntent();

        byte[] bytes = intent.getByteArrayExtra("image");

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        imageView.setImageBitmap(bitmap);

    }
}
