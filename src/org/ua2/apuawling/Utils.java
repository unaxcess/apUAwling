package org.ua2.apuawling;

public class Utils {
	public static String toString(String[] list) {
		StringBuilder sb = new StringBuilder("[");
		for(String item : list) {
			sb.append(" ").append(item);
		}
		sb.append(" ]");
		return sb.toString();
	}
}
