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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogRecord;

public class UdpClientActivity extends Activity implements View.OnClickListener
{
    private static final int MSG_LOG_REFRESH    = 1;
    private static final int MSG_LOG_CLEAN      = 2;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss");
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private EditText et_server_ip;
    private EditText et_server_port;
    private EditText et_period;
    private EditText et_request_content;
    private Button btn_start_client;
    private Button btn_stop_client;
    private ScrollView sv_log;
    private TextView tv_display;
    private TextView tv_clean;

    private static UdpClientThread udpClientThread;

    private static StringBuffer logBuffer = new StringBuffer();

    private static String ip;
    private static String port;
    private static String msg;
    private static String period;

    private MyHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp_client);

        // 初始化组件
        initWidget();
        // 初始化数据
        initData();
    }

    private void initWidget()
    {
        et_server_ip = (EditText) findViewById(R.id.et_server_ip);
        et_server_port = (EditText) findViewById(R.id.et_server_port);
        et_period = (EditText) findViewById(R.id.et_period);
        et_request_content = (EditText) findViewById(R.id.et_request_content);
        btn_start_client = (Button) findViewById(R.id.btn_start_client);
        btn_stop_client = (Button) findViewById(R.id.btn_stop_client);
        sv_log = (ScrollView) findViewById(R.id.sv_log);
        tv_display = (TextView) findViewById(R.id.tv_display);
        tv_clean = (TextView) findViewById(R.id.tv_clean);

        btn_start_client.setOnClickListener(this);
        btn_stop_client.setOnClickListener(this);
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
            et_request_content.setText(msg);
        }
        if (null != period)
        {
            et_period.setText(period);
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
            case R.id.btn_start_client:
            {
                try
                {
                    startClient();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Toast.makeText(this, "启动UDP客户端异常exception: "
                            + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.btn_stop_client:
            {
                stopClient();
                break;
            }
            case R.id.tv_clean:
            {
                cleanLog();
                break;
            }
        }
    }

    private synchronized void startClient()
    {
        if (null != udpClientThread && !udpClientThread.isCanceled())
        {
            Toast.makeText(this, "UDP客户端已经启动，不需要重复启动", Toast.LENGTH_SHORT).show();
            log("UDP客户端已经启动，不需要重复启动");
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
        String tmp1 = et_server_port.getText().toString().trim();
        et_server_port.setText(tmp1);
        et_server_port.setSelection(et_server_port.getText().toString().length());
        if (TextUtils.isEmpty(tmp1))
        {
            Toast.makeText(this, "端口不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        int port = Integer.valueOf(tmp1);
        String msg = et_request_content.getText().toString();
        String tmp2 = et_period.getText().toString().trim();
        et_period.setText(tmp2);
        et_period.setSelection(et_period.getText().toString().length());
        if (TextUtils.isEmpty(tmp2))
        {
            Toast.makeText(this, "间隔时间不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        long period = 1000 * Long.valueOf(tmp2);

        UdpClientActivity.ip = ip;
        UdpClientActivity.port = tmp1;
        UdpClientActivity.msg = msg;
        UdpClientActivity.period = tmp2;

        udpClientThread = new UdpClientThread(ip, port, msg, period);
        udpClientThread.start();

        Toast.makeText(this, "启动UDP客户端", Toast.LENGTH_SHORT).show();
        log("启动UDP客户端, 本机IP=" + NetworkTool.getCurrIpAddress(this)
                + ", msg=" + msg + ", period=" + tmp2
                + ", 服务端：ip=" + ip + ", port=" + port );
    }

    private void stopClient()
    {
        if (null == udpClientThread || udpClientThread.isCanceled())
        {
            Toast.makeText(this, "UDP客户端已经停止，不需要重复停止", Toast.LENGTH_SHORT).show();
            log("UDP客户端已经停止，不需要重复停止");
            return;
        }

        udpClientThread.cancel();
        Toast.makeText(this, "停止UDP客户端", Toast.LENGTH_SHORT).show();
        log("停止UDP客户端");
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

    private class UdpClientThread extends Thread
    {
        private static final int BUFFER_SIZE = 1024;

        private String ip;
        private int port;
        private String msg;
        private long period;

        private int seq;
        private boolean isCanceled;

        private DatagramSocket ds = null;

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
                    log("关闭UDP客户端DatagramSocket发生异常exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        public boolean isCanceled()
        {
            return isCanceled;
        }

        public UdpClientThread(String ip, int port, String msg, long peroid)
        {
            this.ip = ip;
            this.port = port;
            this.msg = msg;
            this.period = peroid;
        }

        @Override
        public void run()
        {
            try
            {
                ds = new DatagramSocket();
                byte[] buffer = new byte[BUFFER_SIZE];

                while (true)
                {
                    if (isCanceled)
                    {
                        break;
                    }

                    String reqMsg = "client_seq=" + (++seq) + " " + msg;
                    DatagramPacket dp = new DatagramPacket(reqMsg.getBytes()
                            , reqMsg.getBytes().length
                            , InetAddress.getByName(ip)
                            , port);
                    ds.send(dp);
                    log("向[" + ip + "/" + port + "]发送的UDP消息：" + reqMsg);

                    dp = new DatagramPacket(buffer, BUFFER_SIZE);
                    ds.receive(dp);
                    String rspMsg = new String(dp.getData(), 0, dp.getLength());
                    log("由[" + ip + "/" + port + "]响应的UDP消息：" + rspMsg);

                    Thread.sleep(period);
                }
            }
            catch (Exception e)
            {
                log("UdpClientThread occurred exception: " + e.getMessage());
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
        private static WeakReference<UdpClientActivity> reference;

        public MyHandler(UdpClientActivity activity)
        {
            reference = new WeakReference<UdpClientActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);

            final UdpClientActivity theActivity = reference.get();
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
