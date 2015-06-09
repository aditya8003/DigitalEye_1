package com.example.hduser.accessingimages;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.drawable.PictureDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.speech.tts.TextToSpeech;
// Reference http://stackoverflow.com/questions/11144783/how-to-access-an-image-from-the-phones-photo-gallery



public class MainActivity extends Activity {
    TextToSpeech t1;
    final int TAKE_PICTURE = 1;
    private Uri imageUri;
    TextView output;
    ProgressBar pb;
    Button clickButton;
    public String imagepath = "";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        takePhoto();
        output = (TextView) findViewById(R.id.textView);
        output.setMovementMethod(new ScrollingMovementMethod());

        pb = (ProgressBar) findViewById(R.id.progressBar1);
        pb.setVisibility(View.INVISIBLE);

        clickButton = (Button) findViewById(R.id.action_get_data);
        clickButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                requestData("http://services.hanselandpetal.com/feeds/flowers.xml");
            }
        });

        TextToSpeech.OnInitListener  l1 = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        };
        t1=new TextToSpeech(getApplicationContext(), l1);
    }








    public void takePhoto() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        File photo = new File(Environment.getExternalStorageDirectory(),  "Pic.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);
        startActivityForResult(intent, TAKE_PICTURE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PICTURE:
                if (resultCode == Activity.RESULT_OK) {
                    //Uri imageUri;
                    Uri selectedImage = imageUri;
                    getContentResolver().notifyChange(selectedImage, null);
                    ImageView imageView = (ImageView) findViewById(R.id.IMAGE);
                    ContentResolver cr = getContentResolver();
                    Bitmap bitmap;
                    try {
                        bitmap = android.provider.MediaStore.Images.Media
                                .getBitmap(cr, selectedImage);

                        imageView.setImageBitmap(bitmap);
                        imagepath = selectedImage.toString();
                        Toast.makeText(this, "This file: "+imagepath,
                                Toast.LENGTH_LONG).show();


                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT)
                                .show();
                        Log.e("Camera", e.toString());
                    }
                }
        }
    }

    private void requestData(String uri) {
        MyTask task = new MyTask();
        task.execute(uri);
    }

    protected void updateDisplay(String message) {
        output.append(message + "\n");
    }



// The reason for declaring with the below string arguments is because we ultimately would like to
    // use these string arguments to display in the output
    private class MyTask extends AsyncTask<String, String, String> {



        public byte[] convertBitmapToString(Bitmap src) {

            byte[] byteArray = null;
            if(src!= null){
                ByteArrayOutputStream os=new ByteArrayOutputStream();
                src.compress(android.graphics.Bitmap.CompressFormat.PNG, 100,(OutputStream) os);
                byteArray = os.toByteArray();

            }
            return byteArray;
        }


        @Override
        protected String doInBackground(String... params) {


            String content ="";
            ClarifaiClient clarifai = new ClarifaiClient("WJv2YUXzWFWbaPZxKJgL8U3lZwzwHD732PW1Ph1g", "1OgfBngmtI8cWsBt2lIeeAX8S4MzO97Snn4Q0KOQ");
            String filepath = "/storage/emulated/0/Pic.jpg";


            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeFile(filepath, bmpFactoryOptions);

            int heightRatio = (int)Math.ceil(bmpFactoryOptions.outHeight/(float)200);
            int widthRatio = (int)Math.ceil(bmpFactoryOptions.outWidth/(float)200);

            if (heightRatio > 1 || widthRatio > 1)
            {
                if (heightRatio > widthRatio)
                {
                    bmpFactoryOptions.inSampleSize = heightRatio;
                } else {
                    bmpFactoryOptions.inSampleSize = widthRatio;
                }
            }

            bmpFactoryOptions.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFile(filepath, bmpFactoryOptions);

            byte[] result = convertBitmapToString(bitmap);

            List<RecognitionResult> results =
                    clarifai.recognize(new RecognitionRequest(result));


            File f =  getFilesDir();

            for (Tag tag : results.get(0).getTags()) {
                if (tag.getProbability()>0.98) {
                    content = content + tag.getName() + " ";
                }
            }


            t1.setSpeechRate((float) 0.3);
            t1.speak(content, TextToSpeech.QUEUE_FLUSH, null);

            return content;
        }


        @Override
        protected void onPostExecute(String outvalue) {
            updateDisplay(outvalue);


        }


    }

}