package com.huaxia.hpn.user;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.huaxia.hpn.headerview.MyToolBar;
import com.huaxia.hpn.hpnapp.R;
import com.huaxia.hpn.utils.AppUtils;
import com.huaxia.hpn.utils.HttpUtils;
import com.huaxia.hpn.utils.IpMacUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mUserPhoneEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private MyToolBar myToolBar;// 自定义toolbar
    private SharedPreferences sp;
    private CheckBox rem_pw, auto_login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mUserPhoneEmailView = (AutoCompleteTextView) findViewById(R.id.userId_email_phone);
        populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        // Set up the login form.
        // 初始化视图
        initView();
        // 初始化数据
        initData();
        // 事件监听
        initListener();

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mUserPhoneEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUserPhoneEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String userPhoneEmail = mUserPhoneEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(userPhoneEmail)) {
            mUserPhoneEmailView.setError(getString(R.string.error_user_required));
            focusView = mUserPhoneEmailView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_user_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(userPhoneEmail, password);
            mAuthTask.execute((Void) null);
        }
    }

//    private boolean isEmailValid(String email) {
//        //TODO: Replace this with your own logic
//        return email.contains("@");
//    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mUserPhoneEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUserPhoneEmail;
        private final String mPassword;
        private String result;

        UserLoginTask(String userPhoneEmail, String password) {
            mUserPhoneEmail = userPhoneEmail;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                String macCode = IpMacUtils.getMacFromWifi();
                net.sf.json.JSONObject reParams = new net.sf.json.JSONObject();
                reParams.put("number", mUserPhoneEmail);
                reParams.put("password", mPassword);
                reParams.put("macCode", macCode);
                reParams.put("operater", "app");
//                client.setConnectTimeout(10000);
                Properties properties = AppUtils.getProperties(getApplicationContext());
                final String RequestURL=properties.getProperty("loginUrl");
                Log.i("URL", RequestURL);
                result = HttpUtils.doPost(RequestURL,reParams.toString());
                net.sf.json.JSONObject resultJson = net.sf.json.JSONObject.fromObject(result);
                if(!"true".equals(resultJson.get("success").toString())){
                    return false;
                }
            } catch (Exception e) {
                return false;
            }

//            for (String credential : DUMMY_CREDENTIALS) {
//                String[] pieces = credential.split(":");
//                if (pieces[0].equals(mEmail)) {
//                    // Account exists, return true if the password matches.
//                    return pieces[1].equals(mPassword);
//                }
//            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                net.sf.json.JSONObject resultJson = net.sf.json.JSONObject.fromObject(result);
                Toast.makeText(getApplicationContext(), resultJson.get("msg").toString(), Toast.LENGTH_LONG).show();
                //登录成功和记住密码框为选中状态才保存用户信息
                if(rem_pw.isChecked()) {
                    //记住用户名、密码、
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("USER_NAME", mUserPhoneEmail);
                    editor.putString("PASSWORD",mPassword);
                    editor.commit();
                }

                Intent mainIntent = getIntent();
                mainIntent.putExtra("userId", mUserPhoneEmail);
                setResult(RESULT_OK, mainIntent);
                finish();
            } else {
//                mPasswordView.setError(getString(R.string.error_incorrect_password));
                net.sf.json.JSONObject resultJson = net.sf.json.JSONObject.fromObject(result);
                Toast.makeText(getApplicationContext(), resultJson.get("msg").toString(), Toast.LENGTH_LONG).show();
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    /*
     * 初始化视图
     */
    private void initView() {
        myToolBar = (MyToolBar) findViewById(R.id.myToolBar);
        myToolBar.setLeftBtnText("返回");
        myToolBar.setRightBtnText("注册");
        myToolBar.setTvTitle("登录");
        sp = this.getSharedPreferences("userInfo", Context.MODE_WORLD_READABLE);
        rem_pw = (CheckBox) findViewById(R.id.cb_mima);
        auto_login = (CheckBox) findViewById(R.id.cb_auto);

        //判断记住密码多选框的状态
        if(sp.getBoolean("ISCHECK", false)) {
            //设置默认是记录密码状态
            rem_pw.setChecked(true);
            mUserPhoneEmailView.setText(sp.getString("USER_NAME", ""));
            mPasswordView.setText(sp.getString("PASSWORD", ""));
            //判断自动登陆多选框状态
            if(sp.getBoolean("AUTO_ISCHECK", false) && !TextUtils.isEmpty(sp.getString("USER_NAME", ""))
                    && !TextUtils.isEmpty(sp.getString("PASSWORD", ""))) {
                //设置默认是自动登录状态
                auto_login.setChecked(true);
                //跳转界面
//                Intent intent = new Intent(LoginActivity.this,MainActivity.class);
//                LoginActivity.this.startActivity(intent);
            }
        }

        //监听记住密码多选框按钮事件
        rem_pw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (rem_pw.isChecked()) {
                    System.out.println("记住密码已选中");
                    sp.edit().putBoolean("ISCHECK", true).commit();
                }else {
                    System.out.println("记住密码没有选中");
                    sp.edit().putBoolean("ISCHECK", false).commit();
                }
            }
        });

        //监听自动登录多选框事件
        auto_login.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if (auto_login.isChecked()) {
                    System.out.println("自动登录已选中");
                    sp.edit().putBoolean("AUTO_ISCHECK", true).commit();
                } else {
                    System.out.println("自动登录没有选中");
                    sp.edit().putBoolean("AUTO_ISCHECK", false).commit();
                }
            }
        });
    }

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
                Toast.makeText(LoginActivity.this, "注册", Toast.LENGTH_SHORT).show();
                Intent loginIntent = new Intent();
                loginIntent.setClass(LoginActivity.this,RegisterActivity.class);// TestActivity 是要跳转到的Activity，需要在src下手动建立TestActivity.java文件
                startActivityForResult(loginIntent, 1);
            }

            @Override
            public void leftBtnClick() {// 左边按钮点击事件
                Toast.makeText(LoginActivity.this, "返回", Toast.LENGTH_SHORT).show();
                LoginActivity.this.finish();
            }
        });
    }
}

