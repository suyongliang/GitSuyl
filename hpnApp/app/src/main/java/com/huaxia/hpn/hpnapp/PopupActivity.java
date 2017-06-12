package com.huaxia.hpn.hpnapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.huaxia.hpn.utils.AppUtils;
import com.huaxia.hpn.utils.ImageUtils;
import com.huaxia.hpn.utils.IpMacUtils;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.huaxia.hpn.utils.ImageUtils.image2Base64;


public class PopupActivity extends Activity implements OnClickListener {
    private Button btn_take_photo, btn_pick_photo, btn_cancel;
    private LinearLayout layout;
    private Intent intent;
    private String mCurrentPhotoPath;
    static Uri capturedImageUri=null;
    private Bitmap bitmap = null;
    private Bitmap image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);
        intent = getIntent();
        btn_take_photo = (Button) this.findViewById(R.id.btn_take_photo);
        btn_pick_photo = (Button) this.findViewById(R.id.btn_pick_photo);
        btn_cancel = (Button) this.findViewById(R.id.btn_cancel);

        layout = (LinearLayout) findViewById(R.id.pop_layout);

        layout.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                Toast.makeText(getApplicationContext(), "提示：点击空白地方可以关闭",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btn_cancel.setOnClickListener(this);
        btn_pick_photo.setOnClickListener(this);
        btn_take_photo.setOnClickListener(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        finish();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) {
            return;
        }
        if (data != null) {
            if (data.getExtras() != null) {
                bitmap = (Bitmap) data.getExtras().get("data");
//                intent.putExtras(data.getExtras());
//                intent.putExtra("uri", capturedImageUri);
//                intent.putExtra("requestCode", requestCode);
//                intent.putExtra("image", bitmap);
                image = (Bitmap) data.getExtras().get("data");
            }

            if (data.getData() != null) {
//                intent.setData(data.getData());
                Uri mImageCaptureUri = data.getData();
                try {
                    image = (Bitmap) ImageUtils.getBitmapFormUri(this, mImageCaptureUri);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            uploadFile();
        }
//        setResult(requestCode, intent);
//        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_take_photo:
                dispatchTakePictureIntent();
                break;
            case R.id.btn_pick_photo:
                try {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, 2);
                } catch (ActivityNotFoundException e) {
                }
                break;
            case R.id.btn_cancel:
                finish();
                break;
            default:
                break;
        }
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            capturedImageUri = Uri.fromFile(photoFile);
            if (photoFile != null) {
                //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
                startActivityForResult(takePictureIntent, 1);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg",     /* suffix */
                storageDir   /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    /**
     * android上传文件到服务器
     * @return  返回响应的内容
     */
    private void uploadFile(){
        String result = null;
        try {
//            // 图片转换成base64
            String imgBase64 = image2Base64(image);
//            // 得到手机Mac
            String macCode = IpMacUtils.getMacFromWifi();
//            Map<String, Object> params = new HashMap<String, Object>();
//            params.put("macCode", macCode);
//            params.put("photo", imgBase64);
//            params.put("operater", "admin");
//
//            HttpUtils.doPost(RequestURL, imgBase64.toString());
            Properties properties = AppUtils.getProperties(getApplicationContext());
            final String RequestURL=properties.getProperty("photo_uploadUrl");
            Log.i("URL", RequestURL);

            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams params = new RequestParams();
            params.put("macCode", macCode);
            params.put("photo", imgBase64);
            params.put("operater", "app");
            client.setConnectTimeout(100000);
            client.setResponseTimeout(9000000);
            client.setMaxRetriesAndTimeout(2, 1000000);
            client.post(RequestURL, params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                    try {
                        JSONArray imageArray = new JSONArray(response.get("collectionses").toString());
                        if(imageArray.length() > 0){
                            Toast.makeText(getApplicationContext(), "图片导览成功!", Toast.LENGTH_SHORT).show();
                            intent.putExtra("image", response.get("collectionses").toString());
                            intent.putExtra("requestCode", RESULT_OK);
                            setResult(RESULT_OK, intent);
                        } else {
                            Toast.makeText(getApplicationContext(), "没找到关联资源!", Toast.LENGTH_SHORT).show();
                        }
                        PopupActivity.this.finish();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse){
                    Toast.makeText(getApplicationContext(), "没找到关联资源!", Toast.LENGTH_SHORT).show();
                    PopupActivity.this.finish();
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Toast.makeText(getApplicationContext(), "没找到关联资源!", Toast.LENGTH_SHORT).show();
                    PopupActivity.this.finish();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
//        return result;
    }
}
