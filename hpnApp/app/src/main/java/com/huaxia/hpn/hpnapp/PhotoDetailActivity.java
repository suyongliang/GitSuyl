package com.huaxia.hpn.hpnapp;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.huaxia.hpn.utils.AppUtils;
import com.huaxia.hpn.utils.ImageUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Properties;

public class PhotoDetailActivity extends AppCompatActivity {
    Bitmap image;
    private Handler uiHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        /*获取Intent中的Bundle对象*/
        Bundle bundle = this.getIntent().getExtras();

        /*获取Bundle中的数据，注意类型和key*/
        String imageList = bundle.getString("image");
        try {
            JSONArray imageArray = new JSONArray(imageList);
            Properties properties = AppUtils.getProperties(getApplicationContext());
            final String defURL=properties.getProperty("defUrl");
            for(int i=0;i<imageArray.length();i++){
                final JSONObject jsonobject = imageArray.getJSONObject(i);
                final String imgurl = defURL + jsonobject.get("pictureUrl").toString();
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            image = ImageUtils.getImage(imgurl);
                            System.out.println("bitmap:" + image);
                            uiHandler.post(new Runnable(){
                                @Override
                                public void run() {
                                    //更新界面
                                    try{
                                        ((ImageView)findViewById(R.id.imageVPhotoDetail)).setImageBitmap(image);
                                        ((TextView)findViewById(R.id.textVPhotoDetail)).setText(jsonobject.getString("name"));
                                        ((TextView)findViewById(R.id.textVPhotoCommentText)).setText(jsonobject.getString("commentText"));
                                        // 播放声音
                                        final MediaPlayer mediaPlayer = new MediaPlayer();
                                        if (mediaPlayer.isPlaying()) {
                                            mediaPlayer.reset();//重置为初始状态
                                        }
                                        mediaPlayer.setDataSource(defURL + jsonobject.get("voiceUrl").toString());
                                        mediaPlayer.prepare();//缓冲
                                        mediaPlayer.start();//开始或恢复播放
                                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {//播出完毕事件
                                            @Override public void onCompletion(MediaPlayer arg0) {
                                                mediaPlayer.release();
                                            }
                                        });
                                        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {//错误处理事件
                                            @Override
                                            public boolean onError(MediaPlayer player, int arg1, int arg2) {
                                                mediaPlayer.release();
                                                return false;
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
            System.out.println("imageArray:"+imageArray);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
