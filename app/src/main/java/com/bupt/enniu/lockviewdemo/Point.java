package com.bupt.enniu.lockviewdemo;

import android.graphics.RectF;
import android.util.Log;

/**
 * Created by edison on 15/12/11.
 */
public class Point {

    volatile int state; //Point当前的状态.0,未选中;1,选中;-1,错误
    RectF rectF; //圆圈的范围
    int index; //圆环的索引
    int x; //rectF左上角的X轴坐标
    int y; //rectF左上角的Y轴坐标
    int length; //rectF的边长
    int sideLength; //整个正方形锁盘的边长
    int gap; //圆环与圆环之间,水平方向和竖直方向上的间距.默认设为一个圆环的直径,即 gap = length.
    int centerX; //圆心的坐标
    int centerY; //圆心的坐标

    public Point(int index, int sideLength, int paddingLeft, int paddingTop) {
        this.index = index;
        this.sideLength = sideLength;
        initPoint(index, sideLength, paddingLeft, paddingTop);
    }

    private void initPoint(int index, int sideLength, int paddingLeft, int paddingTop) {
        state = 0;

        length = gap = sideLength / 5; //圆与圆之间的间隔为一个圆

        x = (index % 3) * 2 * length + paddingLeft;
        y = (index / 3) * 2 * length + paddingTop;

        rectF = new RectF(x, y, x + length, y + length);

        centerX = x + length / 2;
        centerY = y + length / 2;
    }

    public int getIndex() {
        return index;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getState() {
        return state;
    }

    public void setState(int updatestate) {
        state = updatestate;
    }

    public RectF getRectF() {
        return rectF;
    }

    //判断坐标是否与矩形相交,如果相交,则选中此圆环
    public boolean isIntersected(int x, int y) {
        if (rectF.contains(x, y)) {
            //如果包含坐标
            if (getState() == 0) {
                //且圆环当前处于未选中状态
                setState(1);
                return true;
            } else {
                //如果圆环已经被选中,不可再次被选中
                return false;
            }
        } else {
            return false;
        }
    }

    //重设圆环的状态为未选中
    public void reset() {
        setState(0);
    }
}
