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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TcpClientActivity extends Activity implements View.OnClickListener
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

    private static TcpClientActivity.TcpClientThread TcpClientThread;

    private static StringBuffer logBuffer = new StringBuffer();

    private static String ip;
    private static String port;
    private static String msg;
    private static String period;

    private TcpClientActivity.MyHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcp_client);

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

        handler = new TcpClientActivity.MyHandler(this);
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
                    Toast.makeText(this, "启动TCP客户端异常exception: "
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
        if (null != TcpClientThread && !TcpClientThread.isCanceled())
        {
            Toast.makeText(this, "TCP客户端已经启动，不需要重复启动", Toast.LENGTH_SHORT).show();
            log("TCP客户端已经启动，不需要重复启动");
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

        TcpClientActivity.ip = ip;
        TcpClientActivity.port = tmp1;
        TcpClientActivity.msg = msg;
        TcpClientActivity.period = tmp2;

        TcpClientThread = new TcpClientActivity.TcpClientThread(ip, port, msg, period);
        TcpClientThread.start();

        Toast.makeText(this, "启动TCP客户端", Toast.LENGTH_SHORT).show();
        log("启动TCP客户端, 本机IP=" + NetworkTool.getCurrIpAddress(this)
                + ", msg=" + msg + ", period=" + tmp2
                + ", 服务端：ip=" + ip + ", port=" + port );
    }

    private void stopClient()
    {
        if (null == TcpClientThread || TcpClientThread.isCanceled())
        {
            Toast.makeText(this, "TCP客户端已经停止，不需要重复停止", Toast.LENGTH_SHORT).show();
            log("Tcp客户端已经停止，不需要重复停止");
            return;
        }

        TcpClientThread.cancel();
        Toast.makeText(this, "停止TCP客户端", Toast.LENGTH_SHORT).show();
        log("停止TCP客户端");
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

    private class TcpClientThread extends Thread
    {
        private static final int BUFFER_SIZE = 1024;

        private String ip;
        private int port;
        private String msg;
        private long period;

        private int seq;
        private boolean isCanceled;

        private Socket client = null;

        public void cancel()
        {
            isCanceled = true;

            if (null != client)
            {
                try
                {
                    if (!client.isClosed())
                    {
                        client.close();
                    }
                }
                catch (Exception e)
                {
                    log("关闭Tcp客户端DatagramSocket发生异常exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        public boolean isCanceled()
        {
            return isCanceled;
        }

        public TcpClientThread(String ip, int port, String msg, long peroid)
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
                while (true)
                {
                    if (isCanceled)
                    {
                        break;
                    }

                    String reqMsg = "client_seq=" + (++seq) + " " + msg;
                    client = new Socket(ip, port);
                    Writer out = new OutputStreamWriter(client.getOutputStream());
                    out.write(reqMsg);
                    out.flush();
                    client.shutdownOutput();
                    log("向[" + ip + ":" + port + "]发送的TCP消息：" + reqMsg);

                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String rspMsg;
                    while(null != (rspMsg = in.readLine()))
                    {
                        log("由[" + ip + ":" + port + "]响应的TCP消息：" + rspMsg);
                        break;
                    }

                    in.close();
                    out.close();
                    client.close();

                    if (period <= 0)
                    {
                        break;
                    }

                    Thread.sleep(period);
                }
            }
            catch (Exception e)
            {
                log("TcpClientThread occurred exception: " + e.getMessage());
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
        private static WeakReference<TcpClientActivity> reference;

        public MyHandler(TcpClientActivity activity)
        {
            reference = new WeakReference<TcpClientActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);

            final TcpClientActivity theActivity = reference.get();
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
