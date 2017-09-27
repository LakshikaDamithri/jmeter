/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jorphan.math;

//https://commons.apache.org/proper/commons-math/javadocs/api-3.0/org/apache/commons/math3/stat/descriptive/rank/Percentile.html

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.lang3.mutable.MutableLong;

/**
 * This class serves as a way to calculate the median, max, min etc. of a list of values.
 * It is not threadsafe.
 *
 * @param <T> type parameter for the calculator
 *
 */
public abstract class StatCalculator<T extends Number & Comparable<? super T>> {

    // key is the type to collect (usually long), value = count of entries
    private final Map<Double, MutableLong> valuesMap = new TreeMap<>();
    // We use a TreeMap because we need the entries to be sorted

    // Running values, updated for each sample
    private double sum = 0;

    private double sumOfSquares = 0;

    private double mean = 0;

    private double deviation = 0;

    private long count = 0;

    private double min;

    private double max;

    private long bytes = 0;
    
    private long sentBytes = 0;

    private final double ZERO;

    private final double MAX_VALUE; // e.g. Long.MAX_VALUE

    private final double MIN_VALUE; // e.g. Long.MIN_VALUE

    /**
     * This constructor is used to set up particular values for the generic class instance.
     *
     * @param zero - value to return for Median and PercentPoint if there are no values
     * @param min - value to return for minimum if there are no values
     * @param max - value to return for maximum if there are no values
     */
    public StatCalculator(final double zero, final double min, final double max) {
        super();
        ZERO = zero;
        MAX_VALUE = max;
        MIN_VALUE = min;
        this.min = MAX_VALUE;
        this.max = MIN_VALUE;
    }

    public void clear() {
        valuesMap.clear();
        sum = 0;
        sumOfSquares = 0;
        mean = 0;
        deviation = 0;
        count = 0;
        bytes = 0;
        sentBytes = 0;
        max = MIN_VALUE;
        min = MAX_VALUE;
    }

    /**
     * Add to received bytes
     * @param newValue number of newly received bytes
     */
    public void addBytes(long newValue) {
        bytes += newValue;
    }

    /**
     * Add to sent bytes
     * @param newValue number of newly sent bytes
     */
    public void addSentBytes(long newValue) {
        sentBytes += newValue;
    }

    public void addAll(StatCalculator<Double> calc) {
        for(Entry<Double, MutableLong> ent : calc.valuesMap.entrySet()) {
            addEachValue(ent.getKey(), ent.getValue().longValue());
        }
    }

    public double getMedian() {
        return getPercentPoint(0.5);
    }

    public long getTotalBytes() {
        return bytes;
    }
    
    public long getTotalSentBytes() {
        return sentBytes;
    }

    /**
     * Get the value which %percent% of the values are less than. This works
     * just like median (where median represents the 50% point). A typical
     * desire is to see the 90% point - the value that 90% of the data points
     * are below, the remaining 10% are above.
     *
     * @param percent
     *            number representing the wished percent (between <code>0</code>
     *            and <code>1.0</code>)
     * @return number of values less than the percentage
     */
    public double getPercentPoint(float percent) {
        return getPercentPoint((double) percent);
    }

    /**
     * Get the value which %percent% of the values are less than. This works
     * just like median (where median represents the 50% point). A typical
     * desire is to see the 90% point - the value that 90% of the data points
     * are below, the remaining 10% are above.
     *
     * @param percent
     *            number representing the wished percent (between <code>0</code>
     *            and <code>1.0</code>)
     * @return the value which %percent% of the values are less than
     */
    public double getPercentPoint(double percent) {
        if (count <= 0) {
                return ZERO;
        }
//        if (percent >= 1.0) {
//            return getMax();
//        }

        // use Math.round () instead of simple (long) to provide correct value rounding
        //long target = Math.round (count * percent);

        double pos = percent*(count +1);
        double dif = pos - Math.floor(pos);
        double L = Math.floor(pos);
        double lower = ZERO ;
        double upper = ZERO ;
        double ret_val;
        int check=0;

        if (pos<1) return getMin();
        if (pos >= count) return  getMax();

        try {
            for (Entry<Double, MutableLong> entry : valuesMap.entrySet()) {

                if(check == 1){
                    upper = entry.getKey();
                    break;
                }

                L -= entry.getValue().longValue();

                if (L == 0){
                    check = check + 1;
                    lower = entry.getKey();
                } else if ( L < 0){
                    lower = entry.getKey();
                    upper = entry.getKey();
                    break;
                }
            }
            ret_val = lower+dif*(upper-lower);
            return ret_val;

        } catch (ConcurrentModificationException ignored) {
            // ignored. May happen occasionally, but no harm done if so.
        }
        return ZERO; // TODO should this be getMin()?
    }

    /**
     * Returns the distribution of the values in the list.
     *
     * @return map containing either Integer or Long keys; entries are a Number array containing the key and the [Integer] count.
     * TODO - why is the key value also stored in the entry array? See Bug 53825
     */
    public Map<Number, Number[]> getDistribution() {
        Map<Number, Number[]> items = new HashMap<>();

        for (Entry<Double, MutableLong> entry : valuesMap.entrySet()) {
            Number[] dis = new Number[2];
            dis[0] = entry.getKey();
            dis[1] = entry.getValue();
            items.put(entry.getKey(), dis);
        }
        return items;
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return deviation;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public long getCount() {
        return count;
    }

    public double getSum() {
        return sum;
    }

    protected abstract double divide(double val, int n);

    protected abstract double divide(double val, long n);

    /**
     * Update the calculator with the values for a set of samples.
     * 
     * @param val the common value, normally the elapsed time
     * @param sampleCount the number of samples with the same value
     */
    void addEachValue(double val, long sampleCount) {
        count += sampleCount;
        double currentVal = val;
        sum += currentVal * sampleCount;
        // For n same values in sum of square is equal to n*val^2
        sumOfSquares += currentVal * currentVal * sampleCount;
        updateValueCount(val, sampleCount);
        calculateDerivedValues(val);
    }

    /**
     * Update the calculator with the value for an aggregated sample.
     * 
     * @param val the aggregate value, normally the elapsed time
     * @param sampleCount the number of samples contributing to the aggregate value
     */
    public void addValue(double val, long sampleCount) {
        count += sampleCount;
        double currentVal = val;
        sum += currentVal;
        double actualValue = val;
        if (sampleCount > 1){
            // For n values in an aggregate sample the average value = (val/n)
            // So need to add n * (val/n) * (val/n) = val * val / n
            sumOfSquares += currentVal * currentVal / sampleCount;
            actualValue = divide(val, sampleCount);
        } else { // no need to divide by 1
            sumOfSquares += currentVal * currentVal;
        }
        updateValueCount(actualValue, sampleCount);
        calculateDerivedValues(actualValue);
    }

    private void calculateDerivedValues(double actualValue) {
        mean = sum / count;
        deviation = Math.sqrt((sumOfSquares / count) - (mean * mean));
        if (actualValue> max){
            max=actualValue;
        }
        if (actualValue < min){
            min=actualValue;
        }
    }

    /**
     * Add a single value (normally elapsed time)
     * 
     * @param val the value to add, which should correspond with a single sample
     * @see #addValue(Number, long)
     */
    public void addValue(double val) {
        addValue(val, 1L);
    }

    private void updateValueCount(double actualValue, long sampleCount) {
        MutableLong count = valuesMap.get(actualValue);
        if (count != null) {
            count.add(sampleCount);
        } else {
            // insert new value
            valuesMap.put(actualValue, new MutableLong(sampleCount));
        }
    }
}
