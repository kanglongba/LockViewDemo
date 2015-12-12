package com.bupt.enniu.lockviewdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by edison on 15/12/11.
 * 这个View的思想主要是:把判断是否选中的任务交给Point完成,并且Point自动更新自己的状态,然后LockView根据Point的状态,绘制相应的图标和手势轨迹.
 * LockView利用 ActionMode参数和 step参数判断当前的模式和应该进行的操作.
 *
 *   ActionMode    step     status
 *   0              1        设置初始密码,第一次输入
 *   0              2        设置初始密码,第二次输入
 *   1              NA       开锁
 *   2              1        重设密码,判断是否允许
 *   2              2        重设密码,第一次输入新密码
 *   2              3        重设密码,第二次输入新密码
 *
 * 使用方法: 只需要调用 setActionMode(ActionMode),然后LockView可自动进行相关处理.
 *
 * 注意事项:
 *  1.LockView被强制成正方形,高度被强制等于宽度,因此android:layout_width有效,而 android:layout_height无效.
 *
 */
public class LockView extends View {
    Context context;

    Point point0, point1, point2, point3, point4, point5, point6, point7, point8; //九个锁圈
    List<Point> points = new ArrayList<Point>();

    List<Point> pointTrace = new ArrayList<Point>(); //手势轨迹

    Paint paint;
    Bitmap lock_unselected, lock_selected, lock_error_selected;

    int currentX, currentY; //当前手指的坐标

    volatile boolean isReseted = false; //LockView的状态是否已经被重置为全部未选中状态

    String password; //手势密码

    //LockView的模式
    int ActionMode; //0,第一次设置密码;1,解锁;2,重设密码
    //当前操作处于第几步
    int step;

    int password_length_restriction; //密码最小长度为4

    int try_time_restriction; //密码最多允许尝试5次

    public LockView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public LockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    public LockView(Context context) {
        super(context);
    }

    private void init(Context context) {
        this.context = context;

        //主要用于绘制轨迹
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(5);

        //获取到三个bitmap
        lock_selected = BitmapFactory.decodeResource(context.getResources(), R.drawable.lock_selected);
        lock_unselected = BitmapFactory.decodeResource(context.getResources(), R.drawable.lock_unselected);
        lock_error_selected = BitmapFactory.decodeResource(context.getResources(), R.drawable.lock_error_selected);

        //取手势密码,如果没有设置过密码,将取出-1
        password = getPassword();

        //初始化LockView为开锁模式
        ActionMode = 1;
        step = 1;

        password_length_restriction = 4;
        try_time_restriction = 5;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int MeasuredWidth, MeasureHeight;

        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        //把LockView设置成一个正方形.使width = height.
        if (widthSpecMode == MeasureSpec.EXACTLY) {
            MeasuredWidth = widthSpecSize;
            MeasureHeight = MeasuredWidth;
        } else {  //如果没有指定明确的值,就用父View测量的值

            //获取LockView的margin值,后来用不到了.
//            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();

            MeasuredWidth = getMeasuredWidth();
            MeasureHeight = MeasuredWidth;
        }

        //测量好边长后,初始化Point
        initPoints(MeasuredWidth - getPaddingLeft() - getPaddingRight(), getPaddingLeft(), getPaddingRight());

        setMeasuredDimension(MeasuredWidth, MeasureHeight);
    }

    /**
     * 初始化九个Point
     *
     * @param sideLength, 正方形锁盘的边长
     * @param paddingLeft
     * @param paddingTop
     */
    private void initPoints(int sideLength, int paddingLeft, int paddingTop) {
        point0 = new Point(0, sideLength, paddingLeft, paddingTop);
        point1 = new Point(1, sideLength, paddingLeft, paddingTop);
        point2 = new Point(2, sideLength, paddingLeft, paddingTop);
        point3 = new Point(3, sideLength, paddingLeft, paddingTop);
        point4 = new Point(4, sideLength, paddingLeft, paddingTop);
        point5 = new Point(5, sideLength, paddingLeft, paddingTop);
        point6 = new Point(6, sideLength, paddingLeft, paddingTop);
        point7 = new Point(7, sideLength, paddingLeft, paddingTop);
        point8 = new Point(8, sideLength, paddingLeft, paddingTop);

        points.add(point0);
        points.add(point1);
        points.add(point2);
        points.add(point3);
        points.add(point4);
        points.add(point5);
        points.add(point6);
        points.add(point7);
        points.add(point8);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawPoints(canvas);
        drawLines(canvas, currentX, currentY);
    }

    //绘制所有的Point
    private void drawPoints(Canvas canvas) {
        for (Point point : points) {
            if (point.getState() == 1) {
                canvas.drawBitmap(lock_selected, null, point.getRectF(), paint);
            } else if (point.getState() == -1) {
                canvas.drawBitmap(lock_error_selected, null, point.getRectF(), paint);
            } else {
                canvas.drawBitmap(lock_unselected, null, point.getRectF(), paint);
            }
        }
    }

    //绘制轨迹
    private void drawLines(Canvas canvas, int currentX, int currentY) {
        if (pointTrace.size() > 0) {
            Point firstPoint = pointTrace.get(0);

            for (Point point : pointTrace) {
                canvas.drawLine(firstPoint.getCenterX(), firstPoint.getCenterY(), point.getCenterX(), point.getCenterY(), paint);
                firstPoint = point;
            }

            canvas.drawLine(firstPoint.getCenterX(), firstPoint.getCenterY(), currentX, currentY, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //重绘LockView,更新UI
                if (!isReseted) {
                    resetLockView();
                    isReseted = true;
                }

                currentX = (int) event.getX();
                currentY = (int) event.getY();
                //判断当前的坐标是否击中了某个Point
                for (Point point : points) {
                    if (point.isIntersected(currentX, currentY)) {
                        pointTrace.add(point);
                        invalidate();
                        break;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                currentX = (int) event.getX();
                currentY = (int) event.getY();
                for (int i = 0; i < 9; i++) {
                    Point point = points.get(i);
                    if (point.isIntersected(currentX, currentY)) {
                        if (pointTrace.size() > 0) { //判断两点之间连成的直线,是否穿过中间的某个点,如果穿过,那么这个点也要被选中
                            Point middlePoint = checkPointInline(pointTrace.get(pointTrace.size() - 1), point);
                            if (middlePoint != null) {
                                middlePoint.setState(1);
                                pointTrace.add(middlePoint);
                            }
                        }
                        //把当前Point添加到轨迹中
                        pointTrace.add(point);
                        break;
                    }
                }

                if (pointTrace.size() > 0) {
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (pointTrace.size() > 0) {
                    if (ActionMode == 0) {//初设密码
                        initPassword();
                    } else if (ActionMode == 1) { //打开密码锁
                        openLockView();
                    } else if (ActionMode == 2) { //重设密码
                        resetPassword();
                    }
                }
                break;
            default:
                break;
        }
        return true;
    }

    //第一次设置密码
    private void initPassword() {
        String message = "";
        if (step == 1) {
            password = convertToPassword(pointTrace);
            if(password.length() >= password_length_restriction) {
                message = "再次输入密码";
                refreshLockView(100);
                step++;
            }else{
                message = "密码不能小于"+String.valueOf(password_length_restriction)+"位";
                refreshLockView(100);
            }
        } else if (step == 2) {
            if (password.equalsIgnoreCase(convertToPassword(pointTrace))) {
                message = "密码已设置";
                refreshLockView(100);
                setPassword(password);
                setVisibility(View.GONE);
                ActionMode++; //切换到打开模式
                step--;
            } else {
                message = "密码不符,重新设置新密码";
                for (Point point : pointTrace) {
                    point.setState(-1);
                }
                refreshLockView(500);
                step--;
            }
        }
        if(null != onUpdateMessageListener){
            onUpdateMessageListener.onUpdateMessage(message);
        }
    }

    //解锁
    private void openLockView() {
        String message = "";
        if (password.equalsIgnoreCase(convertToPassword(pointTrace))) {
            message = "密码正确";
            refreshLockView(100);
            setVisibility(View.GONE);
            try_time_restriction = 5;
        } else {
            if(try_time_restriction>1) {
                message = "密码错误,还可尝试" + String.valueOf(--try_time_restriction) + "次";
                for (Point point : pointTrace) {
                    point.setState(-1);
                }
                refreshLockView(2000);
            }else{
                if(null != onLockPanelListener) {
                    onLockPanelListener.onLockPanel(); //超过尝试次数,仍然没有输对,锁定密码锁
                }
                message = "密码盘已锁定,请1分钟以后再试";
                for (Point point : pointTrace) {
                    point.setState(-1);
                }
                refreshLockView(1000);
                try_time_restriction = 5;
            }
        }
        if(null != onUpdateMessageListener){
            onUpdateMessageListener.onUpdateMessage(message);
        }
    }

    //重设密码
    private void resetPassword() {
        String message = "";
        if (step == 1) {
            if (password.equalsIgnoreCase(convertToPassword(pointTrace))) {
                message = "密码正确,请输入新密码";
                refreshLockView(100);
                step++;
                try_time_restriction = 5;
            } else {
                if(try_time_restriction>1) {
                    message = "密码错误,还可尝试" + String.valueOf(--try_time_restriction) + "次";
                    for (Point point : pointTrace) {
                        point.setState(-1);
                    }
                    refreshLockView(1000);
                }else{
                    if(null != onLockPanelListener) {
                        onLockPanelListener.onLockPanel(); //超过尝试次数,仍然没有输对,锁定密码锁
                    }
                    message = "密码盘已锁定,请1分钟以后再试";
                    for (Point point : pointTrace) {
                        point.setState(-1);
                    }
                    refreshLockView(1000);
                    try_time_restriction = 5;
                }
            }
        } else if (step == 2) {
            password = convertToPassword(pointTrace);
            if(password.length() >= password_length_restriction) {
                message = "再次输入密码";
                refreshLockView(100);
                step++;
            }else{
                message = "密码不能小于"+String.valueOf(password_length_restriction)+"位";
                refreshLockView(100);
            }
        } else if (step == 3) {
            if (password.equalsIgnoreCase(convertToPassword(pointTrace))) {
                message = "密码已设置";
                refreshLockView(100);
                setPassword(password);
                setVisibility(View.GONE);
                ActionMode--; //切换到打开模式
                step = 1;
            } else {
                message = "密码不符,重新设置新密码";
                for (Point point : pointTrace) {
                    point.setState(-1);
                }
                refreshLockView(500);
                step--;
            }
        }
        if(null != onUpdateMessageListener){
            onUpdateMessageListener.onUpdateMessage(message);
        }
    }

    //根据当前的状态,重绘LockView,更新UI
    private void refreshLockView(int time) {
        currentX = pointTrace.get(pointTrace.size() - 1).getCenterX();
        currentY = pointTrace.get(pointTrace.size() - 1).getCenterY();
        invalidate();
        isReseted = false;

        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isReseted) {
                    resetLockView();
                    isReseted = true;
                }
            }
        }, time);
    }

    //将轨迹转换成字符串密码
    private String convertToPassword(List<Point> pointList) {
        StringBuilder builder = new StringBuilder();
        for (Point point : pointList) {
            builder.append(point.getIndex());
        }
        Log.d("password", builder.toString());
        return builder.toString();
    }

    //重设所有Point的状态为未选中
    private void resetLockView() {
        pointTrace.clear();
        for (Point point : points) {
            point.reset();
        }
        invalidate();
    }

    //检查两点连成的直线,是否穿过了某个中间的点
    private Point checkPointInline(Point beginPoint, Point endPoint) {
        int sum = beginPoint.getIndex() + endPoint.getIndex();
        //如果和不是偶数,直接返回null
        if (sum % 2 == 0) {
            int index = sum / 2;
            //一条横线
            if ((beginPoint.getCenterY() == endPoint.getCenterY()) && (beginPoint.getCenterY()) == points.get(index).getCenterY()) {
                return points.get(index);
            }
            //一条竖线
            if ((beginPoint.getCenterX() == endPoint.getCenterX()) && (beginPoint.getCenterX()) == points.get(index).getCenterX()) {
                return points.get(index);
            }
            //一条斜线
            if (((endPoint.getCenterX() - points.get(index).getCenterX()) == (points.get(index).getCenterX() - beginPoint.getCenterX())) &&
                    ((endPoint.getCenterY() - points.get(index).getCenterY()) == (points.get(index).getCenterY() - beginPoint.getCenterY()))) {
                return points.get(index);
            }
        }
        return null;
    }

    //存储密码到SharedPreferences中
    private void setPassword(String pw) {
        SharedPreferences sp = context.getSharedPreferences("lock", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("password", pw);
        editor.commit();
    }

    //取出存储在SharedPreferences中的密码
    private String getPassword() {
        SharedPreferences sp = context.getSharedPreferences("lock", Context.MODE_PRIVATE);
        String pw = sp.getString("password", "-1");
        return pw;
    }

    /**
     * 设置LockView的操作模式
     *
     * @param ActionMode 0, 代表LockView处于第一次设置密码模式;
     *                   1, 代表LockView处于开锁模式;
     *                   2, 代表LockView处于重设密码模式.
     *                   <p/>
     *                   ActionMode 与 step 合作,一起决定LockView的当前状态
     */
    public void setActionMode(int ActionMode) {
        this.ActionMode = ActionMode;
        this.step = 1;
    }

    /**
     *
     * @return 返回当前的ActionMode
     */
    public int getActionMode(){
        return ActionMode;
    }

    /**
     *
     * @return 返回当前的step
     */
    public int getStep(){
        return step;
    }

    /**
     * 接口,更新提示文案
     */
    public interface OnUpdateMessageListener{
        public abstract void onUpdateMessage(String message);
    }

    OnUpdateMessageListener onUpdateMessageListener;

    public void setOnUpdateMessageListener(OnUpdateMessageListener listener){
        this.onUpdateMessageListener = listener;
    }

    /**
     * 接口,当尝试解锁次数超过限定次数后,锁定密码盘,一分钟内不让尝试.
     */
    public interface OnLockPanelListener{
        public abstract  void onLockPanel();
    }

    OnLockPanelListener onLockPanelListener;

    public void setOnLockPanelListener(OnLockPanelListener listener){
        this.onLockPanelListener = listener;
    }

    /**
     * 接口,更新密码锁的小指示盘.如果没有可以不设置.
     */
    public interface OnUpdateIndicatorListener{
        public abstract void onUpdateIndicator();
    }

    OnUpdateIndicatorListener onUpdateIndicatorListener;

    public void setOnUpdateIndicatorListener(OnUpdateIndicatorListener listener){
        this.onUpdateIndicatorListener = listener;
    }

    boolean isPanelLocked = false;

    /**
     * 判断密码盘是否被锁定
     * @return
     */
    public boolean getIsPanelLocked() {
        return isPanelLocked;
    }

    /**
     * 解锁/锁定密码盘
     * @param isPanelLocked
     */
    public void setIsPanelLocked(boolean isPanelLocked) {
        this.isPanelLocked = isPanelLocked;
    }


    /**
     * 获取密码盘被锁定的时长
     * @return
     */
    public long getLockTime(){
        SharedPreferences sp = context.getSharedPreferences("lock", Context.MODE_PRIVATE);
        long locktime = sp.getLong("locktime",-1);
        return locktime;
    }

    /**
     * 设置密码盘被锁定的时间
     * @param time 单位:分钟
     */
    public void setLockTime(int time){
        long currentTime = System.currentTimeMillis();
        long unbanTime = currentTime + time * 60 * 1000;

        SharedPreferences sp = context.getSharedPreferences("lock", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong("locktime", unbanTime);
        editor.commit();
    }

    /**
     * 把时间转换成人类能看懂的形式
     * @param time
     * @return
     */
    public String formatTime(long time){
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(time);
        String humanTime = dateFormat.format(date);
        return humanTime;
    }
}
