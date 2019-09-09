/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2010-2019  Minnesota Department of Transportation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package us.mn.state.dot.tms.server;

import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import us.mn.state.dot.sched.DebugLog;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.units.Interval;
import static us.mn.state.dot.tms.server.Constants.MISSING_DATA;

/**
 * A cache for periodic sample data.  
 *
 * @author Douglas Lau
 */
public class PeriodicSampleCache {

	/** Sample cache debug log */
	static private final DebugLog SAMPLE_LOG = new DebugLog("samples");

	/** Threshold for minimum valid stamp */
	static private final long SAMPLE_MIN_MS = new Interval(2,
		Interval.Units.DAYS).ms();

	/** Threshold for maximum valid stamp */
	static private final long SAMPLE_MAX_MS = new Interval(1,
		Interval.Units.HOURS).ms();

	/** Check if a timestamp is valid */
	static private boolean checkStamp(long ts) {
		long now = TimeSteward.currentTimeMillis();
		return (ts > now - SAMPLE_MIN_MS && ts < now + SAMPLE_MAX_MS);
	}

	/** Interpolate summed sample data into an array of values.
	 * @param values Array of existing sample values.
	 * @param total Total of all sample values.
	 * @return Array of interpolated samples. */
	static private int[] interpolateSum(int[] values, int total) {
		int e_total = 0;	// existing values total
		int n_miss = 0;		// number of missing samples
		for (int value: values) {
			if (value < 0)
				n_miss++;
			else
				e_total += value;
		}
		if (n_miss > 0) {
			int excess = total - e_total;
			if (excess >= 0)
				return missingSum(values, excess, n_miss);
		}
		return new int[0];
	}

	/** Put missing sum sample data into an array of values.
	 * @param values Array of existing sample values.
	 * @param excess Excess sample data to distribute in missing samples.
	 * @param n_miss Number of missing samples.
	 * @return Array of samples which were missing. */
	static private int[] missingSum(int[] values, int excess, int n_miss) {
		int[] vals = new int[values.length];
		int t_miss = excess / n_miss;
		int m_miss = excess % n_miss;
		for (int i = 0; i < values.length; i++) {
			if (values[i] < 0) {
				vals[i] = t_miss;
				if (m_miss > 0) {
					vals[i]++;
					m_miss--;
				}
			} else
				vals[i] = MISSING_DATA;
		}
		return vals;
	}

	/** Interpolate averaged sample data into an array of values.
	 * @param values Array of existing sample values.
	 * @param average Average of all sample values.
	 * @return Array of interpolated samples. */
	static private int[] interpolateAverage(int[] values, int average) {
		int e_total = 0;	// existing values total
		int n_miss = 0;		// number of missing samples
		for (int value: values) {
			if (value < 0)
				n_miss++;
			else
				e_total += value;
		}
		if (n_miss > 0) {
			int a_total = average * values.length;
			float excess = a_total - e_total;
			if (excess >= 0) {
				int m_avg = Math.round(excess / n_miss);
				return missingAverage(values, m_avg);
			}
		}
		return new int[0];
	}

	/** Put missing average sample data into an array of values.
	 * @param values Array of existing sample values.
	 * @param m_avg Average value to store in missing samples.
	 * @return Array of samples which were missing. */
	static private int[] missingAverage(int[] values, int m_avg) {
		int[] vals = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			vals[i] = (values[i] < 0)
			        ? m_avg
			        : MISSING_DATA;
		}
		return vals;
	}

	/** Sample type */
	public final PeriodicSampleType sample_type;

	/** Collection of periodic samples.  Every sample is guaranteed to
	 * have the same sampling period. */
	private final ConcurrentSkipListSet<PeriodicSample> samples =
		new ConcurrentSkipListSet<PeriodicSample>();

	/** Create a new periodic sample cache.
	 * @param tp Sample type. */
	public PeriodicSampleCache(PeriodicSampleType tp) {
		sample_type = tp;
	}

	/** Add a periodic sample to the cache.
	 * If the sampling period is incompatable with existing samples, the
	 * cache is cleared first.
	 * @param ps Sample to add to the cache. */
	public void add(PeriodicSample ps, String name) {
		if (checkStamp(ps.stamp)) {
			if (sample_type.isValid(ps)) {
				if (!isPeriodOk(ps.period))
					samples.clear();
				if (isPeriodSame(ps.period))
					addSample(ps);
				else
					interpolate(ps);
			}
		} else {
			if (SAMPLE_LOG.isOpen()) {
				SAMPLE_LOG.log(name + ": invalid stamp: " +
					new Date(ps.stamp));
			}
		}
	}

	/** Check if a period is OK to be cached.
	 * @param period Period to check (seconds).
	 * @return true if period is OK to be cached. */
	private boolean isPeriodOk(int period) {
		return period % getPeriod(period) == 0;
	}

	/** Is a sample period the same as for the cache? */
	private boolean isPeriodSame(int period) {
		return period == getPeriod(period);
	}

	/** Get the sample period.
	 * @param period Default sample period.
	 * @return Sample period (seconds). */
	private int getPeriod(int period) {
		return samples.isEmpty()
		      ? period
		      : samples.first().period;
	}

	/** Add a sample */
	private void addSample(PeriodicSample ps) {
		assert ps.period == getPeriod(ps.period) : "Invalid period";
		assert !exists(ps.start()) : "Duplicate start time";
		samples.add(ps);
	}

	/** Check if a sample exists with the given time stamp (start) */
	private boolean exists(long stamp) {
		for (PeriodicSample ps: samples) {
			if (ps.start() == stamp)
				return true;
		}
		return false;
	}

	/** Interpolate sample data from a sample with a larger period.
	 * Any missing samples are estimated and added to the cache.
	 * @param ps Periodic sample (with a larger period). */
	private void interpolate(PeriodicSample ps) {
		long start = ps.start();
		int period = getPeriod(ps.period);
		int n_samples = ps.period / period;
		assert n_samples > 1;
		int[] values = getValues(start, ps.end(), n_samples, period);
		switch (sample_type.aggregation) {
		case SUM:
			addSamples(start, period, interpolateSum(values,
				ps.value));
			return;
		case AVERAGE:
			addSamples(start, period, interpolateAverage(values,
				ps.value));
			return;
		default:
			return;
		}
	}

	/** Get an array of sample values from the cache.
	 * @param start Time stamp at start of samples.
	 * @param end Time stamp at end of samples.
	 * @param n_samples Number of sample values.
	 * @param period Period used for samples.
	 * @return Array of samples values. */
	private int[] getValues(long start, long end, int n_samples,
		int period)
	{
		int period_ms = period * 1000;
		int[] values = new int[n_samples];
		for (int i = 0; i < values.length; i++)
			values[i] = MISSING_DATA;
		for (PeriodicSample ps: samples) {
			long stamp = ps.start();
			if (stamp >= start && stamp < end) {
				int i = (int) ((stamp - start) / period_ms);
				values[i] = ps.value;
			}
		}
		return values;
	}

	/** Get aggregate of sampled values in a time interval */
	public int getValue(long start, long end) {
		switch (sample_type.aggregation) {
		case SUM:
			return getSum(start, end);
		case AVERAGE:
			return getAverage(start, end);
		default:
			return MISSING_DATA;
		}
	}

	/** Get sum of sampled values in a time interval */
	private int getSum(long start, long end) {
		int period = 0;
		int total = 0;
		int n_samples = 0;
		for (PeriodicSample ps: samples) {
			if (0 == period)
				period = ps.period;
			else if (period != ps.period)
				break;
			if (ps.value >= 0) {
				long stamp = ps.start();
				if (stamp >= start && stamp < end) {
					total += ps.value;
					n_samples++;
				}
			}
		}
		long sam_ms = n_samples * period * 1000; // sampled period
		long full_ms = end - start;              // full period
		if (sam_ms == full_ms)
			return total;
		else if (2 * sam_ms >= full_ms) {  // at least half sampled
			float r = full_ms / (float) sam_ms;
			return Math.round(total * r);
		} else
			return MISSING_DATA;
	}

	/** Get average of sampled values in a time interval */
	private int getAverage(long start, long end) {
		int total = 0;
		int n_samples = 0;
		for (PeriodicSample ps: samples) {
			if (ps.value >= 0) {
				long stamp = ps.start();
				if (stamp >= start && stamp < end) {
					total += ps.value;
					n_samples++;
				}
			}
		}
		return (n_samples > 0)
		      ? Math.round(total / (float) n_samples)
		      : MISSING_DATA;
	}

	/** Add an array of samples.
	 * @param start Start time of sample array.
	 * @param period Sampling period (seconds).
	 * @param vals Array of sample values to add. */
	private void addSamples(long start, int period, int[] vals) {
		int period_ms = period * 1000;
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] >= 0) {
				long stamp = start + period_ms * (i + 1);
				addSample(new PeriodicSample(stamp, period,
					vals[i]));
			}
		}
	}

	/** Get a sample iterator. */
	public Iterator<PeriodicSample> iterator() {
		return samples.iterator();
	}

	/** Purge all samples before a specified time stamp.
	 * @param before Time stamp to purge before. */
	public void purge(long before) {
		Iterator<PeriodicSample> it = iterator();
		while (it.hasNext()) {
			PeriodicSample ps = it.next();
			if (ps.end() < before)
				it.remove();
			else
				break;
		}
	}
}
