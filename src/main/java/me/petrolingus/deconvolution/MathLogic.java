package me.petrolingus.deconvolution;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextField;

import java.util.Arrays;

public class MathLogic extends Service<Void> {

    public AreaChart<Number, Number> signalChart;
    public AreaChart<Number, Number> convolutionChart;

    public TextField vectorLengthField;
    public TextField functionalValueField;
    public TextField deviationField;
    public TextField counterField;

    public double[] impulseData;
    public double[] convolutionData;
    public double[] recoveredData;
    public double[] recoveredConvolutionData;

    public double[] startPoint;
    public double startDeviation;
    public double accuracy;

    public boolean isRunning = false;

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {

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
                Arrays.fill(hi, 1.0);

                int counter = 0;

                int innerCounter = 0;

                while (!isCancelled()) {

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

                    if (hLength < accuracy) {
                        break;
                    }

                    do {
                        for (int i = 0; i < n; i++) {
                            double value = prevPoint[i] + 2 * (currPoint[i] - prevPoint[i]);
                            nextPoint[i] = value;
                            lastPoint[i] = value;
                        }
                        nextDeviation = calculateConvolution(nextPoint);
                        lastDeviation = nextDeviation;

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

                        System.arraycopy(currPoint, 0, prevPoint, 0, n);
                        prevDeviation = currDeviation;
                        if (lastDeviation < currDeviation) {
                            System.arraycopy(lastPoint, 0, currPoint, 0, n);
                            currDeviation = lastDeviation;
                        } else {
                            break;
                        }
                        int finalInnerCounter = innerCounter;
                        Platform.runLater(() -> {
                            counterField.setText(String.valueOf(finalInnerCounter));
                        });
                        innerCounter++;
                    } while (!isCancelled());

                    int finalCounter = counter++;
                    double finalPrevDeviation = prevDeviation;
                    double finalHLength = hLength;
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
                        vectorLengthField.setText(String.valueOf(finalHLength));
                        functionalValueField.setText(String.valueOf(finalPrevDeviation));
                        deviationField.setText(String.valueOf(calculateFoo()));
                        //counterField.setText(String.valueOf(finalCounter));
                    });

                    Thread.yield();
                }

                isRunning = false;

                return null;
            }
        };
    }

    private double calculateFoo() {
        ObservableList<XYChart.Data<Number, Number>> data1 = signalChart.getData().get(0).getData();
        ObservableList<XYChart.Data<Number, Number>> data2 = signalChart.getData().get(1).getData();
        double result = 0;
        for (int i = 0; i < data1.size(); i++) {
            double a1 = data1.get(i).getYValue().doubleValue();
            double a2 = data2.get(i).getYValue().doubleValue();
            result += Math.pow(a1 - a2, 2);
        }
        return result;
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
