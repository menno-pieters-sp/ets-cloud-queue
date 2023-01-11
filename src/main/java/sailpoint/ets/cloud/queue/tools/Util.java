package sailpoint.ets.cloud.queue.tools;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Util {

	public static final Logger log = LogManager.getLogger(Util.class);
	
	public static final String ALPHANUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	public static final String TOKENCHARS = ALPHANUM + "_@.-";
	public static final String[] DATEFORMATS = { "yyyyMMddHHmmss", "yyyyMMddHHmmss z", "yyyyMMddHHmmss Z", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm:ss z", "yyyy.MM.dd HH:mm:ss Z" };
	public static final String PREF_DATEFORMAT = "yyyy.MM.dd HH:mm:ss z";
	
	public Util() {	}
	
	public static boolean isNullOrEmpty(String s) {
		return (s == null || s.trim().length() == 0);
	}

	public static boolean isNotNullOrEmpty(String s) {
		return !isNullOrEmpty(s);
	}
	
	public static String otos(Object o) {
		if (o != null) {
			return o.toString();
		}
		return null;
	}
	
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
	
	public static boolean otob(Object o, boolean def) {
		if (o != null) {
			if (o instanceof Boolean) {
				return ((Boolean) o).booleanValue();
			}
			if (o instanceof String) {
				String s = (String) o;
				return ("true".equalsIgnoreCase(s.trim()));
			}
			if (o instanceof Byte) {
				int i = ((Byte) o).intValue();
				return (i != 0);
			}
			if (o instanceof Short) {
				int i = ((Short) o).intValue();
				return (i != 0);
			}
			if (o instanceof Integer) {
				int i = ((Integer) o).intValue();
				return (i != 0);
			}
			if (o instanceof BigInteger) {
				int i = ((BigInteger) o).intValue();
				return (i != 0);
			}
			if (o instanceof Long) {
				long l = ((Long) o).intValue();
				return (l != 0L);
			}
			if (o instanceof Float) {
				float f = ((Float) o).floatValue();
				return (f != 0.0);
			}
			if (o instanceof Double) {
				double d = ((Double) o).floatValue();
				return (d != 0.0);
			}
			if (o instanceof Character) {
				char c = ((Character) o).charValue();
				if (c == 't' || c == 'T') return true;
				if (c == 'f' || c == 'F') return false;
			}
		}
		return def;
	}

	public static boolean otob(Object o) {
		return otob(o, false);
	}

	public static String uuid() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
	
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

	public static int randomInt(int end) {
		return randomInt(0, end);
	}
	
	public static int randomInt() {
		return randomInt(0, 10);
	}
	
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
	
	public static String generateSalt() {
		return generateRandomString(8, ALPHANUM);		
	}
	
	public static String generateToken(int len) {
		if (len <= 0) {
			len = 16;
		}
		return generateRandomString(len, TOKENCHARS);
	}

	public static String generateToken() {
		return generateToken(0);
	}
	
	public static Date stringToDate(String s, String format) throws ParseException {
		if (Util.isNotNullOrEmpty(s) && Util.isNotNullOrEmpty(format)) {
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			return sdf.parse(s);
		}
		return null;
	}

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
