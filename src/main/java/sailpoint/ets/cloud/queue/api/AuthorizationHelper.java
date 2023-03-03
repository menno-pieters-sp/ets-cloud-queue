package sailpoint.ets.cloud.queue.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.HashMap;
import java.util.Map;

import sailpoint.ets.cloud.queue.tools.Util;

public class AuthorizationHelper {

	public final static String SSHA256PREFIX = "{SSHA256}";
	
	public static final Logger log = LogManager.getLogger(AuthorizationHelper.class);

	public AuthorizationHelper() {
	}

	public static String getBearerToken(String header) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: getBearerToken(%s)", (header==null)?"null":"********"));
		}
		if (header != null && header.length() > 0) {
			if (header.startsWith("Bearer ")) {
				String token = header.substring("Bearer ".length() - 1).trim();
				return token;
			}
		}
		return null;
	}

	public static Map<String, String> getBasicCredentials(String header) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: getBasicCredentials(%s)", (header==null)?"null":"********"));
		}
		if (header != null && header.length() > 0) {
			if (header.startsWith("Basic ")) {
				String encodedAuthData = header.substring("Basic ".length() - 1).trim();
				String authData = new String(Base64.getDecoder().decode(encodedAuthData));
				int i = authData.indexOf(":");
				if (i >= 0) {
					String username = authData.substring(0, i);
					String password = authData.substring(i + 1);
					Map<String, String> map = new HashMap<String, String>();
					map.put("username", username);
					map.put("password", password);
					if (log.isTraceEnabled()) {
						log.trace(String.format("Credentials: %s", map));
					}
					return map;
				}
			}
		}
		return null;
	}

	public static String ssha256(String salt, String password) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: ssha256(%s, %s)", "********", "********"));
		}
		if (Util.isNotNullOrEmpty(salt) && Util.isNotNullOrEmpty(password)) {
			String saltedPassword = salt + password;
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] encodedHash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
				Encoder encoder = Base64.getEncoder();
				String hashString = encoder.encodeToString(encodedHash);
				String result = SSHA256PREFIX + encoder.encodeToString(salt.getBytes(StandardCharsets.UTF_8)) + "$" + hashString;
				if (log.isTraceEnabled()) {
					log.trace(String.format("Return ssha256: %s********", SSHA256PREFIX));
				}
				return result;
			} catch (NoSuchAlgorithmException e) {
				// Unsupported - return null;
			}
		}
		return null;
	}

	public static boolean validatePassword(String hashedPassword, String password) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: validatePassword(%s, %s)", "********", "********"));
		}
		if (Util.isNotNullOrEmpty(hashedPassword)) {
			if (hashedPassword.startsWith(SSHA256PREFIX) && hashedPassword.contains("$")) {
				String h = hashedPassword.substring(SSHA256PREFIX.length());
				int i = h.indexOf("$");
				String salt64 = h.substring(0, i);
				String salt = new String(Base64.getDecoder().decode(salt64));
				String hash = ssha256(salt, password);
				return (hashedPassword.equals(hash));
			}
		}
		return false;
	}

}
