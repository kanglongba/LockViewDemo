package com.bupt.enniu.lockviewdemo;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,LockView.OnUpdateMessageListener,LockView.OnLockPanelListener{
    LockView lockView;
    Button btn_setpassword;
    Button btn_openlock;
    Button btn_resetpassword;
    SharedPreferences sp;
    TextView tv_message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();

    }

    void findViews(){

        btn_openlock = (Button)findViewById(R.id.btn_openlock);
        btn_setpassword = (Button) findViewById(R.id.btn_setpassword);
        btn_resetpassword = (Button) findViewById(R.id.btn_resetpassword);
        btn_openlock.setOnClickListener(this);
        btn_resetpassword.setOnClickListener(this);
        btn_setpassword.setOnClickListener(this);

        tv_message = (TextView)findViewById(R.id.tv_message);

        lockView = (LockView)findViewById(R.id.lockview);
        lockView.setOnUpdateMessageListener(this);
        lockView.setOnLockPanelListener(this);

        sp = getSharedPreferences("lock",MODE_PRIVATE);
    }

    @Override
    public void onClick(View v) {
        if(lockView.getIsPanelLocked()){
            if(System.currentTimeMillis() > lockView.getLockTime()){ //表示已经到了解封的时间
                lockView.setIsPanelLocked(false);
                doAsAction(v);
            }else {
                String locktime = lockView.formatTime(lockView.getLockTime());
                tv_message.setText("请于" + locktime + "后尝试");
            }
        }else {
            doAsAction(v);
        }
    }

    private void doAsAction(View v){
        switch (v.getId()) {
            case R.id.btn_setpassword:
                String pw = sp.getString("password", "-1");
                if ("-1".equalsIgnoreCase(pw)) {
                    tv_message.setText("请输入密码");
                    lockView.setVisibility(View.VISIBLE);
                    lockView.setActionMode(0);
                } else {
                    tv_message.setText("密码已经设置");
                }
                break;
            case R.id.btn_openlock:
                tv_message.setText("请输入设置的密码");
                lockView.setVisibility(View.VISIBLE);
                lockView.setActionMode(1);
                break;
            case R.id.btn_resetpassword:
                tv_message.setText("请输入当前密码");
                lockView.setVisibility(View.VISIBLE);
                lockView.setActionMode(2);
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
        lockView.setIsPanelLocked(true);
        lockView.setLockTime(1);
        lockView.postDelayed(new Runnable() {
            @Override
            public void run() {
                lockView.setVisibility(View.GONE);
            }
        },1000);
    }
}
