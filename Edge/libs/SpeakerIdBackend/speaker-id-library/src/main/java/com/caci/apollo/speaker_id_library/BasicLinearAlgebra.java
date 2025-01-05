package com.caci.apollo.speaker_id_library;

// Taken from SMILE java libary  https://haifengl.github.io/

import org.json.simple.JSONArray;

public class BasicLinearAlgebra {

    /**
     * Returns the dot product between two vectors.
     * @param x a vector.
     * @param y a vector.
     * @return the dot product.
     */
    public static double dot(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Arrays have different length.");
        }

        double sum = 0.0;
        for (int i = 0; i < x.length; i++) {
            sum += x[i] * y[i];
        }

        return sum;
    }

    
    public static double[] fillData(JSONArray jsonArray){
        double[] fData = new double[jsonArray.size()];
        for (int idx = 0; idx < jsonArray.size(); idx++) {
            fData[idx] = (double) jsonArray.get(idx);
        }
        return fData;
    }

    /**
     * Element-wise sum of two arrays y = x + y.
     * @param x a vector.
     * @param y avector.
     */
    public static void add(double[] y, double[] x) {
        if (x.length != y.length) {
            throw new IllegalArgumentException
                (String.format("Arrays have different length: x[%d], y[%d]",
                               x.length, y.length));
        }
            
        for (int i = 0; i < x.length; i++) {
            y[i] += x[i];
        }
    }

    /**
     * Unitizes each row of a matrix to unit length (L_2 norm).
     * @param x the matrix.
     */
    public static void normalizeRows(double[][] x) {
        for (double[] xi : x) {
            scale (1.0 / norm2 (xi), xi);
        }
    }
        
        
    /**
     * Scale each element of an array by a constant x = a * x.
     * @param a the scale factor.
     * @param x the input and output vector.
     */
    public static void scale(double a, double[] x) {
        for (int i = 0; i < x.length; i++) {
            x[i] *= a;
        }
    }
        
    /**
     * L<sub>2</sub> vector norm.
     * @param x a vector.
     * @return L<sub>2</sub> norm.
     */
    public static double norm2(double[] x) {
        double norm = 0.0;
        for (double n : x) {
            norm += n * n;
        }
        norm = Math.sqrt(norm);
        return norm;
    }
        
}
