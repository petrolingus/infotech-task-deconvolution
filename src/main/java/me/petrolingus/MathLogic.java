package me.petrolingus;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public class MathLogic extends Service<Void> {

    public AreaChart<Number, Number> signalChart;
    public AreaChart<Number, Number> convolutionChart;

    public Button deconvolutionButton;
    public TextField functionalValueField;
    public TextField counterField;

    public double[] impulseData;
    public double[] convolutionData;
    public double[] recoveredData;
    public double[] recoveredConvolutionData;

    public double[] startPoint;
    public double startDeviation;

    boolean isDebug = false;

    public boolean isRunning = false;

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {

                System.out.println("NEW TASK WAS CREATED");

                int n = startPoint.length;

                double[] prevPoint = new double[n];
                System.arraycopy(startPoint, 0, prevPoint, 0, n);
                double prevDeviation;

                double[] currPoint = new double[n];
                System.arraycopy(startPoint, 0, currPoint, 0, n);
                double currDeviation = startDeviation;

                double[] nextPoint = new double[n];
                System.arraycopy(startPoint, 0, nextPoint, 0, n);
                double nextDeviation;

                double[] lastPoint = new double[n];
                System.arraycopy(startPoint, 0, lastPoint, 0, n);
                double lastDeviation;

                double[] hi = new double[n];
                for (int i = 0; i < n; i++) {
                    hi[i] = 1.0;
                }

                int counter = 0;

                System.out.println("1 dec:" + startDeviation);

                while (!isCancelled()) {

                    counter++;

                    if (isDebug) {
                        System.out.println("ITERATION " + counter);
                    }

                    for (int i = 0; i < n; i++) {
                        double temp = currDeviation;
                        currPoint[i] = prevPoint[i] + hi[i];
                        currDeviation = calculateConvolution(currPoint);
                        if (currDeviation >= temp) {
                            currPoint[i] = prevPoint[i] - hi[i];
                            currDeviation = calculateConvolution(currPoint);
                            if (currDeviation >= temp) {
                                currPoint[i] = prevPoint[i];
                                currDeviation = temp;
                                hi[i] /= 10.0;
                            }
                        }
                    }

                    double hLength = 0;
                    for (int i = 0; i < n; i++) {
                        hLength += Math.pow(hi[i], 2);
                    }

                    if (hLength < 1.0E-10) {
                        break;
                    }

                    if (isDebug) {
                        System.out.println("2 dec:" + currDeviation);
                    }

                    do {

                        for (int i = 0; i < n; i++) {
                            double value = prevPoint[i] + 2 * (currPoint[i] - prevPoint[i]);
                            nextPoint[i] = value;
                            lastPoint[i] = value;
                        }
                        nextDeviation = calculateConvolution(nextPoint);
                        lastDeviation = nextDeviation;

                        if (isDebug) {
                            System.out.println("3 dec:" + nextDeviation);
                        }

                        for (int i = 0; i < n; i++) {
                            double temp = lastDeviation;
                            lastPoint[i] = nextPoint[i] + hi[i];
                            lastDeviation = calculateConvolution(lastPoint);
                            if (lastDeviation >= temp) {
                                lastPoint[i] = nextPoint[i] - hi[i];
                                lastDeviation = calculateConvolution(lastPoint);
                                if (lastDeviation >= temp) {
                                    lastPoint[i] = nextPoint[i];
                                    lastDeviation = temp;
                                }
                            }
                        }

                        if (isDebug) {
                            System.out.println("4 dec:" + lastDeviation);
                        }

                        System.arraycopy(currPoint, 0, prevPoint, 0, n);
                        prevDeviation = currDeviation;
                        if (lastDeviation < currDeviation) {
                            System.arraycopy(lastPoint, 0, currPoint, 0, n);
                            currDeviation = lastDeviation;
                        } else {
                            break;
                        }

                    } while (true);

                    int finalCounter = counter;
                    double finalPrevDeviation = prevDeviation;
                    Platform.runLater(() -> {
                        XYChart.Series<Number, Number> recoveredSignal = new XYChart.Series<>();
                        for (int i = 0; i < n; i++) {
                            double value = recoveredData[i];
                            recoveredSignal.getData().add(new XYChart.Data<>(i, value));
                        }
                        XYChart.Series<Number, Number> recoveredConvolutionSignal = new XYChart.Series<>();
                        for (int i = 0; i < n; i++) {
                            double value = recoveredConvolutionData[i];
                            recoveredConvolutionSignal.getData().add(new XYChart.Data<>(i, value));
                        }
                        signalChart.getData().set(1, recoveredSignal);
                        convolutionChart.getData().set(1, recoveredConvolutionSignal);
                        functionalValueField.setText(String.valueOf(finalPrevDeviation));
                        counterField.setText(String.valueOf(finalCounter));
                    });

                    Thread.yield();
                }

                isRunning = false;

                Platform.runLater(() -> {
                    ImageView imageView = new ImageView("icons/play.png");
                    imageView.setViewport(new Rectangle2D(1, 1, 24, 24));
                    deconvolutionButton.setGraphic(imageView);
                });

                return null;
            }
        };
    }

    private double calculateConvolution(double[] point) {

        int n = point.length;

        for (int i = 0; i < n; i++) {
            double value = -1.0;
            for (int j = 0; j < n; j++) {
                value -= point[j] * impulseData[(n - j + i) % n];
            }
            recoveredData[i] = Math.exp(value);
        }

        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < n; j++) {
                sum += recoveredData[j] * impulseData[(n - j + i) % n];
            }
            recoveredConvolutionData[i] = sum;
        }

        double result = 0;
        for (int i = 0; i < n; i++) {
            double a = recoveredConvolutionData[i];
            double b = convolutionData[i];
            result += (a - b) * (a - b);
        }

        return result;
    }
}
