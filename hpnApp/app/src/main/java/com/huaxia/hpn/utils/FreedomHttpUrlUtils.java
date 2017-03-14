package com.huaxia.hpn.utils;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * @ClassName: FreedomHttpUrlUtils
 * @author hx-suyl
 * @createddate 2017/3/9
 * @Description: http请求管理者
 */
public class FreedomHttpUrlUtils implements Runnable {

    private Context context;
    /** http访问结果监听器 */
    private FreedomHttpListener listener;
    /** 当前访问线程 */
    private Thread currentRequest = null;
    /** 访问链接 */
    HttpURLConnection conn = null;
    /** 拿到的流 */
    InputStream input = null;
    private static final String ENCODING = "UTF-8";
    public static final int GET_MOTHOD = 1;
    private static final int TIME = 40 * 1000;
    public static final int POST_MOTHOD = 2;
    /**
     * 1： get请求 2： post请求
     */
    private int requestStatus = 1;

    /**
     * <p>
     * Title:
     * </p>
     * <p>
     * Description:构造方法，其实在这里可以传入一个传输协议包，博主是测试代码，所以请求中直接写死了。
     * </p>
     *
     * @param mContext
     * @param listener
     *            监听器
     * @param mRequeststatus
     *            请求方式
     */
    public FreedomHttpUrlUtils(Context mContext, FreedomHttpListener listener,
                               int mRequeststatus) {
        this.context = mContext;
        this.requestStatus = mRequeststatus;
        this.listener = listener;
    }

    /**
     * @Title: postRequest
     * @Description:Post请求触发
     * @throws
     */
    public void postRequest() {
        requestStatus = 2;
        currentRequest = new Thread(this);
        currentRequest.start();
    }

    /**
     * @Title: getRequeest
     * @Description:GET请求触发
     * @throws
     */
    public void getRequeest() {
        requestStatus = 1;
        currentRequest = new Thread(this);
        currentRequest.start();
    }

    /**
     * 对请求的字符串进行编码
     *
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String requestEncodeStr(String requestStr)
            throws UnsupportedEncodingException {
        return URLEncoder.encode(requestStr, ENCODING);
    }

    /**
     * @Title: sendGetRequest
     * @Description: 发送get请求
     * @throws
     */
    private void sendGetRequest() {
        try {

            URL url = new URL(
                    "http://192.168.31.144:10010/MINATest/servlet/DataTestServlet?username=victor&password=strikefreedom");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIME);
            conn.setReadTimeout(TIME);
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                input = conn.getInputStream();
                if (input != null) {
                    listener.action(FreedomHttpListener.EVENT_GET_DATA_SUCCESS,
                            readStream(input));
                }

            } else {
                listener.action(FreedomHttpListener.EVENT_NETWORD_EEEOR, null);
            }
        } catch (SocketException e) {
            e.printStackTrace();
            listener.action(FreedomHttpListener.EVENT_CLOSE_SOCKET, null);
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            listener.action(FreedomHttpListener.EVENT_NETWORD_EEEOR, null);
        } catch (IOException e) {
            e.printStackTrace();
            listener.action(FreedomHttpListener.EVENT_GET_DATA_EEEOR, null);
        } catch (Exception e) {
            e.printStackTrace();
            listener.action(FreedomHttpListener.EVENT_NETWORD_EEEOR, null);
        }
    }

    /**
     * @Title: sendPostRequest
     * @Description: 发送post请求
     * @throws
     */
    private void sendPostRequest() {
        try {
            String data = "username=justice&password=infiniteJustice";
            URL url = new URL(
                    "http://192.168.31.144:10010/MINATest/servlet/DataTestServlet");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIME);
            conn.setReadTimeout(TIME);
            conn.setDoInput(true);// 允许输入
            conn.setDoOutput(true);// 允许输出
            conn.setUseCaches(false);// 不使用Cache
            conn.setRequestProperty("Charset", ENCODING);
            conn.setRequestProperty("Content-Length",
                    String.valueOf(data.length()));
            conn.setRequestProperty("Content-Type", "text/*;charset=utf-8");
            conn.setRequestMethod("POST");
            DataOutputStream outStream = new DataOutputStream(
                    conn.getOutputStream());
            outStream.write(data.getBytes());
            outStream.flush();
            outStream.close();
            if (conn == null) {
                return;
            }
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                input = conn.getInputStream();
                if (input != null) {
                    listener.action(FreedomHttpListener.EVENT_GET_DATA_SUCCESS,
                            readStream(input));
                }
            } else if (responseCode == 404) {
                input = conn.getErrorStream();
                if (input != null) {
                    listener.action(FreedomHttpListener.EVENT_GET_DATA_SUCCESS,
                            readStream(input));
                } else {
                    listener.action(FreedomHttpListener.EVENT_NETWORD_EEEOR,
                            null);
                }
            } else {
                listener.action(FreedomHttpListener.EVENT_NETWORD_EEEOR, null);
            }
        } catch (SocketException e) {
            e.printStackTrace();
            listener.action(FreedomHttpListener.EVENT_CLOSE_SOCKET, null);
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            listener.action(FreedomHttpListener.EVENT_NETWORD_EEEOR, null);
        } catch (IOException e) {
            e.printStackTrace();
            listener.action(FreedomHttpListener.EVENT_GET_DATA_EEEOR, null);
        } catch (Exception e) {
            e.printStackTrace();
            listener.action(FreedomHttpListener.EVENT_NETWORD_EEEOR, null);
        }
    }

    /**
     * @Title: isRunning
     * @Description: 判断是否正在访问
     * @return
     * @throws
     */
    public boolean isRunning() {
        if (currentRequest != null && currentRequest.isAlive()) {
            return true;
        }
        return false;
    }

    /**
     * 读取数据
     *
     * @param inStream
     *            输入流
     * @return
     * @throws Exception
     */
    private Object readStream(InputStream inStream) throws Exception {
        String result;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        result = new String(outStream.toByteArray(), ENCODING);
        outStream.close();
        inStream.close();
        return result;
    }

    /**
     * 取消当前HTTP连接处理
     */
    public void cancelHttpRequest() {
        if (currentRequest != null && currentRequest.isAlive()) {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            input = null;
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            conn = null;
            currentRequest = null;
            System.gc();
        }
    }

    /**
     * 发送请求
     */
    public void run() {
        // 判断是否有网络
        boolean netType = NetUtils.isConnected(context);
        if (netType) {
            if (requestStatus == 1) {
                sendGetRequest();
            } else if (requestStatus == 2) {
                sendPostRequest();
            }
        } else {
            listener.action(FreedomHttpListener.EVENT_NOT_NETWORD, null);
        }
    }

}
