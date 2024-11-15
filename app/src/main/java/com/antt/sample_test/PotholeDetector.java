package com.antt.sample_test;


import org.tensorflow.lite.Interpreter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class PotholeDetector {

    private Interpreter tflite;

    public PotholeDetector(AssetManager assetManager, String modelPath) throws IOException {
        tflite = new Interpreter(loadModelFile(assetManager, modelPath));
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float classify(float[] accelerometerData) {
        float[][] output = new float[1][1];
        tflite.run(accelerometerData, output);
        return output[0][0];
    }
}


