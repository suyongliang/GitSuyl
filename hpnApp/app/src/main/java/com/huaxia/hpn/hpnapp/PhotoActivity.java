package com.huaxia.hpn.hpnapp;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.huaxia.hpn.utils.AppUtils;
import com.huaxia.hpn.utils.ImageUtils;
import com.huaxia.hpn.utils.IpMacUtils;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.huaxia.hpn.utils.ImageUtils.image2Base64;

@SuppressLint("NewApi")
public class PhotoActivity extends AppCompatActivity implements View.OnClickListener {

    private View mView;
    private int mPhotoId;
    private static String srcPath;
    private static final int TIME_OUT = 10*1000;   //超时时间
    private static final String CHARSET = "utf-8"; //设置编码
    private String TAG = "PhotoActivity";
    private Bitmap image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        mView = (View) findViewById(R.id.imageView);
        findViewById(R.id.choose_image).setOnClickListener(this);
        findViewById(R.id.upload_image).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.choose_image:// 选择图片
                Intent popupIntent = new Intent(PhotoActivity.this, PopupActivity.class);
                mPhotoId = R.id.imageView;
                startActivityForResult(popupIntent, 1);
                break;
            case R.id.upload_image: // 上传图片
//                uploadImage();
                submitUploadFile();
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)  {
        super.onActivityResult(requestCode, resultCode, data);

        ImageView photo = (ImageView) mView.findViewById(mPhotoId);
        switch (resultCode) {
            case 1:
                if (data != null) {
                    Uri mImageCaptureUri = (Uri) data.getExtras().get("uri");
                    if (mImageCaptureUri != null) {
//                        Bitmap image;
                        try {
                            //image = MediaStore.Images.Media.getBitmap(this.getActivity().getContentResolver(), mImageCaptureUri);
                            image = (Bitmap) data.getExtras().get("image");
                            Bitmap b = (Bitmap) data.getExtras().get("data");
                            if (image != null) {
                                photo.setImageBitmap(image);
                            }
                            String name = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
                            String fileNmae = Environment.getExternalStorageDirectory().toString()+ File.separator+"dong/image/"+name+".jpg";
                            srcPath = fileNmae;
                            System.out.println(srcPath+"----------保存路径1");
                            File myCaptureFile =new File(fileNmae);
                            try {
                                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                                    if(!myCaptureFile.getParentFile().exists()){
                                        myCaptureFile.getParentFile().mkdirs();
                                    }
                                    BufferedOutputStream bos;
                                    bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
                                    b.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                                    bos.flush();
                                    bos.close();
                                }else{
                                    Toast toast= Toast.makeText(PhotoActivity.this, "保存失败，SD卡无效", Toast.LENGTH_SHORT);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                }
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Bundle extras = data.getExtras();
                        if (extras != null) {
                            image = extras.getParcelable("data");
                            if (image != null) {
                                photo.setImageBitmap(image);
                            }
                        }
                    }
                }
                break;
            case 2:
                if (data != null) {
                    Uri mImageCaptureUri = data.getData();
                    if (mImageCaptureUri != null) {
                        try{
                            image = (Bitmap) ImageUtils.getBitmapFormUri(this, mImageCaptureUri);
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    if (mImageCaptureUri != null) {
                        photo.setImageURI(mImageCaptureUri);
                        ContentResolver cr = this.getContentResolver();
                        Cursor c = cr.query(mImageCaptureUri, null, null, null, null);
                        c.moveToFirst();
                        //这是获取的图片保存在sdcard中的位置
                        srcPath = c.getString(c.getColumnIndex("_data"));
                        System.out.println(srcPath+"----------保存路径2");
                        break;
                    } else {
                        Bundle extras = data.getExtras();
                        if (extras != null) {

                        }
                    }
                }
                break;
            default:
                break;
        }
    }
    private void submitUploadFile(){
        if(srcPath == null || "".equals(srcPath)){
            return;
        }
        final File file=new File(srcPath);
//        ResourceBundle bundle = ResourceBundle.getBundle("system.properties");
        Properties properties = AppUtils.getProperties(getApplicationContext());
        final String RequestURL=properties.getProperty("photo_uploadUrl");
        Log.i("URL", RequestURL);
        if (file == null || (!file.exists())) {
            return;
        }

        Log.i(TAG, "请求的URL=" + RequestURL);
        Log.i(TAG, "请求的fileName=" + file.getName());
        final Map<String, String> params = new HashMap<String, String>();
//        params.put("user_id", loginKey);
        params.put("file_type", "1");
//        params.put("content", img_content.getText().toString());
//        showProgressDialog();
//        new Thread(new Runnable() { //开启线程上传文件
//            @Override
//            public void run() {
//                uploadFile(srcPath, RequestURL,params);
//            }
//        }).start();
        uploadFile(srcPath, RequestURL,params);
    }

    /**
     * android上传文件到服务器
     * @param filePath  需要上传的文件
     * @param RequestURL  请求的rul
     * @return  返回响应的内容
     */
    private String uploadFile(String filePath,String RequestURL,Map<String, String> param){
        String result = null;
        // 显示进度框
//      showProgressDialog();
        try {
//            // 图片转换成base64
            String imgBase64 = image2Base64(image);
//            // 得到手机Mac
            String macCode = IpMacUtils.getMacFromWifi();
//
//            Map<String, Object> params = new HashMap<String, Object>();
//            params.put("macCode", macCode);
//            params.put("photo", imgBase64);
//            params.put("operater", "admin");
//
//            HttpUtils.doPost(RequestURL, imgBase64.toString());
//            File file = new File(filePath); //这里的path就是那个地址的全局变量
//
//            result = UploadUtils.uploadFile(file, RequestURL);
//photo=URLEncoder.encode(photo,"UTF-8");
            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams params = new RequestParams();
            params.put("macCode", macCode);
            params.put("photo", imgBase64);

            client.post(RequestURL, params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                    Toast.makeText(getApplicationContext(), "头像上传成功!", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse){
                    Toast.makeText(getApplicationContext(), "头像上传失败!", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
