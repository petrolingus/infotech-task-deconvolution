package me.petrolingus.deconvolution;

public class ComputingCore {

    private double[] signal;

    /**
     * Создает сигнал из {n} семплов на основе гауссовских куполов
     * @param n - количество семплов сигнала
     * @param domes - массив гауссовских куполов
     */
    public void createSignal(int n, GaussianDome[] domes) {
        signal = new double[n];
        for (int i = 0; i < n; i++) {
            double yValue = 0;
            for (int j = 0; j < 3; j++) {
                double amplitude = domes[i].amplitude;
                double mean = domes[i].mean;
                double deviation = domes[i].deviation;
                yValue += amplitude * Math.exp(Math.pow(i - mean, 2) / (-2 * Math.pow(deviation, 2)));
            }
            signal[i] = yValue;
        }
    }
}
