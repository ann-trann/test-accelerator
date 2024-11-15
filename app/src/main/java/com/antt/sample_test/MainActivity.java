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

import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
    private Interpreter tflite; // TensorFlow Lite interpreter

    // Đường dẫn đến file mô hình .tflite
    private static final String MODEL_PATH = "pothole_detection_model.tflite";

    // Biến lưu giá trị trọng lực
    private float[] gravity = new float[3];
    private static final float alpha = 0.8f;

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

        // Khởi tạo sensor manager và accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Load mô hình TFLite
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            e.printStackTrace();
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

    private MappedByteBuffer loadModelFile() throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(getAssets().openFd(MODEL_PATH).getFileDescriptor())) {
            FileChannel fileChannel = fileInputStream.getChannel();
            long startOffset = getAssets().openFd(MODEL_PATH).getStartOffset();
            long declaredLength = getAssets().openFd(MODEL_PATH).getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isCollecting || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        // Áp dụng bộ lọc để lấy giá trị trọng lực
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Tính gia tốc ròng bằng cách loại bỏ trọng lực
        float x = event.values[0] - gravity[0];
        float y = event.values[1] - gravity[1];
        float z = event.values[2] - gravity[2];

        // Đưa dữ liệu vào mô hình để dự đoán
        float[] input = {x, y, z};
        float[][] output = new float[1][1];
        tflite.run(input, output);

        // Kiểm tra xem mô hình dự đoán có phát hiện ổ gà không
        if (output[0][0] > 0.5f) { // ngưỡng xác suất phát hiện ổ gà
            PotholeData pothole = new PotholeData(x, y, z, output[0][0], event.timestamp);
            potholes.add(pothole);
            addPotholeCard(pothole);
            statusText.setText("PHÁT HIỆN Ổ GÀ!");
        } else {
            statusText.setText("Đang theo dõi...");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không cần xử lý
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close(); // Giải phóng TFLite Interpreter
        }
    }

    private void addPotholeCard(PotholeData pothole) {
        CardView card = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.pothole_card, potholeContainer, false);

        TextView potholeText = card.findViewById(R.id.pothole_text);
        potholeText.setText(pothole.getDetails());

        potholeContainer.addView(card, 0);
    }
}
