/*
 * Copyright (C) 2016 Actinarium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.actinarium.kinetic.pipeline;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class with a method to generate Java code out of the provided data array
 *
 * @author Paul Danyliuk
 */
public final class CodeGenerator {

    public static final String DEFAULT_PACKAGE_NAME = "com.example.kinetic";

    /**
     * Template for a resulting table lookup interpolator. The parameters are: <ol> <li>Package name</li> <li>Class
     * name</li> <li>Which record was used to generate this interpolator (e.g. Offset - X)</li>  <li>Float values,
     * delimited with a comma, optionally split into rows of 6</li> </ol>
     */
    private static final String TABLE_LOOKUP_TEMPLATE = "package %1$s;"
            + "\n"
            + "\nimport android.view.animation.Interpolator;"
            + "\n"
            + "\n/**"
            + "\n * <p>Natural motion interpolator that uses lookup table sampled at regular intervals"
            + "\n * and interpolates linearly between lookup table values.</p>"
            + "\n *"
            + "\n * <p>Generated with <a href=\"https://github.com/Actinarium/Kinetic\">Kinetic</a> from"
            + "\n * <b>%3$s</b> recorded motion."
            + "\n * Derives from Apache 2.0 licensed code from Android Support v4 Library, specifically"
            + "\n * {@link android.support.v4.view.animation.LookupTableInterpolator LookupTableInterpolator}</p>"
            + "\n */"
            + "\npublic class %2$s implements Interpolator {"
            + "\n"
            + "\n    /**"
            + "\n     * Lookup table values sampled with x at regular intervals between 0 and 1"
            + "\n     */"
            + "\n    private static final float[] VALUES = new float[]{"
            + "\n            %4$s"
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
            + "\n        int position = Math.min((int) (input * STEPS), STEPS - 1);"
            + "\n        float quantized = position * STEP_SIZE;"
            + "\n        float diff = input - quantized;"
            + "\n        float weight = diff / STEP_SIZE;"
            + "\n"
            + "\n        return VALUES[position] + weight * (VALUES[position + 1] - VALUES[position]);"
            + "\n    }"
            + "\n"
            + "\n}";

    /**
     * How many chars a lookup table line takes, given that the format of an individual value is <code>-0.1234f</code>,
     * 6 per row, delimited with comma, and spaces or newline in the end, plus 16 leading spaces for line indents
     */
    private static final int CHARS_PER_LINE = 76;

    /**
     * How many values per row to print. For pretty output.
     */
    private static final int VALUES_PER_ROW = 6;

    /**
     * Private constructor, to prevent instantiation
     */
    private CodeGenerator() {}

    /**
     * Generates Java code for a table lookup interpolator based on provided values
     *
     * @param packageName package name to write into the template
     * @param className   class name to write into the template
     * @param sourceName  title of the measurement the values were taken from
     * @param values      an array of float values that must be recorded at equal intervals
     * @return generated drop-in Java code
     */
    public static String generateInterpolatorCode(String packageName, String className, String sourceName, float[] values) {
        NumberFormat format = DecimalFormat.getNumberInstance(Locale.ROOT);
        format.setMinimumFractionDigits(4);
        format.setMaximumFractionDigits(4);
        StringBuilder valuesBuilder = new StringBuilder(CHARS_PER_LINE * (values.length / VALUES_PER_ROW + 1));

        // Append all values but the last one
        final int lengthMinusOne = values.length - 1;
        for (int i = 0; i < lengthMinusOne; /* incremented in loop body */) {
            if (values[i] > 0) {
                // Append space before positive numbers to align with those having minus sign
                valuesBuilder.append(' ');
            }
            valuesBuilder.append(format.format(values[i])).append('f').append(',');
            if (++i % VALUES_PER_ROW == 0) {
                valuesBuilder.append("\n            ");
            } else {
                valuesBuilder.append(' ');
            }
        }
        // Append last value
        valuesBuilder.append(format.format(values[lengthMinusOne])).append('f');

        // and generate Java code out of the given template
        return String.format(TABLE_LOOKUP_TEMPLATE, packageName, className, sourceName, valuesBuilder.toString());
    }

}
