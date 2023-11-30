package com.zorsecyber.bouncer.api.lib;

import com.zorsecyber.bouncer.api.dao.Session;

public class SessionUtils {
	private static final int SESSION_DURATION_HOURS = 1;

	public static Boolean sessionIsOpen(Session s)
	{
		return true;
//		Calendar cal = Calendar.getInstance();
//		cal.setTime(s.getCreationTimestamp());
//		System.out.println("Current time : "+(new Date()).toString());
//		cal.add(Calendar.HOUR_OF_DAY, SESSION_DURATION_HOURS);
//		System.out.println("Times out at : "+cal.getTime().toString());
//		return ((new Date()).before(cal.getTime()));
	}
	
}
