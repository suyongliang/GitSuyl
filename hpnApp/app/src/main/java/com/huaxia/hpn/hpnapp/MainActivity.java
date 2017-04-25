package com.huaxia.hpn.hpnapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.huaxia.hpn.headerview.GuidePageAdapter;
import com.huaxia.hpn.user.LoginActivity;
import com.huaxia.hpn.user.MyDetInfoActivity;
import com.huaxia.hpn.utils.AppUtils;
import com.huaxia.hpn.utils.HttpUtils;
import com.huaxia.hpn.utils.ImageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AppCompatActivity";

    // 底部菜单4个Linearlayout
    private LinearLayout ll_home;
    private LinearLayout ll_address;
    private LinearLayout ll_friend;
    private LinearLayout ll_setting;

    // 底部菜单4个ImageView
    private ImageButton iv_home;
    private ImageButton iv_address;
    private ImageButton iv_friend;
    private ImageButton iv_setting;

    // 底部菜单4个菜单标题
    private TextView tv_home;
    private TextView tv_address;
    private TextView tv_friend;
    private TextView tv_setting;
    Bitmap image;
    // 中间内容区域
    private ViewPager viewPager;

    // ViewPager适配器ContentAdapter
    private GuidePageAdapter adapter;
    // 我的按钮
    private TextView tv_login;
    private TextView tv_my_footmark;
    private TextView tv_my_comment;
    private TextView tv_my_info;
    private TextView tv_questionnaire;
    private TextView tv_abountWe;
    private TextView tv_clear_cache;
    private LinearLayout ll_layout_login;

    private List<View> views;

    private Handler uiHandler = new Handler();
    private SharedPreferences sp;
    // 是否登录标志
    private boolean loginFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        //创建属于主线程的handler
        uiHandler=new Handler();
        // 初始化控件
        initView();
        // 初始化底部按钮事件
        initEvent();
    }

    private void initEvent() {
        // 设置按钮监听
        ll_home.setOnClickListener(new ImageButton.OnClickListener(){
            public void onClick(View v){
                onClickButton(v);
            }
        });
        ll_address.setOnClickListener(new ImageButton.OnClickListener(){
            public void onClick(View v){
                onClickButton(v);
            }
        });
        ll_friend.setOnClickListener(new ImageButton.OnClickListener(){
            public void onClick(View v){
                onClickButton(v);
            }
        });
        ll_setting.setOnClickListener(new ImageButton.OnClickListener(){
            public void onClick(View v){
                onClickButton(v);
            }
        });

        //设置ViewPager滑动监听
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrollStateChanged(int arg0) {
                }

                @Override
                public void onPageScrolled(int arg0, float arg1, int arg2) {
                }

                @Override
                public void onPageSelected(final int arg0) {
                    restartBotton();
                    //当前view被选择的时候,改变底部菜单图片，文字颜色
                    switch (arg0) {
                        case 0:
                            iv_home.setImageResource(R.drawable.tab_weixin_pressed);
                            tv_home.setTextColor(0xff1B940A);
                            break;
                        case 1:
                            iv_address.setImageResource(R.drawable.tab_address_pressed);
                            tv_address.setTextColor(0xff1B940A);
                            break;
                        case 2:
                            iv_friend.setImageResource(R.drawable.tab_find_frd_pressed);
                            tv_friend.setTextColor(0xff1B940A);
                            break;
                        case 3:
                            iv_setting.setImageResource(R.drawable.tab_find_frd_pressed);
                            tv_setting.setTextColor(0xff1B940A);
                            break;

                        default:
                            break;
                    }
                }

        });
    }

    private void initView() {
        // 底部菜单4个Linearlayout
        this.ll_home = (LinearLayout) findViewById(R.id.ld_navigation);
        this.ll_address = (LinearLayout) findViewById(R.id.id_guide);
        this.ll_friend = (LinearLayout) findViewById(R.id.id_photo_guide);
        this.ll_setting = (LinearLayout) findViewById(R.id.id_settings);

        // 底部菜单4个ImageView
        this.iv_home = (ImageButton) findViewById(R.id.id_navigation_img);
        this.iv_address = (ImageButton) findViewById(R.id.id_guide_img);
        this.iv_friend = (ImageButton) findViewById(R.id.id_photo_guide_img);
        this.iv_setting = (ImageButton) findViewById(R.id.id_settings_img);

        // 底部菜单4个菜单标题
        this.tv_home = (TextView) findViewById(R.id.tv_navigation);
        this.tv_address = (TextView) findViewById(R.id.tv_guide);
        this.tv_friend = (TextView) findViewById(R.id.tv_photo_guide);
        this.tv_setting = (TextView) findViewById(R.id.tv_settings);

        // 中间内容区域ViewPager
        this.viewPager = (ViewPager) findViewById(R.id.vp_content);

        // 适配器
        View page_01 = View.inflate(MainActivity.this, R.layout.navigation, null);
        View page_02 = View.inflate(MainActivity.this, R.layout.guide, null);
        View page_03 = View.inflate(MainActivity.this, R.layout.photo_guide, null);
        View page_04 = View.inflate(MainActivity.this, R.layout.user_info, null);

        views = new ArrayList<View>();
        views.add(page_01);
        views.add(page_02);
        views.add(page_03);
        views.add(page_04);

        this.adapter = new GuidePageAdapter(views);
        viewPager.setAdapter(adapter);

        // 我的页面按钮注册
        ll_layout_login = (LinearLayout) findViewById(R.id.layout_login);
//        tv_login = (TextView) findViewById(R.id.user_login);
        tv_login = (TextView) page_04.findViewById(R.id.user_login);
        tv_my_footmark= (TextView) findViewById(R.id.my_footmark);
        tv_my_comment= (TextView) findViewById(R.id.my_comment);
        tv_my_info= (TextView) findViewById(R.id.my_info);
        tv_questionnaire =(TextView) findViewById(R.id.questionnaire);
        tv_abountWe = (TextView) findViewById(R.id.abountWe);
        tv_clear_cache = (TextView) findViewById(R.id.clear_cache);

        // 保存的用户名密码
        sp = this.getSharedPreferences("userInfo", Context.MODE_WORLD_READABLE);
        // 自动登录
        autoLogin();
    }

    private void restartBotton() {
        // ImageView置为灰色
        iv_home.setImageResource(R.drawable.tab_weixin_normal);
        iv_address.setImageResource(R.drawable.tab_address_normal);
        iv_friend.setImageResource(R.drawable.tab_find_frd_normal);
        iv_setting.setImageResource(R.drawable.tab_settings_normal);
        // TextView置为灰色
        tv_home.setTextColor(808080);
        tv_address.setTextColor(808080);
        tv_friend.setTextColor(808080);
        tv_setting.setTextColor(808080);
    }

    public void onClickButton(View v) {
        // 在每次点击后将所有的底部按钮(ImageView,TextView)颜色改为灰色，然后根据点击着色
        restartBotton();
        // ImageView和TetxView置为绿色，页面随之跳转
        switch (v.getId()) {
            case R.id.ld_navigation:
                iv_home.setImageResource(R.drawable.tab_weixin_pressed);
                tv_home.setTextColor(0xff1B940A);
                Intent intent = new Intent();
                intent.setClass(MainActivity.this,MapBoxActivity.class);// TestActivity 是要跳转到的Activity，需要在src下手动建立TestActivity.java文件
                startActivity(intent);
                setTitle("导航");
                break;
            case R.id.id_guide:
                iv_address.setImageResource(R.drawable.tab_address_pressed);
                tv_address.setTextColor(0xff1B940A);
                viewPager.setCurrentItem(1);
                setTitle("导览");
                break;
            case R.id.id_photo_guide:
                iv_friend.setImageResource(R.drawable.tab_find_frd_pressed);
                tv_friend.setTextColor(0xff1B940A);
                viewPager.setCurrentItem(2);
                Intent pIntent = new Intent();
                pIntent.setClass(MainActivity.this,PhotoActivity.class);// TestActivity 是要跳转到的Activity，需要在src下手动建立TestActivity.java文件
                startActivityForResult(pIntent, 2);
                setTitle("拍照导览");
                break;
            case R.id.id_settings:
                iv_setting.setImageResource(R.drawable.tab_find_frd_pressed);
                tv_setting.setTextColor(0xff1B940A);
                viewPager.setCurrentItem(3);
                break;
            case R.id.login:
                Intent loginIntent = new Intent();
                loginIntent.setClass(MainActivity.this,LoginActivity.class);// TestActivity 是要跳转到的Activity，需要在src下手动建立TestActivity.java文件
                startActivityForResult(loginIntent, 1);
                setTitle("登录");
                break;
            default:
                break;
        }
    }
    public void tvClickListener(View v){
        switch (v.getId()) {
            case R.id.user_login:
                if(loginFlag){
                    Intent MyInfoIntent = new Intent();
                    MyInfoIntent.setClass(MainActivity.this,MyDetInfoActivity.class);// TestActivity 是要跳转到的Activity，需要在src下手动建立TestActivity.java文件
                    startActivityForResult(MyInfoIntent, 5);
                    setTitle("我的资料");
                } else {
                    Intent loginIntent = new Intent();
                    loginIntent.setClass(MainActivity.this,LoginActivity.class);// TestActivity 是要跳转到的Activity，需要在src下手动建立TestActivity.java文件
                    startActivityForResult(loginIntent, 4);
                    setTitle("登录");
                }
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case 2://拍照导览
                Properties properties = AppUtils.getProperties(getApplicationContext());
                final String defURL=properties.getProperty("defUrl");
                if (resultCode != RESULT_OK) {
                    return;
                }
                try {
                    JSONArray imageArray = new JSONArray((String)data.getExtras().get("image"));
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
                                                ((ImageView)findViewById(R.id.imageView2)).setImageBitmap(image);
                                                ((TextView)findViewById(R.id.textView)).setText(jsonobject.getString("name"));
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
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case 4:// 登录
                String userId = (String)data.getExtras().get("userId");
                tv_login = (TextView) findViewById(R.id.user_login);
                tv_login.setText("欢迎，" + userId);
                break;
            default:
                break;
        }
    }

    // 自动登录
    protected Boolean autoLogin() {
        // TODO: attempt authentication against a network service.
        new Thread(getThread).start();
        return loginFlag;
    }

    private Thread getThread = new Thread(){
        public void run() {
            try {
                // Simulate network access.
                net.sf.json.JSONObject reParams = new net.sf.json.JSONObject();
                reParams.put("number", sp.getString("USER_NAME", ""));
                reParams.put("password", sp.getString("PASSWORD", ""));
                reParams.put("operater", "app");
//                client.setConnectTimeout(10000);
                Properties properties = AppUtils.getProperties(getApplicationContext());
                final String RequestURL=properties.getProperty("loginUrl");
                Log.i("URL", RequestURL);
                String result = HttpUtils.doPost(RequestURL,reParams.toString());
                net.sf.json.JSONObject resultJson = net.sf.json.JSONObject.fromObject(result);
                if(!"true".equals(resultJson.get("success").toString())){
                    loginFlag = false;
                }
            } catch (Exception e) {
                loginFlag = false;
            }
            // TODO: register the new account here.
            loginFlag = true;

            Message msg = Message.obtain();
            msg.what = 0;
            getHandler.sendMessage(msg);
        };
    };

    private Handler getHandler = new Handler(){
        public void handleMessage(Message msg) {
            if(msg.what == 0 && loginFlag){
                Log.i(TAG, "return: " + loginFlag);
                tv_login.setText("欢迎，" + sp.getString("USER_NAME", ""));
            }
        };
    };
}

