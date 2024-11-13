package com.antt.sample_test;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private TextView accelerationText;
    private TextView statusText;
    private TextView detailsText;
    private Button startButton;
    private Button stopButton;
    private LinearLayout potholeContainer;
    private boolean isCollecting = false;
    private List<PotholeData> potholes = new ArrayList<>();

    // Ngưỡng để xác định ổ gà (đơn vị: m/s²)
    private static final float POTHOLE_THRESHOLD = 15.0f;

    // Để lọc nhiễu, chỉ xét các giá trị cách nhau một khoảng thời gian
    private static final long MIN_TIME_BETWEEN_DETECTIONS = 1000; // 1 giây
    private long lastDetectionTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo các view
        accelerationText = findViewById(R.id.acceleration_text);
        statusText = findViewById(R.id.status_text);
        detailsText = findViewById(R.id.details_text);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        potholeContainer = findViewById(R.id.pothole_container);

        // Thiết lập trạng thái ban đầu của các nút
        stopButton.setEnabled(false);

        // Khởi tạo sensor manager và accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Thiết lập sự kiện cho các nút
        startButton.setOnClickListener(v -> startCollecting());
        stopButton.setOnClickListener(v -> stopCollecting());
    }

    private void startCollecting() {
        isCollecting = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusText.setText("Đang theo dõi...");
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void stopCollecting() {
        isCollecting = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusText.setText("Đã dừng theo dõi");
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isCollecting) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isCollecting && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void addPotholeCard(PotholeData pothole) {
        // Tạo card view mới cho ổ gà
        CardView card = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.pothole_card, potholeContainer, false);

        // Thiết lập nội dung cho card
        TextView potholeText = card.findViewById(R.id.pothole_text);
        potholeText.setText(pothole.getDetails());

        // Thêm card vào container
        potholeContainer.addView(card, 0); // Thêm vào đầu danh sách
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isCollecting || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Tính toán độ lớn của gia tốc
        float acceleration = (float) Math.sqrt(x*x + y*y + z*z);
        float netAcceleration = Math.abs(acceleration - 9.81f);

        // Hiển thị giá trị gia tốc chi tiết
        String details = String.format(
                "Chi tiết gia tốc:\n" +
                        "Trục X: %.2f m/s²\n" +
                        "Trục Y: %.2f m/s²\n" +
                        "Trục Z: %.2f m/s²\n" +
                        "Độ lớn gia tốc: %.2f m/s²\n" +
                        "Net Acceleration: %.2f m/s²",
                x, y, z, acceleration, netAcceleration
        );

        accelerationText.setText(String.format("Gia tốc: %.2f m/s²", netAcceleration));
        detailsText.setText(details);

        // Kiểm tra có đi qua ổ gà không
        long currentTime = System.currentTimeMillis();
        if (netAcceleration > POTHOLE_THRESHOLD &&
                (currentTime - lastDetectionTime) > MIN_TIME_BETWEEN_DETECTIONS) {
            // Tạo đối tượng PotholeData mới
            PotholeData pothole = new PotholeData(x, y, z, netAcceleration, currentTime);
            potholes.add(pothole);

            // Thêm card mới
            addPotholeCard(pothole);

            statusText.setText("PHÁT HIỆN Ổ GÀ!");
            lastDetectionTime = currentTime;
        } else if ((currentTime - lastDetectionTime) > 1000) {
            statusText.setText("Đang theo dõi...");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không cần xử lý
    }
}