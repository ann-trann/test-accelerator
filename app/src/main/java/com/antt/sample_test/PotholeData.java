package com.antt.sample_test;

public class PotholeData {
    private final float accelerationX;
    private final float accelerationY;
    private final float accelerationZ;
    private final float totalAcceleration;
    private final long timestamp;

    public PotholeData(float x, float y, float z, float total, long time) {
        this.accelerationX = x;
        this.accelerationY = y;
        this.accelerationZ = z;
        this.totalAcceleration = total;
        this.timestamp = time;
    }

    public String getFormattedTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date(timestamp));
    }

    public String getDetails() {
        return String.format("Thời gian: %s\nGia tốc X: %.2f m/s²\nGia tốc Y: %.2f m/s²\n" +
                        "Gia tốc Z: %.2f m/s²\nGia tốc tổng: %.2f m/s²",
                getFormattedTime(), accelerationX, accelerationY, accelerationZ, totalAcceleration);
    }
}