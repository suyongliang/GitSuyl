package com.huaxia.hpn.hpnapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.huaxia.hpn.headerview.GuidePageAdapter;
import com.huaxia.hpn.utils.NetUtils;

import java.util.ArrayList;
import java.util.List;


public class WelcomeActivity extends AppCompatActivity {

    private ViewPager vp;
    private int []imageIdArray;//图片资源的数组
    private List<View> viewList;//图片资源的集合
    private ViewGroup vg;//放置圆点
    //实例化原点View
    private ImageView iv_point;
    private ImageView []ivPointArray;
    //最后一页的按钮
    private ImageButton ib_start;

    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_welcome);
        ib_start = (ImageButton) findViewById(R.id.guide_ib_start);
        ib_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this,MainActivity.class));
                setTitle("主頁面");
                finish();
            }
        });
        // 判断是否是wifi打开
        if(!NetUtils.isWifi(getApplicationContext())){
            NetUtils.openSetting(WelcomeActivity.this);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }

        //加载ViewPager
        initViewPager();

        //加载底部圆点
        initPoint();

//        ImageButton imageButton = (ImageButton)findViewById(R.id.guide_ib_start); //找到在main.xml中创建的Button
//        //给Buton设置一个listener，这里注意的是setOnClickListener()的参数如果是this，就要在 “public class your_project extends Activity“的后面加上” implements OnClickListener“ ，否则会出错；这里是自己建立的listener所以不用
//        imageButton.setOnClickListener(new Button.OnClickListener(){
//            public void onClick(View v){
//                Intent intent = new Intent();
//                intent.setClass(WelcomeActivity.this,MainActivity.class);// TestActivity 是要跳转到的Activity，需要在src下手动建立TestActivity.java文件
//                startActivity(intent);
//                setTitle("主頁面");
//            }
//        });
    }

    /**
     * 加载底部圆点
     */
    private void initPoint() {

        //这里实例化LinearLayout
        vg = (ViewGroup) findViewById(R.id.guide_ll_point);

        //根据ViewPager的item数量实例化数组
        ivPointArray = new ImageView[viewList.size()];

        //循环新建底部圆点ImageView，将生成的ImageView保存到数组中
        int size = viewList.size();
        for (int i = 0;i<size;i++){
            iv_point = new ImageView(this);
            iv_point.setLayoutParams(new ViewGroup.LayoutParams(20,20));
            iv_point.setPadding(30,0,30,0);//left,top,right,bottom
            ivPointArray[i] = iv_point;
            //第一个页面需要设置为选中状态，这里采用两张不同的图片

            if (i == 0){
                iv_point.setBackgroundResource(R.mipmap.full_holo);
            }else{
                iv_point.setBackgroundResource(R.mipmap.empty_holo);
            }
            //将数组中的ImageView加入到ViewGroup
            vg.addView(ivPointArray[i]);

        }
    }
    /**
     * 加载图片ViewPager
     */
    private void initViewPager() {
        vp = (ViewPager) findViewById(R.id.guide_vp);
        //实例化图片资源
        imageIdArray = new int[]{R.mipmap.guide1,R.mipmap.guide2,R.mipmap.guide3};
        viewList = new ArrayList<>();
        //获取一个Layout参数，设置为全屏
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);
        //循环创建View并加入到集合中
        int len = imageIdArray.length;
        for (int i = 0;i<len;i++){
            //new ImageView并设置全屏和图片资源
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(params);
            imageView.setBackgroundResource(imageIdArray[i]);
            //将ImageView加入到集合中
            viewList.add(imageView);
        }
        //View集合初始化好后，设置Adapter
        vp.setAdapter(new GuidePageAdapter(viewList));
        //设置滑动监听
        vp.setOnPageChangeListener(new ViewPager.OnPageChangeListener(){
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            /**
             * 滑动后的监听
             * @param position
             */
            @Override
            public void onPageSelected(int position) {
                //循环设置当前页的标记图
                int length = imageIdArray.length;
                for (int i = 0;i<length;i++){
                    ivPointArray[position].setBackgroundResource(R.mipmap.full_holo);
                    if (position != i){
                        ivPointArray[i].setBackgroundResource(R.mipmap.empty_holo);
                    }
                }
                //判断是否是最后一页，若是则显示按钮
                if (position == imageIdArray.length - 1){
                    ib_start.setVisibility(View.VISIBLE);
                }else {
                    ib_start.setVisibility(View.GONE);
                }
            }
            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
//                return;
            } else {
                // Permission Denied
                Toast.makeText(WelcomeActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
