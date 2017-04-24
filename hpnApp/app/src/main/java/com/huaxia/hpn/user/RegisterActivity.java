package com.huaxia.hpn.user;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
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
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.huaxia.hpn.headerview.MyToolBar;
import com.huaxia.hpn.hpnapp.R;
import com.huaxia.hpn.utils.AppUtils;
import com.huaxia.hpn.utils.HttpUtils;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static android.Manifest.permission.READ_CONTACTS;

public class RegisterActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
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
    private RegisterActivity.UserRegisterTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mUserIdView;
    private AutoCompleteTextView mPhoneView;
    private EditText mPasswordView;
    private EditText mConfPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private MyToolBar myToolBar;// 自定义toolbar
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // 初始化视图
        initView();
        // 初始化数据
        initData();
        // 事件监听
        initListener();

        mUserIdView = (AutoCompleteTextView) findViewById(R.id.userId);
        mPhoneView = (AutoCompleteTextView) findViewById(R.id.phone);
        //populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.regPassword);
        mConfPasswordView = (EditText) findViewById(R.id.confirm_password);
        mConfPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.ime_password || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.register_button);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.register_form);
        mProgressView = findViewById(R.id.register_progress);
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
            Snackbar.make(mUserIdView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
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
        mUserIdView.setError(null);
        mPhoneView.setError(null);
        mPasswordView.setError(null);
        mConfPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String userId = mUserIdView.getText().toString();
        String phone = mPhoneView.getText().toString();
        String password = mPasswordView.getText().toString();
        String confPassword = mConfPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(userId)) {
            mUserIdView.setError(getString(R.string.error_field_required));
            focusView = mUserIdView;
            cancel = true;
        }
        if (TextUtils.isEmpty(phone)) {
            mPhoneView.setError(getString(R.string.error_field_required));
            focusView = mPhoneView;
            cancel = true;
        } else if (!isPhoneValid(phone)) {
            mUserIdView.setError(getString(R.string.error_invalid_email));
            focusView = mUserIdView;
            cancel = true;
        }
        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        } else if(TextUtils.isEmpty(password)){
            mPasswordView.setError("密码不能为空");
            focusView = mPasswordView;
            cancel = true;
        }
        if(TextUtils.isEmpty(confPassword)){
            mPasswordView.setError("密码不能为空");
            focusView = mConfPasswordView;
            cancel = true;
        } else if(!password.equals(confPassword)){
            mPasswordView.setError("两次输入密码不一致");
            focusView = mConfPasswordView;
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
            Map<String, String> paramMap = new HashMap<String, String>();
            paramMap.put("userId", userId);
            paramMap.put("phone", phone);
            paramMap.put("password", password);
//            doRegisterTask(paramMap);
            mAuthTask = new RegisterActivity.UserRegisterTask(userId, phone, password);
            mAuthTask.execute((String) null);
        }
    }

    private boolean isPhoneValid(String phone) {
        //TODO: Replace this with your own logic
        return phone.length() == 11;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

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
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), RegisterActivity.ProfileQuery.PROJECTION,

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
            emails.add(cursor.getString(RegisterActivity.ProfileQuery.ADDRESS));
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
                new ArrayAdapter<>(RegisterActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mUserIdView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    private void doRegisterTask(Map params){
        try {
            // Simulate network access.
            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams reParams = new RequestParams();
            reParams.put("id", params.get("userId"));
            reParams.put("phone", params.get("phone"));
            reParams.put("password", params.get("password"));
            reParams.put("operater", "app");

            client.setConnectTimeout(10000);
            Properties properties = AppUtils.getProperties(getApplicationContext());
            final String RequestURL=properties.getProperty("registerUrl");
            Log.i("URL", RequestURL);
            client.post(RequestURL, reParams, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                    try {
                        if("true".equals(response.get("success").toString())){
                            Toast.makeText(getApplicationContext(), "注册成功!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(), response.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse){
                    Toast.makeText(getApplicationContext(), "注册失败!", Toast.LENGTH_SHORT).show();
                    showProgress(false);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Toast.makeText(getApplicationContext(), "注册失败!", Toast.LENGTH_SHORT).show();
                    showProgress(false);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
    * the user.
     */
    public class UserRegisterTask extends AsyncTask<String, Void, Boolean> {

        private final String mUserId;
        private final String mPhone;
        private final String mPassword;
        private String result;

        UserRegisterTask(String userId, String phone, String password) {
            mUserId = userId;
            mPassword = password;
            mPhone = phone;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
//                AsyncHttpClient client = new AsyncHttpClient();
                net.sf.json.JSONObject reParams = new net.sf.json.JSONObject();
                reParams.put("number", mUserId);
                reParams.put("phone", mPhone);
                reParams.put("password", mPassword);
                reParams.put("operater", "app");
//                client.setConnectTimeout(10000);
                Properties properties = AppUtils.getProperties(getApplicationContext());
                final String RequestURL=properties.getProperty("registerUrl");
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
            showProgress(false);

            if (success) {
                Toast.makeText(getApplicationContext(), "注册成功!", Toast.LENGTH_LONG).show();
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
        myToolBar.setRightBtnText("");
        myToolBar.setTvTitle("注册");
    }

    /*
     * 初始化数据
     */
    private void initData() {
        // 设置左边右边的按钮是否显示
        myToolBar.setToolBarBtnVisiable(true, false);
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
                Toast.makeText(RegisterActivity.this, "注册", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void leftBtnClick() {// 左边按钮点击事件
                Toast.makeText(RegisterActivity.this, "返回", Toast.LENGTH_SHORT).show();
                RegisterActivity.this.finish();
            }
        });
    }

}
