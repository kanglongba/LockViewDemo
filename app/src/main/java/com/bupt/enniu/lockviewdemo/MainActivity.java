package com.bupt.enniu.lockviewdemo;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ExpandLockView.OnUpdateMessageListener, ExpandLockView.OnLockPanelListener, ExpandLockView.OnUpdateIndicatorListener {
    ExpandLockView expandLockView;
    IndicatorLockView indicatorLockView;
    Button btn_setpassword;
    Button btn_openlock;
    Button btn_resetpassword;
    Button btn_deletepwd;
    SharedPreferences sp;
    TextView tv_message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();

    }

    void findViews() {

        btn_openlock = (Button) findViewById(R.id.btn_openlock);
        btn_setpassword = (Button) findViewById(R.id.btn_setpassword);
        btn_resetpassword = (Button) findViewById(R.id.btn_resetpassword);
        btn_deletepwd = (Button) findViewById(R.id.btn_deletepassword);
        btn_openlock.setOnClickListener(this);
        btn_resetpassword.setOnClickListener(this);
        btn_setpassword.setOnClickListener(this);
        btn_deletepwd.setOnClickListener(this);

        tv_message = (TextView) findViewById(R.id.tv_message);

        expandLockView = (ExpandLockView) findViewById(R.id.lockview);
        expandLockView.setOnUpdateMessageListener(this);
        expandLockView.setOnLockPanelListener(this);
        expandLockView.setOnUpdateIndicatorListener(this);

        indicatorLockView = (IndicatorLockView) findViewById(R.id.lockview_indicator);

        sp = getSharedPreferences("lock", MODE_PRIVATE);
    }

    @Override
    public void onClick(View v) {
        if (expandLockView.getIsPanelLocked()) {
            if (System.currentTimeMillis() > expandLockView.getLockTime()) { //表示已经到了解封的时间
                expandLockView.setIsPanelLocked(false);
                doAsAction(v);
            } else {
                String locktime = expandLockView.formatTime(expandLockView.getLockTime());
                tv_message.setText("请于" + locktime + "后尝试");
            }
        } else {
            doAsAction(v);
        }
    }

    private void doAsAction(View v) {
        switch (v.getId()) {
            case R.id.btn_setpassword:
                String pw = sp.getString("password", "-1");
                if ("-1".equalsIgnoreCase(pw)) {
                    tv_message.setText("请输入密码");
                    expandLockView.setVisibility(View.VISIBLE);
                    expandLockView.setActionMode(0);
                } else {
                    tv_message.setText("密码已经设置");
                }
                break;
            case R.id.btn_openlock:
                tv_message.setText("请输入设置的密码");
                expandLockView.setVisibility(View.VISIBLE);
                expandLockView.setActionMode(1);
                break;
            case R.id.btn_resetpassword:
                tv_message.setText("请输入当前密码");
                expandLockView.setVisibility(View.VISIBLE);
                expandLockView.setActionMode(2);
                break;
            case R.id.btn_deletepassword:
                expandLockView.setActionMode(0);
                expandLockView.putPassword("-1");
                break;
            default:
                break;
        }
    }

    @Override
    public void onUpdateMessage(String message) {
        tv_message.setText(message);
    }

    @Override
    public void onLockPanel() {
        expandLockView.setIsPanelLocked(true);
        expandLockView.setLockTime(1);
        expandLockView.postDelayed(new Runnable() {
            @Override
            public void run() {
                expandLockView.setVisibility(View.GONE);
            }
        }, 1000);
    }

    @Override
    public void onUpdateIndicator() {
        if (expandLockView.getPointTrace().size() > 0)
            Log.d("onDraw", "run onUpdateIndicator");
        indicatorLockView.setPath(expandLockView.getPointTrace());
    }
}
