package com.wise.breaker;

/**
 * Stands in for real time, as provided in the interview. Tests move time forward with the setter.
 */
public class Clock {

    private int timeInMinutes;

    public int getTimeInMinutes() {
        return timeInMinutes;
    }

    public void setTimeInMinutes(int timeInMinutes) {
        this.timeInMinutes = timeInMinutes;
    }
}
