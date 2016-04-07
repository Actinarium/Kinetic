package com.actinarium.kinetic.pipeline;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Generates Java code out of the provided data array
 *
 * @author Paul Danyliuk
 */
public class CodeGenerator {

    /**
     * Template for a resulting table lookup interpolator. The parameters are: <ol> <li>Package name</li> <li>Class
     * name</li> <li>Float values, delimited with a comma, optionally split into rows of 6</li> </ol>
     */
    private static final String TABLE_LOOKUP_TEMPLATE = "package %1$s;"
            + "\n"
            + "\nimport android.view.animation.Interpolator;"
            + "\n"
            + "\n/**"
            + "\n * <p>Natural motion interpolator that uses lookup table sampled at regular intervals"
            + "\n * and interpolates linearly between lookup table values.</p>"
            + "\n *"
            + "\n * <p>Generated with <a href=\"https://github.com/Actinarium/Kinetic\">Kinetic</a>."
            + "\n * Derives from Apache 2.0 licensed code from Android Support v4 Library, specifically"
            + "\n * {@link android.support.v4.view.animation.LookupTableInterpolator LookupTableInterpolator}</p>"
            + "\n */"
            + "\npublic class %2$s implements Interpolator {"
            + "\n"
            + "\n    /**"
            + "\n     * Lookup table values sampled with x at regular intervals between 0 and 1"
            + "\n     */"
            + "\n    private static final float[] VALUES = new float[]{"
            + "\n            %3$s"
            + "\n    };"
            + "\n    private static final int STEPS = VALUES.length - 1;"
            + "\n    private static final float STEP_SIZE = 1f / STEPS;"
            + "\n"
            + "\n    @Override"
            + "\n    public float getInterpolation(float input) {"
            + "\n        if (input >= 1.0f) {"
            + "\n            return 1.0f;"
            + "\n        }"
            + "\n        if (input <= 0f) {"
            + "\n            return 0f;"
            + "\n        }"
            + "\n"
            + "\n        // Calculate index - We use min with length - 2 to avoid IndexOutOfBoundsException when"
            + "\n        // we lerp (linearly interpolate) in the return statement"
            + "\n        int position = Math.min((int) (input * STEPS), STEPS - 1);"
            + "\n"
            + "\n        // Calculate values to account for small offsets as the lookup table has discrete values"
            + "\n        float quantized = position * STEP_SIZE;"
            + "\n        float diff = input - quantized;"
            + "\n        float weight = diff / STEP_SIZE;"
            + "\n"
            + "\n        // Linearly interpolate between the table values"
            + "\n        return VALUES[position] weight * (VALUES[position 1] - VALUES[position]);"
            + "\n    }"
            + "\n"
            + "\n}";

    /**
     * How many chars a lookup table value takes, given that the format is <code>0.1234f, </code> with either space or
     * newline in the end
     */
    private static final int CHARS_PER_VALUE = 9;

    /**
     * How many values per row to print. For pretty output.
     */
    private static final int VALUES_PER_ROW = 6;

    private static final NumberFormat FORMAT = makeFormat();

    /**
     * Generates Java code for a table lookup interpolator based on provided values
     *
     * @param packageName package name to write into the template
     * @param className   class name to write into the template
     * @param values      an array of float values that must be recorded at equal intervals
     * @return generated drop-in Java code
     */
    public String generateInterpolatorCode(String packageName, String className, float[] values) {
        StringBuilder valuesBuilder = new StringBuilder(values.length * CHARS_PER_VALUE);

        // Append all values but the last one
        for (int i = 0, len = values.length - 1; i < len; /* incremented in loop body */) {
            valuesBuilder.append(FORMAT.format(values[i])).append('f').append(',');
            if (++i % VALUES_PER_ROW == 0) {
                valuesBuilder.append('\n');
            } else {
                valuesBuilder.append(' ');
            }
        }
        // Append last value
        valuesBuilder.append(FORMAT.format(values[values.length - 1])).append('f');

        // and generate Java code out of the given template
        return String.format(TABLE_LOOKUP_TEMPLATE, packageName, className, valuesBuilder.toString());
    }

    private static NumberFormat makeFormat() {
        NumberFormat format = DecimalFormat.getNumberInstance(Locale.ROOT);
        format.setMinimumFractionDigits(4);
        format.setMaximumFractionDigits(4);
        return format;
    }

}
