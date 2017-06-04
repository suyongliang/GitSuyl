package com.huaxia.hpn.utils;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by dell on 2017/5/21.
 */

public class SocketUtil extends Thread {

    protected Selector selector = null;
    protected SocketChannel client = null;
    protected static final int CONNECT_TIMEOUT = 10000;
    protected static final int READ_TIMEOUT = 10000;
    protected static final int RECONNECT_TIME = 120000;
    protected static final int RECONNECT_TIME_SECOND = RECONNECT_TIME / 1000;

    protected final byte CONNECT = 1;
    protected final byte RUNNING = 2;
    protected byte STATE = CONNECT;
    protected boolean onWork;// 是否工作状态
    static {
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");

    };
    private static String ip = "127.0.0.1";
    private static int port = 9527;
    private String sendMessage = "";
    private ConnectListener connectListener;

    public static enum ENUM_CONNECT {
        STATUS_OK, STATUS_FAIL
    };

    public SocketUtil(String ip, int port, String sendMsg) {
        SocketUtil.ip = ip;
        SocketUtil.port = port;
        sendMessage = sendMsg;
        onWork = true;
    }

    public boolean isReady() {
        return STATE == RUNNING;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        while (onWork) {
            switch (STATE) {
                case CONNECT:
                    connect();
                    writeBuf();
                    break;
                case RUNNING:
                    running();
                    break;
                default:
                    break;
            }
        }
    }

    private synchronized void running() {
        SelectionKey key = null;
        try {
            while (selector.select() > 0) {
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                    byte[] data = readBuf(key);
                    if (data != null){

                    }
//                        MLog.makeText(new String(data));
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            closeKey(key);
        }
    }

    private final byte[] readBuf(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isReadable()) {
            SocketChannel client = (SocketChannel) selectionKey.channel();
            // 如果缓冲区过小的话那么信息流会分成多次接收
            ByteArrayOutputStream bos = (ByteArrayOutputStream) selectionKey
                    .attachment();
            ByteBuffer buffer = ByteBuffer.allocate(10240);// 10kb缓存
            int actual = 0;
            while ((actual = client.read(buffer)) > 0) {
                buffer.flip();
                int limit = buffer.limit();
                byte b[] = new byte[limit];
                buffer.get(b);
                bos.write(b);
                buffer.clear();// 清空
            }
            if (actual < 0) {
                // 出现异常
                selectionKey.cancel();
                client.socket().close();
                client.close();
                throw new EOFException("Read EOF");
            }
            bos.flush();
            byte[] data = bos.toByteArray();
            bos.reset();
            return data;
        }
        return null;
    }

    public final boolean writeBuf(){
        byte[] data = sendMessage.getBytes();
        try{
            if (client.isConnected()) {
                ByteBuffer buffer = ByteBuffer.wrap(data);
                int size = buffer.remaining();
                // 此处需加中途断开逻辑，下次再继续发送数据包
                int actually = client.write(buffer);
                if (actually == size)
                    return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 唤起连接线程重新连接
     */
    protected synchronized void reconnect() {
        notify();
    }

    private synchronized void connect() {
        try {
            selector = Selector.open();
            InetSocketAddress isa = new InetSocketAddress(ip, port);
            client = SocketChannel.open();
            // 设置连超时
            client.socket().connect(isa, CONNECT_TIMEOUT);
            // 设置读超时
            client.socket().setSoTimeout(READ_TIMEOUT);
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ,
                    new ByteArrayOutputStream());
            if (client.isConnected()) {
                // 连接成功开始监听服务端消息
                // 发送一个验证数据包到服务器进行验证
                STATE = RUNNING;
                if (connectListener != null)
                    connectListener.connect(ENUM_CONNECT.STATUS_OK);
                Log.i("", "连接成功");
            } else {
                // 关闭通道过60S重新开始连接
                if (connectListener != null)
                    connectListener.connect(ENUM_CONNECT.STATUS_FAIL);
                StringBuffer buffer = new StringBuffer("服务器连接失败");
                buffer.append(RECONNECT_TIME_SECOND);
                buffer.append("秒后再尝试连接");
//                MLog.makeText(buffer);

                close();// 关闭通道
                Wait(RECONNECT_TIME);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // 有异常关闭通道过60S重新开始连接
            e.printStackTrace();
            StringBuffer buffer = new StringBuffer("连接出错啦！");
            buffer.append(RECONNECT_TIME_SECOND);
            buffer.append("秒后再尝试连接");
//            MLog.makeText(buffer);
            close();// 关闭通道
            Wait(RECONNECT_TIME);
        }
    }

    public void close() {
        STATE = CONNECT;
        try {
            if (client != null) {
                client.socket().close();
                client.close();
                client = null;
            }
            if (selector != null) {
                selector.close();
                selector = null;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void closeKey(SelectionKey key) {
        if (key != null) {
            key.cancel();
            try {
                key.channel().close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                StringBuffer buffer = new StringBuffer("连接断开啦！");
                buffer.append(RECONNECT_TIME_SECOND);
                buffer.append("秒后再尝试连接");
//                MLog.makeText(buffer);
                Wait(RECONNECT_TIME);
            }
        }
        close();
    }

    private void Wait(long millis) {
        try {
            wait(millis);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public ConnectListener getConnectListener() {
        return connectListener;
    }

    public void setConnectListener(ConnectListener connectListener) {
        this.connectListener = connectListener;
    }

    public interface ConnectListener {

        public void connect(ENUM_CONNECT STATUS);
    }
}
