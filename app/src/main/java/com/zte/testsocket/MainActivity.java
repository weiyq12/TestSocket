package com.zte.testsocket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.zte.testsocket.activity.TcpClientActivity;
import com.zte.testsocket.activity.TcpServerActivity;
import com.zte.testsocket.activity.UdpClientActivity;
import com.zte.testsocket.activity.UdpServerActivity;

public class MainActivity extends Activity implements View.OnClickListener
{
    private Button btn_tcp_client;
    private Button btn_tcp_server;
    private Button btn_udp_client;
    private Button btn_udp_server;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化组件
        initWidget();
        // 初始化数据
        initData();
    }

    private void initWidget()
    {
        btn_tcp_client = (Button) findViewById(R.id.btn_tcp_client);
        btn_tcp_server = (Button) findViewById(R.id.btn_tcp_server);
        btn_udp_client = (Button) findViewById(R.id.btn_udp_client);
        btn_udp_server = (Button) findViewById(R.id.btn_udp_server);

        btn_tcp_client.setOnClickListener(this);
        btn_tcp_server.setOnClickListener(this);
        btn_udp_client.setOnClickListener(this);
        btn_udp_server.setOnClickListener(this);
    }

    private void initData()
    {

    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_tcp_client:
            {
                startActivity(new Intent(this, TcpClientActivity.class));
                break;
            }
            case R.id.btn_tcp_server:
            {
                startActivity(new Intent(this, TcpServerActivity.class));
                break;
            }
            case R.id.btn_udp_client:
            {
                startActivity(new Intent(this, UdpClientActivity.class));
                break;
            }
            case R.id.btn_udp_server:
            {
                startActivity(new Intent(this, UdpServerActivity.class));
                break;
            }
        }
    }
}
