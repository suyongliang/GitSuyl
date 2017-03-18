package com.huaxia.hpn.hpnapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by hx-suyl on 2017/3/17.
 */

public class PhotoGuideActivity extends AppCompatActivity {

    private static int REQUEST_THUMBNAIL = 1;// 请求缩略图信号标识
    private static int REQUEST_ORIGINAL = 2;// 请求原图信号标识
    private Button button1, button2;
    private ImageView mImageView;
    private String sdPath;//SD卡的路径
    private String picPath;//图片存储路径

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_guide);

        mImageView = (ImageView) findViewById(R.id.imageView1);

        //获取SD卡的路径
        sdPath = Environment.getExternalStorageDirectory().getPath();
        picPath = sdPath + "/" + "tempPhotoGuidePicture";
        Log.e("sdPhotoGuidePath1",sdPath);
    }

    /**
     * 返回应用时回调方法
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_THUMBNAIL) {//对应第一种方法
                /**
                 * 通过这种方法取出的拍摄会默认压缩，因为如果相机的像素比较高拍摄出来的图会比较高清，
                 * 如果图太大会造成内存溢出（OOM），因此此种方法会默认给图片尽心压缩
                 */
                Bundle bundle = data.getExtras();
                Bitmap bitmap = (Bitmap) bundle.get("data");
                mImageView.setImageBitmap(bitmap);
            }else if(requestCode == REQUEST_ORIGINAL){//对应第二种方法
                /**
                 * 这种方法是通过内存卡的路径进行读取图片，所以的到的图片是拍摄的原图
                 */
                FileInputStream fis = null;
                try {
                    Log.e("sdPath2",picPath);
                    //把图片转化为字节流
                    fis = new FileInputStream(picPath);
                    //把流转化图片
                    Bitmap bitmap = BitmapFactory.decodeStream(fis);
                    mImageView.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }finally{
                    try {
                        fis.close();//关闭流
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
