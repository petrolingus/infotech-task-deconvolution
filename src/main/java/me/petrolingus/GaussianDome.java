package me.petrolingus;

import javafx.scene.control.TextField;

public class GaussianDome {

    public final double amplitude;

    public final double mean;

    public final double deviation;

    public GaussianDome(double amplitude, double mean, double deviation) {
        this.amplitude = amplitude;
        this.mean = mean;
        this.deviation = deviation;
    }

    public static void getRandom(TextField amplitude, TextField mean, TextField deviation, int n) {
        amplitude.setText(String.valueOf(Math.floor(n * Math.random() / 2.0)));
        mean.setText(String.valueOf(Math.floor(5 * Math.random() + 2.0)));
        deviation.setText(String.valueOf(Math.floor(n * Math.random())));
    }
}
