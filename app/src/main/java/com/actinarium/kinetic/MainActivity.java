package com.actinarium.kinetic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView mTimestamp;
    private TextView mMovementX;
    private TextView mMovementY;
    private TextView mMovementZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTimestamp = (TextView) findViewById(R.id.timestamp);
        mMovementX = (TextView) findViewById(R.id.movementX);
        mMovementY = (TextView) findViewById(R.id.movementY);
        mMovementZ = (TextView) findViewById(R.id.movementZ);

        KineticSensorModule kineticSensorModule = new KineticSensorModule(this);
        kineticSensorModule.setKineticSensorListener(new KineticSensorModule.KineticSensorListener() {
            @Override
            public void onMovementEvent(long timestamp, float x, float y, float z) {
                mTimestamp.setText(Long.toString(timestamp));
                mMovementX.setText(Float.toString(x));
                mMovementY.setText(Float.toString(y));
                mMovementZ.setText(Float.toString(z));
            }

            @Override
            public void onRotationEvent(long timestamp, float x, float y, float z) {

            }
        });
    }
}
