package org.camunda.bpm.platform.plugin.optimize.data.preparation;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateConverter {

	public static Date convertToDateOfCurrentYear(long dayOfYear)
	{
		if (dayOfYear < 1 || dayOfYear > 365)
		{
			throw new IllegalArgumentException("Invalid day of year: " + dayOfYear);
		}
		
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.YEAR, 2016);
		calendar.set(Calendar.DAY_OF_YEAR, (int) dayOfYear);
		return calendar.getTime();
	}
	
	public static void main(String[] args) {
		System.out.println(convertToDateOfCurrentYear(1));
		System.out.println(convertToDateOfCurrentYear(8));
		System.out.println(convertToDateOfCurrentYear(52));
		System.out.println(convertToDateOfCurrentYear(200));
	}
}
