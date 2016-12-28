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
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TcpServerActivity extends Activity implements View.OnClickListener
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

    private static TcpServerThread TcpServerThread;

    private static StringBuffer logBuffer = new StringBuffer();

    private TcpServerActivity.MyHandler handler;

    private static String ip;
    private static String port;
    private static String msg;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcp_server);

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

        handler = new TcpServerActivity.MyHandler(this);
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
        if (null != TcpServerThread && !TcpServerThread.isCanceled)
        {
            Toast.makeText(this, "TCP服务端已经启动，不需要重复启动", Toast.LENGTH_SHORT).show();
            log("TCP服务端已经启动，不需要重复启动");
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

        TcpServerActivity.ip = ip;
        TcpServerActivity.port = tmp;
        TcpServerActivity.msg = msg;

        TcpServerThread = new TcpServerActivity.TcpServerThread(ip, port, msg);
        TcpServerThread.start();

        Toast.makeText(this, "启动TCP服务端", Toast.LENGTH_SHORT).show();
        log("启动TCP服务端");
    }

    private void stopServer()
    {
        if (null == TcpServerThread || TcpServerThread.isCanceled())
        {
            Toast.makeText(this, "TCP服务端已经停止，不需要重复停止", Toast.LENGTH_SHORT).show();
            log("TCP服务端已经停止，不需要重复停止");
            return;
        }

        TcpServerThread.cancel();
        Toast.makeText(this, "停止TCP服务端", Toast.LENGTH_SHORT).show();
        log("停止TCP服务端");
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

    private class TcpServerThread extends Thread
    {
        private static final int BUFFER_SIZE = 1024;

        private String ip;
        private int port;
        private String msg;

        private int seq;
        private boolean isCanceled;

        ServerSocket serverSocket = null;

        public boolean isCanceled()
        {
            return isCanceled;
        }

        public void cancel()
        {
            isCanceled = true;

            if (null != serverSocket)
            {
                try
                {
                    if (!serverSocket.isClosed())
                    {
                        serverSocket.close();
                    }
                }
                catch (Exception e)
                {
                    log("关闭TCP服务端ServerSocket发生异常exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        public TcpServerThread(String ip, int port, String msg)
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
                serverSocket = new ServerSocket(port);
            }
            catch (Exception e)
            {
                isCanceled = true;
                log("创建服务端ServerSocket发生异常exception: " + e.getMessage());
                e.printStackTrace();
            }

            try
            {
                while (true)
                {
                    if (isCanceled)
                    {
                        break;
                    }

                    // serverSocket = new ServerSocket(port);
                    Socket clientSocet = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocet.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocet.getOutputStream());
                    String reqMsg = in.readLine();
                    String clientIp = clientSocet.getRemoteSocketAddress().toString();
                    if (clientIp.startsWith("/"))
                    {
                        clientIp = clientIp.substring(1);
                    }
                    int clientPort = clientSocet.getPort();
                    log("收到来自[" + clientIp /*+ ":" + clientPort*/ + "]的消息：" + reqMsg);

                    String rspMsg = "server_seq=" + (++seq) + " " + msg;
                    out.println(rspMsg);
                    log("响应回给[" + clientIp /*+ ":" + clientPort*/ + "]的消息：" + rspMsg);
                    out.flush();
                    out.close();
                    in.close();
                    clientSocet.close();
                }
            }
            catch (Exception e)
            {
                log("TcpServerThread occurred exception: " + e.getMessage());
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
        private static WeakReference<TcpServerActivity> reference;

        public MyHandler(TcpServerActivity activity)
        {
            reference = new WeakReference<TcpServerActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);

            final TcpServerActivity theActivity = reference.get();
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
