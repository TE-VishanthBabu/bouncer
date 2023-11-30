package com.zorsecyber.bouncer.api.lib;

public class EnumUtils {
	public static <E extends Enum<E>> String formatName(E e) {
		return e.toString();
	}
}
