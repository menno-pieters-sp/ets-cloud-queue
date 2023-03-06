/**
 * Copyright (C) 2022-2023 SailPoint Technologies
 */
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

/**
 * 
 * @author menno.pieters
 *
 */
public class AuthorizationHelper {

	public final static String SSHA256PREFIX = "{SSHA256}";

	private static final Logger log = LogManager.getLogger(AuthorizationHelper.class);

	public AuthorizationHelper() {
	}

	/**
	 * Retrieve the Bearer token from the authorization header.
	 * 
	 * @param header
	 *            Contents of the Authorization header.
	 * @return token or null if not found.
	 */
	public static String getBearerToken(String header) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: getBearerToken(%s)", (header == null) ? "null" : "********"));
		}
		if (header != null && header.length() > 0) {
			if (header.startsWith("Bearer ")) {
				String token = header.substring("Bearer ".length() - 1).trim();
				return token;
			}
		}
		return null;
	}

	/**
	 * Find the username and password from the Authorization header for Basic authentication.
	 * 
	 * @param header
	 *            Contents of the Authorization header.
	 * @return A map with the username and password if found, null if no Basic authentication found or an empty map if decoding fails.
	 * 
	 */
	public static Map<String, String> getBasicCredentials(String header) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: getBasicCredentials(%s)", (header == null) ? "null" : "********"));
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

	/**
	 * Hash a password using the provided salt.
	 * 
	 * @param salt
	 * @param password
	 * @return
	 */
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

	/**
	 * Compare the hashed password against a provided password. To do so, the salt must be extracted from the password hash and a new hash generated.
	 * 
	 * @param hashedPassword	Existing password hash.
	 * @param password	Plain password.
	 * @return	true if passwords match, false otherwise.
	 */
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
