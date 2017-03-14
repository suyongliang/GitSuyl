package com.huaxia.hpn.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

/**
 * @ClassName: BaseHandler
 * @author hx-suyl
 * @createddate 2017/3/9
 * @Description: 消息处理器
 */
public class BaseHandler extends Handler {
    private Context context;
    /** 事件回调接口处理 */
    private FreedomDataCallBack callBack;

    public BaseHandler(Context context, FreedomDataCallBack callBack) {
        this.context = context;
        this.callBack = callBack;
    }

    public void handleMessage(Message msg) {
        // 根据不同的结果触发不同的动作
        if (msg.what == FreedomHttpListener.EVENT_GET_DATA_SUCCESS) {
            if (msg.obj == null) {
                callBack.onFailed();

            } else {
                // 后台处理数据
                callBack.processData(msg.obj, true);
            }
        } else if (msg.what == FreedomHttpListener.EVENT_NOT_NETWORD) {
            callBack.onFailed();
            // CommonUtil.showInfoDialog(context,
            // getString(R.string.net_error));
        } else if (msg.what == FreedomHttpListener.EVENT_NETWORD_EEEOR) {
            callBack.onFailed();

        } else if (msg.what == FreedomHttpListener.EVENT_GET_DATA_EEEOR) {
            callBack.onFailed();

        } else if (msg.what == FreedomHttpListener.EVENT_CLOSE_SOCKET) {

        }
        callBack.onFinish();
    }
}
