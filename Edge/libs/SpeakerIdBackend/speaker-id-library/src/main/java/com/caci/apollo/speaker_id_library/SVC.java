/*
 * Automatically generated from sklearn-porter.
 *
 * To create this file, do (as of porter version 0.7.4):
 *
 *  porter svmClassifier.pkl --to ./ --java -e
 *
 * TC 2021-03-01 (Mon) --
 */
package com.caci.apollo.speaker_id_library;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Scanner;
import com.google.gson.Gson;

class SVC {
    public static final String TAG = "Tommy SVC"; // proguard needs 'final'!

    private enum Kernel { LINEAR, POLY, RBF, SIGMOID }
    private class Classifier {
        private int nClasses;
        private int nRows;
        private int[] classes;
        private double[][] vectors;
        private double[][] coefficients;
        private double[] intercepts;
        private int[] weights;
        private String kernel;
        private Kernel kkernel;
        private double gamma;
        private double coef0;
        private double degree;
        private double[] probA; // TC 2021-03-08 (Mon) --
        private double[] probB; // TC 2021-03-08 (Mon) --
    }

    private Classifier clf;
    public double [] probs;     // TC 2021-03-08 (Mon) --

    public SVC(String file) throws FileNotFoundException {
        // String jsonStr = new Scanner(new File(file)).useDelimiter("\\Z").next();
        // Log.d (TAG, "jsonStr = " + jsonStr);
        // this.clf = new Gson().fromJson (jsonStr, Classifier.class);
        // TC 2021-03-30 (Tue) -- works better for API 21
        InputStream is       = new FileInputStream(file);
        Reader isr = new InputStreamReader(is);
        this.clf = new Gson().fromJson (isr, Classifier.class);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new FileNotFoundException();
        }

        this.clf.classes = new int[this.clf.nClasses];
        for (int i = 0; i < this.clf.nClasses; i++) {
            this.clf.classes[i] = i;
        }
        this.clf.kkernel = Kernel.valueOf(this.clf.kernel.toUpperCase());
        // System.out.println (this.clf.probA[0]); // TC 2021-03-08 (Mon) --
        // System.out.println (this.clf.probB[0]); // TC 2021-03-08 (Mon) --
    }

    public int predict(double[] features) {
        double[] kernels = new double[this.clf.vectors.length];
        double kernel;
        switch (this.clf.kkernel) {
            case LINEAR:
                // <x,x'>
                for (int i = 0; i < this.clf.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.clf.vectors[i].length; j++) {
                        kernel += this.clf.vectors[i][j] * features[j];
                    }
                    kernels[i] = kernel;
                }
                break;
            case POLY:
                // (y<x,x'>+r)^d
                for (int i = 0; i < this.clf.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.clf.vectors[i].length; j++) {
                        kernel += this.clf.vectors[i][j] * features[j];
                    }
                    kernels[i] = Math.pow((this.clf.gamma * kernel) + this.clf.coef0, this.clf.degree);
                }
                break;
            case RBF:
                // exp(-y|x-x'|^2)
                for (int i = 0; i < this.clf.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.clf.vectors[i].length; j++) {
                        kernel += Math.pow(this.clf.vectors[i][j] - features[j], 2);
                    }
                    kernels[i] = Math.exp(-this.clf.gamma * kernel);
                }
                break;
            case SIGMOID:
                // tanh(y<x,x'>+r)
                for (int i = 0; i < this.clf.vectors.length; i++) {
                    kernel = 0.;
                    for (int j = 0; j < this.clf.vectors[i].length; j++) {
                        kernel += this.clf.vectors[i][j] * features[j];
                    }
                    kernels[i] = Math.tanh((this.clf.gamma * kernel) + this.clf.coef0);
                }
                break;
        }

        int[] starts = new int[this.clf.nRows];
        for (int i = 0; i < this.clf.nRows; i++) {
            if (i != 0) {
                int start = 0;
                for (int j = 0; j < i; j++) {
                    start += this.clf.weights[j];
                }
                starts[i] = start;
            } else {
                starts[0] = 0;
            }
        }

        int[] ends = new int[this.clf.nRows];
        for (int i = 0; i < this.clf.nRows; i++) {
            ends[i] = this.clf.weights[i] + starts[i];
        }

        if (this.clf.nClasses == 2) {
            for (int i = 0; i < kernels.length; i++) {
                kernels[i] = -kernels[i];
            }
            double decision = 0.;
            for (int k = starts[1]; k < ends[1]; k++) {
                decision += kernels[k] * this.clf.coefficients[0][k];
            }
            for (int k = starts[0]; k < ends[0]; k++) {
                decision += kernels[k] * this.clf.coefficients[0][k];
            }
            decision += this.clf.intercepts[0];
            if (decision > 0) {
                return 0;
            }
            return 1;
        }

        double[] decisions = new double[this.clf.intercepts.length];
        for (int i = 0, d = 0, l = this.clf.nRows; i < l; i++) {
            for (int j = i + 1; j < l; j++) {
                double tmp = 0.;
                for (int k = starts[j]; k < ends[j]; k++) {
                    tmp += this.clf.coefficients[i][k] * kernels[k];
                }
                for (int k = starts[i]; k < ends[i]; k++) {
                    tmp += this.clf.coefficients[j - 1][k] * kernels[k];
                }
                decisions[d] = tmp + this.clf.intercepts[d];
                d++;
            }
        }


        // TC 2021-03-08 (Mon) --
        // Probability:
        // double [] probs = clf.predict_proba (clf.nClasses, double[] dec_values,
        //                                      double[] probA, double[] probB)
        // System.out.println ("decisions are");
        // System.out.println (decisions[0]);
        // System.out.println (decisions[1]);
        // Log.d (TAG, "decisions =" + decisions);
        // Log.d (TAG, "clf.nClasses =" + clf.nClasses);
        // Log.d (TAG, "clf.probA =" + clf.probA);
        // Log.d (TAG, "clf.probB =" + clf.probB);
        probs = predict_proba (clf.nClasses, decisions, clf.probA, clf.probB);
        
        int[] votes = new int[this.clf.intercepts.length];
        for (int i = 0, d = 0, l = this.clf.nRows; i < l; i++) {
            for (int j = i + 1; j < l; j++) {
                votes[d] = decisions[d] > 0 ? i : j;
                d++;
            }
        }

        int[] amounts = new int[this.clf.nClasses];
        for (int i = 0, l = votes.length; i < l; i++) {
            amounts[votes[i]] += 1;
        }

        int classVal = -1, classIdx = -1;
        for (int i = 0, l = amounts.length; i < l; i++) {
            if (amounts[i] > classVal) {
                classVal = amounts[i];
                classIdx = i;
            }
        }
        return this.clf.classes[classIdx];
    }




    private static double sigmoid_predict (double decision_value, double A, double B)
    {
        double fApB = decision_value*A+B;
        // 1-p used later; avoid catastrophic cancellation
        if (fApB >= 0)
            return Math.exp(-fApB)/(1.0+Math.exp(-fApB));
        else
            return 1.0/(1+Math.exp(fApB)) ;
    }


    // Method 2 from the multiclass_prob paper by Wu, Lin, and Weng
    private static void multiclass_probability (int k, double[][] r, double[] p)
    {
        int t,j;
        int iter = 0, max_iter=Math.max(100,k);
        double[][] Q=new double[k][k];
        double[] Qp=new double[k];
        double pQp, eps=0.005/k;
        
        for (t=0;t<k;t++)
            {
                p[t]=1.0/k;  // Valid if k = 1
                Q[t][t]=0;
                for (j=0;j<t;j++)
                    {
                        Q[t][t]+=r[j][t]*r[j][t];
                        Q[t][j]=Q[j][t];
                    }
                for (j=t+1;j<k;j++)
                    {
                        Q[t][t]+=r[j][t]*r[j][t];
                        Q[t][j]=-r[j][t]*r[t][j];
                    }
            }
        for (iter=0;iter<max_iter;iter++)
            {
                // stopping condition, recalculate QP,pQP for numerical accuracy
                pQp=0;
                for (t=0;t<k;t++)
                    {
                        Qp[t]=0;
                        for (j=0;j<k;j++)
                            Qp[t]+=Q[t][j]*p[j];
                        pQp+=p[t]*Qp[t];
                    }
                double max_error=0;
                for (t=0;t<k;t++)
                    {
                        double error=Math.abs(Qp[t]-pQp);
                        if (error>max_error)
                            max_error=error;
                    }
                if (max_error<eps) break;
                
                for (t=0;t<k;t++)
                    {
                        double diff=(-Qp[t]+pQp)/Q[t][t];
                        p[t]+=diff;
                        pQp=(pQp+diff*(diff*Q[t][t]+2*Qp[t]))/(1+diff)/(1+diff);
                        for (j=0;j<k;j++)
                            {
                                Qp[j]=(Qp[j]+diff*Q[t][j])/(1+diff);
                                p[j]/=(1+diff);
                            }
                    }
            }
        // if (iter>=max_iter)
        //     svm.info("Exceeds max_iter in multiclass_prob\n");
    }

    
    // Method 2 from the multiclass_prob paper by Wu, Lin, and Weng
    private static double[] predict_proba (int nr_class, double[] dec_values,
                                           double[] probA, double[] probB) 
    {
        // int i;
        // int nr_class = model.nr_class;
        // double[] dec_values = new double[nr_class*(nr_class-1)/2];
        // svm_predict_values(model, x, dec_values);
        
        double min_prob=1e-7;
        double[][] pairwise_prob = new double[nr_class][nr_class];
        double[] prob_estimates= new double[nr_class];
        
        int k=0;
        for (int i=0;i<nr_class;i++)
            for(int j=i+1;j<nr_class;j++)
                {
                    pairwise_prob[i][j] =
                        Math.min (Math.max (sigmoid_predict (dec_values[k],
                                                             probA[k], probB[k]),
                                            min_prob), 1-min_prob);
                    pairwise_prob[j][i] = 1 - pairwise_prob[i][j];
                    k++;
                }
        if (nr_class == 2)
            {
                prob_estimates[0] = pairwise_prob[0][1];
                prob_estimates[1] = pairwise_prob[1][0];
            }
        else
            multiclass_probability(nr_class,pairwise_prob,prob_estimates);
    
        return prob_estimates;
    }

    public double[] get_predict_proba () {
        return probs;
    }

    public double get_max_predict_proba () {
        if (probs == null) return 0;
        int prob_max_idx = 0;
        for (int i = 1; i < clf.nClasses; i++)
            if (probs[i] > probs[prob_max_idx])
                prob_max_idx = i;
        return probs [prob_max_idx];
    }

    
    // public static void main(String[] args) throws FileNotFoundException {
    //     if (args.length > 0 && args[0].endsWith(".json")) {

    //         // Features:
    //         double[] features = new double[args.length-1];
    //         for (int i = 1, l = args.length; i < l; i++) {
    //             features[i - 1] = Double.parseDouble(args[i]);
    //         }

    //         // Parameters:
    //         String modelData = args[0];

    //         // Estimators:
    //         SVC clf = new SVC(modelData);

    //         // Prediction:
    //         int prediction = clf.predict(features);
    //         System.out.println(prediction);

    //         // Probability:  TC 2021-03-08 (Mon) --
    //         for (double element: clf.probs) {
    //             System.out.println(element);
    //         }
    //     }
    // }
}
