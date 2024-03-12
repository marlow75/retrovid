package at.fhtw.ai.nn.activation;

import at.fhtw.ai.nn.Neuron;

/**
 * Simple, (binary) step function.<br>
 * <code>f(x) = 0 for x <= 0</code><br>
 * <code>f(x) = 1 for x > 0</code><br><br>
 * Derivative:<br>
 * <code>f'(x) = 0 for x != 0</code><br>
 * <code>f'(x) = ? for x = 0</code>
 * <p>
 * Created On: 24.04.2018
 *
 * @author Daniel Kleebinder
 * @since 0.0.1
 */
public class BinaryStep implements ActivationFunction {
    private static final long serialVersionUID = -3516386212264246948L;

    @Override
    public double activate(Neuron neuron) {
        return neuron.preActivationValue < 0.0 ? 0.0 : 1.0;
    }

    @Override
    public double derivative(Neuron neuron) {
        if (Double.compare(neuron.preActivationValue, 0.0) == 0) {
            throw new ArithmeticException("Unknown derivative for x = 0");
        }
        return 0;
    }
}
