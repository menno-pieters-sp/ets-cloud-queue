/**
 * Copyright (C) 2022-2023 SailPoint Technologies
 */
package sailpoint.ets.cloud.queue.tools;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * 
 * @author menno.pieters
 *
 * Class with several utility methods. 
 */
public class Util {

	private static final Logger log = LogManager.getLogger(Util.class);
	
	public static final String ALPHANUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	public static final String TOKENCHARS = ALPHANUM + "_@.-";
	public static final String[] DATEFORMATS = { "yyyyMMddHHmmss", "yyyyMMddHHmmss z", "yyyyMMddHHmmss Z", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm:ss z", "yyyy.MM.dd HH:mm:ss Z" };
	public static final String PREF_DATEFORMAT = "yyyy.MM.dd HH:mm:ss z";
	
	public Util() {	}
	
	/**
	 * Check whether a string is null or has zero length (after trimming).
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isNullOrEmpty(String s) {
		return (s == null || s.trim().length() == 0);
	}

	/**
	 * Check whether a string is NOT null or has NON-zero length (after trimming).
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isNotNullOrEmpty(String s) {
		return !isNullOrEmpty(s);
	}
	
	/**
	 * Convert any non-null object to its String representation.
	 *  
	 * @param o
	 * @return
	 */
	public static String otos(Object o) {
		if (o != null) {
			return o.toString();
		}
		return null;
	}
	
	/**
	 * Convert any object into its integer value. A zero (0) value will be returned if it is null or cannot be converted to a numeric value.
	 * 
	 * @param o
	 * @return
	 */
	public static int otoi(Object o) {
		if (o != null) {
			if (o instanceof Character) {
				char c = ((Character) o).charValue();
				return (int) c;
			}
			if (o instanceof Byte) {
				return ((Byte) o).intValue();
			}
			if (o instanceof Integer) {
				return ((Integer) o).intValue();
			}
			if (o instanceof BigInteger) {
				return ((BigInteger) o).intValue();
			}
			if (o instanceof Long) {
				return ((Long) o).intValue();
			}
			if (o instanceof Boolean) {
				boolean b = (Boolean) o;
				return (b?1:0);
			}
			String s = o.toString();
			try {
				Integer i = Integer.parseInt(s);
				return i;
			} catch (NumberFormatException e) {
				// Silently ignore
			}
		}
		return 0;
	}
	
	/**
	 * Convert any object into a boolean value. If no conversion is possible, the provided default is returned.
	 * 
	 * String values that equals "true" or "1" will be considered true.
	 * String values that equals "false" or "0" will be considered false.
	 * Numeric 1 is considered true, and 0 as false.
	 * Characters value "t" or "1" is considered true, "f" or "0" as false.
	 * Byte and Numeric values are considered true if not zero.
	 * Other objects will first be converted to a String.
	 *  
	 * @param o
	 * @param def
	 * @return
	 */
	public static boolean otob(Object o, boolean def) {
		if (o != null) {
			if (o instanceof Boolean) {
				return ((Boolean) o).booleanValue();
			}
			if (o instanceof String) {
				String s = (String) o;
				if ("true".equalsIgnoreCase(s.trim()) || "1".equalsIgnoreCase(s.trim())) return true;
				if ("false".equalsIgnoreCase(s.trim()) || "0".equalsIgnoreCase(s.trim())) return false;
			}
			if (o instanceof Character) {
				char c = ((Character) o).charValue();
				if (c == 't' || c == 'T' || c == '1') return true;
				if (c == 'f' || c == 'F' || c == '0') return false;
			}
			if (o instanceof Byte) {
				int i = ((Byte) o).intValue();
				return (i != 0);
			}
			if (o instanceof Number) {
				int i = ((Number) o).intValue();
				return (i != 0);
			}
			return otob(otos(o), def);
		}
		return def;
	}

	/**
	 * Convert any object into a Boolean value, defaulting to false.
	 * 
	 * @param o
	 * @return
	 */
	public static boolean otob(Object o) {
		return otob(o, false);
	}

	/**
	 * Generate a random Unique Universal IDentifier.
	 * 
	 * @return
	 */
	public static String uuid() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
	
	/**
	 * Generate a random integer within the range from start (inclusive) to end (exclusive).
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public static int randomInt(int start, int end) {
		if (start > end) {
			int t = start;
			start = end;
			end = t;
		}
		if (start == end) {
			return start;
		}
		int range = end - start;
		Double d = Math.floor(Math.random() * range);
		return start + d.intValue();
	}

	/**
	 * Generate a random positive integer from 0 (inclusive) to end (exclusive).
	 * 
	 * @param end
	 * @return
	 */
	public static int randomInt(int end) {
		return randomInt(0, end);
	}
	
	/**
	 * Generate a random positive integer from 0 (inclusive) to 10 (exclusive).
	 * 
	 * @return
	 */
	public static int randomInt() {
		return randomInt(0, 10);
	}
	
	/**
	 * Generate a random String of the specified length and using the provided set of characters.
	 *  
	 * @param len	Expected length of the random string.
	 * @param chars	Allowed characters for the random string.
	 * @return
	 */
	public static String generateRandomString(int len, String chars) {
		if (len <= 0) {
			len = 1;
		}
		String token = "";
		if (chars == null || chars.length() == 0) {
			chars = ALPHANUM;
		}
		for (int l = 0; l < len ; l++) {
			int n = chars.length();
			int p = randomInt(n);
			token += chars.substring(p, p+1);
		}
		return token;
	}
	
	/**
	 * Generate a salt for hashing, consisting of alphanumeric characters (0-9, A-Z, a-z) and length 8 (see {@linkplain #ALPHANUM}}).
	 * 
	 * @return
	 */
	public static String generateSalt() {
		return generateRandomString(8, ALPHANUM);		
	}
	
	/**
	 * Generate a random token. If length is 0 or less, the default length of 16 characters will be used (see {@linkplain #TOKENCHARS}).
	 * 
	 * @param len
	 * @return
	 */
	public static String generateToken(int len) {
		if (len <= 0) {
			len = 16;
		}
		return generateRandomString(len, TOKENCHARS);
	}

	/**
	 * Generate a random token of 16 characters (see {@linkplain #TOKENCHARS}).
	 * 
	 * @return
	 */
	public static String generateToken() {
		return generateToken(0);
	}
	
	/**
	 * Convert a String to a date, using the specified format.
	 * 
	 * @param s
	 * @param format
	 * @return
	 * @throws ParseException
	 */
	public static Date stringToDate(String s, String format) throws ParseException {
		if (Util.isNotNullOrEmpty(s) && Util.isNotNullOrEmpty(format)) {
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			return sdf.parse(s);
		}
		return null;
	}

	/**
	 * Try and convert a String value to a date using datformat specified in {@linkplain #DATEFORMATS}.
	 * 
	 * @param s
	 * @return
	 * @throws ParseException
	 */
	public static Date stringToDate(String s) throws ParseException {
		if (Util.isNotNullOrEmpty(s)) {
			for (String format: DATEFORMATS) {
				try {
					Date d = stringToDate(s.trim(), format);
					return d;
				} catch (ParseException e) {
					// Ignore, try next.
				}
			}
			throw new ParseException("Unsupported date format.", 0);
		}
		return null;
	}
	
	/**
	 * Try and convert an object into a date.
	 * <ul>
	 * <li>Integer and Long values will be considered the number of milliseconds since the Epoch.</li>
	 * <li>A String value will be parsed using {@link #stringToDate(String)}.</li>
	 * </ul>
	 * 
	 * @param o
	 * @return
	 * @throws ParseException
	 */
	public static Date objectToDate(Object o) throws ParseException {
		if (o instanceof Date) {
			return (Date) o;
		}
		if (o instanceof Integer) {
			Long l = new Long((Integer) o);
			Date d = new Date();
			d.setTime(l);
			return d;
		}
		if (o instanceof Long) {
			Long l = (Long) o;
			Date d = new Date();
			d.setTime(l);
			return d;
		}
		if (o instanceof String) {
			String s = (String) o;
			return stringToDate(s);
		}
		return null;
	}
}
