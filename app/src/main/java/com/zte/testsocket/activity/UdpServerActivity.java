package com.zte.testsocket.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.zte.testsocket.R;
import com.zte.testsocket.Util.NetworkTool;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UdpServerActivity extends Activity implements View.OnClickListener
{
    private static final int MSG_LOG_REFRESH    = 1;
    private static final int MSG_LOG_CLEAN      = 2;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss");
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private EditText et_server_ip;
    private Button btn_search;
    private EditText et_server_port;
    private EditText et_response_content;
    private Button btn_start_server;
    private Button btn_stop_server;
    private ScrollView sv_log;
    private TextView tv_display;
    private TextView tv_clean;

    private static UdpServerThread udpServerThread;

    private static StringBuffer logBuffer = new StringBuffer();

    private MyHandler handler;

    private static String ip;
    private static String port;
    private static String msg;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp_server);

        // 初始化组件
        initWidget();
        // 初始化数据
        initData();
    }

    private void initWidget()
    {
        et_server_ip = (EditText) findViewById(R.id.et_server_ip);
        btn_search = (Button) findViewById(R.id.btn_search);
        et_server_port = (EditText) findViewById(R.id.et_server_port);
        et_response_content = (EditText) findViewById(R.id.et_response_content);
        btn_start_server = (Button) findViewById(R.id.btn_start_server);
        btn_stop_server = (Button) findViewById(R.id.btn_stop_server);
        sv_log = (ScrollView) findViewById(R.id.sv_log);
        tv_display = (TextView) findViewById(R.id.tv_display);
        tv_clean = (TextView) findViewById(R.id.tv_clean);

        btn_search.setOnClickListener(this);
        btn_start_server.setOnClickListener(this);
        btn_stop_server.setOnClickListener(this);
        tv_clean.setOnClickListener(this);
    }

    private void initData()
    {
        if (null != ip)
        {
            et_server_ip.setText(ip);
        }
        else
        {
            String tmp = NetworkTool.getCurrIpAddress(this);
            if (!TextUtils.isEmpty(tmp))
            {
                et_server_ip.setText(tmp);
            }
        }
        if (null != port)
        {
            et_server_port.setText(port);
        }
        if (null != msg)
        {
            et_response_content.setText(msg);
        }

        et_server_ip.setSelection(et_server_ip.getText().toString().length());
        tv_display.setText(logBuffer);

        handler = new MyHandler(this);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_search:
            {
                search();
                break;
            }
            case R.id.btn_start_server:
            {
                startServer();
                break;
            }
            case R.id.btn_stop_server:
            {
                stopServer();
                break;
            }
            case R.id.tv_clean:
            {
                cleanLog();
                break;
            }
        }
    }

    private void search()
    {
        String ip = NetworkTool.getCurrIpAddress(this);
        et_server_ip.setText(ip);
        et_server_ip.setSelection(et_server_ip.getText().toString().length());

        Toast.makeText(this, "查询本机IP地址为：" + ip, Toast.LENGTH_SHORT).show();
        log("查询本机IP地址为：" + ip);
    }

    private synchronized void startServer()
    {
        if (null != udpServerThread && !udpServerThread.isCanceled)
        {
            Toast.makeText(this, "UDP服务端已经启动，不需要重复启动", Toast.LENGTH_SHORT).show();
            log("UDP服务端已经启动，不需要重复启动");
            return;
        }

        String ip = et_server_ip.getText().toString().trim();
        et_server_ip.setText(ip);
        et_server_ip.setSelection(et_server_ip.getText().toString().length());
        if (TextUtils.isEmpty(ip))
        {
            Toast.makeText(this, "ip地址不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        String tmp = et_server_port.getText().toString().trim();
        et_server_port.setText(tmp);
        et_server_port.setSelection(et_server_port.getText().toString().length());
        if (TextUtils.isEmpty(tmp))
        {
            Toast.makeText(this, "端口不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        int port = Integer.valueOf(tmp);
        String msg = et_response_content.getText().toString();

        UdpServerActivity.ip = ip;
        UdpServerActivity.port = tmp;
        UdpServerActivity.msg = msg;

        udpServerThread = new UdpServerThread(ip, port, msg);
        udpServerThread.start();

        Toast.makeText(this, "启动UDP服务端", Toast.LENGTH_SHORT).show();
        log("启动UDP服务端");
    }

    private void stopServer()
    {
        if (null == udpServerThread || udpServerThread.isCanceled())
        {
            Toast.makeText(this, "UDP服务端已经停止，不需要重复停止", Toast.LENGTH_SHORT).show();
            log("UDP服务端已经停止，不需要重复停止");
            return;
        }

        udpServerThread.cancel();
        Toast.makeText(this, "停止UDP服务端", Toast.LENGTH_SHORT).show();
        log("停止UDP服务端");
    }

    private synchronized void log(String content)
    {
        if (null == logBuffer)
        {
            logBuffer = new StringBuffer();
        }

        logBuffer.append(sdf.format(new Date())).append(" ").append(content).append(LINE_SEPARATOR);

        if (null != handler)
        {
            handler.removeMessages(MSG_LOG_REFRESH);
            handler.sendEmptyMessage(MSG_LOG_REFRESH);
        }
    }

    private void cleanLog()
    {
        if (null != handler)
        {
            handler.sendEmptyMessage(MSG_LOG_CLEAN);
        }
    }

    private class UdpServerThread extends Thread
    {
        private static final int BUFFER_SIZE = 1024;

        private String ip;
        private int port;
        private String msg;

        private int seq;
        private boolean isCanceled;

        DatagramSocket ds = null;

        public boolean isCanceled()
        {
            return isCanceled;
        }

        public void cancel()
        {
            isCanceled = true;

            if (null != ds)
            {
                try
                {
                    if (ds.isConnected())
                    {
                        ds.disconnect();
                    }
                    if (!ds.isClosed())
                    {
                        ds.close();
                    }
                }
                catch (Exception e)
                {
                    log("关闭UDP服务端DatagramSocket发生异常exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        public UdpServerThread(String ip, int port, String msg)
        {
            this.ip = ip;
            this.port = port;
            this.msg = msg;
        }

        @Override
        public void run()
        {
            try
            {
                InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
                ds = new DatagramSocket(socketAddress);
                byte[] buffer = new byte[BUFFER_SIZE];

                while (true)
                {
                    if (isCanceled)
                    {
                        break;
                    }

                    DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);
                    ds.receive(packet);
                    String srcIp = packet.getAddress().getHostAddress();
                    int srcPort = packet.getPort();
                    String reqMsg = new String(packet.getData(), 0, packet.getLength());
                    log("收到来自[" + srcIp + ":" + srcPort + "]的消息：" + reqMsg);

                    DatagramPacket dp = new DatagramPacket(buffer, BUFFER_SIZE, packet.getAddress(), srcPort);
                    String rspMsg = "server_seq=" + (++seq) + " " + msg;
                    dp.setData(rspMsg.getBytes());
                    ds.send(dp);
                    log("响应回给[" + srcIp + ":" + srcPort + "]的消息：" + rspMsg);
                }
            }
            catch (Exception e)
            {
                log("UdpServerThread occurred exception: " + e.getMessage());
                e.printStackTrace();
            }
            finally
            {
                cancel();
            }
        }
    }

    private static class MyHandler extends Handler
    {
        private static WeakReference<UdpServerActivity> reference;

        public MyHandler(UdpServerActivity activity)
        {
            reference = new WeakReference<UdpServerActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);

            final UdpServerActivity theActivity = reference.get();
            if (null == theActivity)
            {
                return;
            }

            switch (msg.what)
            {
                case MSG_LOG_REFRESH:
                {
                    theActivity.tv_display.setText(theActivity.logBuffer);
                    //theActivity.tv_display.scrollTo(0, theActivity.tv_display.getHeight());
                    // theActivity.sv_log.scrollTo(0, theActivity.sv_log.getHeight());
                    theActivity.sv_log.fullScroll(ScrollView.FOCUS_DOWN);
                    break;
                }
                case MSG_LOG_CLEAN:
                {
                    theActivity.logBuffer = new StringBuffer();
                    theActivity.tv_display.setText("");
                    break;
                }
            }
        }
    }
}
