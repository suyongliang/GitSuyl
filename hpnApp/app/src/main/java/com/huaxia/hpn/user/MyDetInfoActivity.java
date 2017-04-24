package com.huaxia.hpn.user;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.huaxia.hpn.headerview.MyToolBar;
import com.huaxia.hpn.hpnapp.R;
import com.huaxia.hpn.utils.AppUtils;
import com.huaxia.hpn.utils.HttpUtils;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static com.huaxia.hpn.hpnapp.R.id.birthdayTextView;

public class MyDetInfoActivity extends AppCompatActivity {
    private static final String TAG = "MyDetInfoActivity";

    private MyToolBar myToolBar;// 自定义toolbar
    private SharedPreferences sp;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLogoutTask mAuthTask = null;

    // 用户详细信息定义
    private EditText etAccount;// 用户名
    private EditText etUserName;// 姓名
    private EditText etPassword; // 密码
    private EditText etPhone; // 手机号
    private EditText etEmail; // 邮箱
    private RadioButton sexRadioMail; // 性别-男
    private RadioButton sexRadioFemale; // 性别-男
    private TextView tvBirthday; // 生日
    private EditText etOccupation; // 职业

    //用户详细信息Map
    private Map<String, String> userUpdateMap;

    // 是否登录标志
    private boolean queryFlag = false;

    private net.sf.json.JSONObject userInfo;

    //获取日期格式器对象
    DateFormat fmtDateAndTime = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //定义一个TextView控件对象
    TextView dateAndTimeLabel = null;
    //获取一个日历对象
    Calendar dateAndTime = Calendar.getInstance(Locale.CHINA);

    //当点击DatePickerDialog控件的设置按钮时，调用该方法
    DatePickerDialog.OnDateSetListener d = new DatePickerDialog.OnDateSetListener()
    {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            //修改日历控件的年，月，日
            //这里的year,monthOfYear,dayOfMonth的值与DatePickerDialog控件设置的最新值一致
            dateAndTime.set(Calendar.YEAR, year);
            dateAndTime.set(Calendar.MONTH, monthOfYear);
            dateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            //将页面TextView的显示更新为最新时间
            updateLabel();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_det_info);
        sp = this.getSharedPreferences("userInfo", Context.MODE_WORLD_READABLE);

        // 初始化视图
        initView();
        // 初始化数据
        initData();
        // 事件监听
        initListener();

        //得到页面设定日期的按钮控件对象
        Button dateBtn = (Button)findViewById(R.id.setDate);
        //设置按钮的点击事件监听器
        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //生成一个DatePickerDialog对象，并显示。显示的DatePickerDialog控件可以选择年月日，并设置
                new DatePickerDialog(MyDetInfoActivity.this,
                        d,
                        dateAndTime.get(Calendar.YEAR),
                        dateAndTime.get(Calendar.MONTH),
                        dateAndTime.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        dateAndTimeLabel=(TextView)findViewById(birthdayTextView);
        updateLabel();

        Button mSignOutButton = (Button) findViewById(R.id.logout_button);
        mSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogOut();
            }
        });

    }

    //更新页面出生年月TextView的方法
    private void updateLabel() {
        dateAndTimeLabel.setText(fmtDateAndTime.format(dateAndTime.getTime()));
    }

    /*
     * 初始化视图
     */
    private void initView() {
        myToolBar = (MyToolBar) findViewById(R.id.myToolBar);
        myToolBar.setLeftBtnText("返回");
        myToolBar.setRightBtnText("保存");
        myToolBar.setTvTitle("我的资料");

        // 根据用户名查询用户详细信息并显示。
        etAccount = (EditText)findViewById(R.id.accountEditText);// 用户名
        etUserName = (EditText)findViewById(R.id.userNameEditText);// 姓名
        etPassword = (EditText)findViewById(R.id.pwdEdittext); // 密码
        etPhone = (EditText)findViewById(R.id.phoneEditText); // 手机号
        etEmail = (EditText) findViewById(R.id.emailEditText); // 邮箱
        sexRadioMail = (RadioButton) findViewById(R.id.radioMale); // 性别
        sexRadioFemale = (RadioButton) findViewById(R.id.radioFemale); // 性别
        tvBirthday = (TextView)findViewById(R.id.birthdayTextView); // 生日
        etOccupation = (EditText) findViewById(R.id.occupationEditText); // 职业

        new Thread(getThread).start();
    }

    private Thread getThread = new Thread(){
        public void run() {
            net.sf.json.JSONObject resultJson;
            try {
                // Simulate network access.
                net.sf.json.JSONObject reParams = new net.sf.json.JSONObject();
                reParams.put("number", sp.getString("USER_NAME", ""));
//                client.setConnectTimeout(10000);
                Properties properties = AppUtils.getProperties(getApplicationContext());
                final String RequestURL=properties.getProperty("queryUserUrl");
                Log.i("URL", RequestURL);
                String result = HttpUtils.doPost(RequestURL,reParams.toString());
                resultJson = net.sf.json.JSONObject.fromObject(result);
                if(!"true".equals(resultJson.get("success").toString())){
                    queryFlag = false;
                } else {
                    // TODO: set user info here.
                    userInfo =  net.sf.json.JSONObject.fromObject(resultJson.get("obj"));
                    queryFlag = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                queryFlag = false;
            }

            Message msg = Message.obtain();
            msg.what = 0;
            getHandler.sendMessage(msg);
        };
    };

    private Handler getHandler = new Handler(){
        public void handleMessage(Message msg) {
            if(msg.what == 0 && queryFlag){
                try{
                    Log.i(TAG, "return: " + queryFlag);
                    etAccount.setText(userInfo.get("number").toString());// 用户名
                    etUserName.setText((String)userInfo.get("name")); // 姓名
                    etPassword.setText("******"); // 密码
                    etPhone.setText((String)userInfo.get("phoneNumber")); // 手机号
                    etEmail.setText((String)userInfo.get("email")); // 邮箱
                    // 性别
                    if("0".equals(userInfo.get("sex"))){
                        sexRadioMail.setChecked(true);
                    } else if ("1".equals(userInfo.get("sex"))){
                        sexRadioFemale.setChecked(true);
                    }
                    if(userInfo.get("birthday") == null){
                        tvBirthday.setText(fmtDateAndTime.format(new Date()));
                    } else {
                        tvBirthday.setText(fmtDateAndTime.format(formatter.parse(userInfo.get("birthday").toString()))); // 生日
                    }
                    etOccupation.setText((String)userInfo.get("occupation")); // 职业
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
    };

    /*
     * 初始化数据
     */
    private void initData() {
        // 设置左边右边的按钮是否显示
        myToolBar.setToolBarBtnVisiable(true, true);
        // 设置是否显示中间标题，默认的是显示
        myToolBar.setToolBarTitleVisible(true);
    }

    /*
     * 事件监听
     */
    private void initListener() {
        /*
         * toolbar的点击事件处理
         */
        myToolBar.setOnMyToolBarClickListener(new MyToolBar.MyToolBarClickListener() {

            @Override
            public void rightBtnClick() {// 右边按钮点击事件
                Toast.makeText(MyDetInfoActivity.this, "保存", Toast.LENGTH_SHORT).show();
                userUpdateMap = new HashMap<String, String>();
                try{
                    // 用户名
                    userUpdateMap.put("number", ObjectToString(etAccount.getText()));
                    // 姓名
                    userUpdateMap.put("name", URLEncoder.encode(ObjectToString(etUserName.getText()), "UTF-8"));
                    // 手机号
                    userUpdateMap.put("phoneNumber", ObjectToString(etPhone.getText()));
                    // 邮箱
                    userUpdateMap.put("email", ObjectToString(etEmail.getText()));
                    // 性别
                    if(sexRadioMail.isChecked()){
                        userUpdateMap.put("sex", "0");
                    } else if (sexRadioFemale.isChecked()){
                        userUpdateMap.put("sex", "1");
                    }
                    // 生日
                    userUpdateMap.put("birthday", ObjectToString(tvBirthday.getText()));
                    // 职业
                    userUpdateMap.put("occupation", ObjectToString(etOccupation.getText()));
                    userUpdateMap.put("operater", "app");
                }catch (Exception e){
                    e.printStackTrace();
                }

                // 更新用户详细信息
                new Thread(updateThread).start();

            }

            @Override
            public void leftBtnClick() {// 左边按钮点击事件
                Toast.makeText(MyDetInfoActivity.this, "返回", Toast.LENGTH_SHORT).show();
                MyDetInfoActivity.this.finish();
            }
        });
    }

    // logout
    private void attemptLogOut(){
        if (mAuthTask != null) {
            return;
        }
        // 调用后台注销接口
        String userPhoneEmail = sp.getString("USER_NAME", "");

        mAuthTask = new UserLogoutTask(userPhoneEmail);
        mAuthTask.execute((Void) null);
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLogoutTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUserPhoneEmail;
        private String result;

        UserLogoutTask(String userPhoneEmail) {
            mUserPhoneEmail = userPhoneEmail;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                net.sf.json.JSONObject reParams = new net.sf.json.JSONObject();

                reParams.put("number", mUserPhoneEmail);
//                reParams.put("password", mPassword);
                reParams.put("operater", "app");
//                client.setConnectTimeout(10000);
                Properties properties = AppUtils.getProperties(getApplicationContext());
                final String RequestURL=properties.getProperty("logoutUrl");
                Log.i("URL", RequestURL);
                result = HttpUtils.doPost(RequestURL,reParams.toString());
                net.sf.json.JSONObject resultJson = net.sf.json.JSONObject.fromObject(result);
                if(!"true".equals(resultJson.get("success").toString())){
                    return false;
                }
            } catch (Exception e) {
                return false;
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
                net.sf.json.JSONObject resultJson = net.sf.json.JSONObject.fromObject(result);
                Toast.makeText(getApplicationContext(), resultJson.get("msg").toString(), Toast.LENGTH_LONG).show();

                //注销成功后清空用户名、密码
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("USER_NAME", "");
                editor.putString("PASSWORD","");
                editor.commit();

                Intent mainIntent = getIntent();
                mainIntent.putExtra("userId", mUserPhoneEmail);
                setResult(RESULT_OK, mainIntent);
                finish();
            } else {
                net.sf.json.JSONObject resultJson = net.sf.json.JSONObject.fromObject(result);
                Toast.makeText(getApplicationContext(), resultJson.get("msg").toString(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }
    }

    // 更新客户信息flag
    private boolean updateFlag;
    private Thread updateThread = new Thread(){
        public void run() {
            net.sf.json.JSONObject resultJson;
            try {
                // Simulate network access.
                net.sf.json.JSONObject reParams = new net.sf.json.JSONObject();
                reParams = net.sf.json.JSONObject.fromObject(userUpdateMap);
                Properties properties = AppUtils.getProperties(getApplicationContext());
                final String RequestURL=properties.getProperty("updateUserUrl");
                Log.i("URL", RequestURL);
                String result = HttpUtils.doPost(RequestURL,reParams.toString());
                resultJson = net.sf.json.JSONObject.fromObject(result);
                if(!"true".equals(resultJson.get("success").toString())){
                    updateFlag = false;
                } else {
                    // TODO: set user info here.
                    userInfo =  net.sf.json.JSONObject.fromObject(resultJson.get("obj"));
                    updateFlag = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                updateFlag = false;
            }

            Message msg = Message.obtain();
            msg.what = 0;
            updateHandler.sendMessage(msg);
        };
    };

    private Handler updateHandler = new Handler(){
        public void handleMessage(Message msg) {
            if(msg.what == 0 && updateFlag){
                Log.i(TAG, "updateUserInfo: " + queryFlag);
                Toast.makeText(MyDetInfoActivity.this, "保存成功！", Toast.LENGTH_SHORT).show();
            }
        };
    };

    private String ObjectToString(Object obj){
        if (obj == null){
            return "";
        }
        return obj.toString();
    }
}
