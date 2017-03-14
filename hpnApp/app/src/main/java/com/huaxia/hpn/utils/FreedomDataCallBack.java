package com.huaxia.hpn.utils;

/**
 * @ClassName: FreedomDataCallBack
 * @author suyl
 * @createddate 2017-3-9
 * @Description: 回调接口，处理返回数据
 * @param <T>
 */
public interface FreedomDataCallBack<T> {

    public abstract void onStart();

    public abstract void processData(T paramObject, boolean paramBoolean);

    public abstract void onFinish();

    public abstract void onFailed();
}