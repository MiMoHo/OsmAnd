package net.osmand.plus.plugins.monitoring.live;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// Encrypts a live track location exactly like the web client (util/livetracks/liveTrackCrypto.js):
// AES-256-GCM, 12-byte random IV prepended to the ciphertext, the whole thing Base64-encoded.
// The key is a 64-char hex string; the plaintext is the JSON of the location point.
class LiveTrackCrypto {

	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;
	private static final SecureRandom RANDOM = new SecureRandom();

	@Nullable
	static String encrypt(@NonNull String keyHex, @NonNull LiveMonitoringData data) {
		try {
			JSONObject point = new JSONObject();
			point.put("lat", data.lat);
			point.put("lon", data.lon);
			point.put("time", data.time);
			point.put("speed", data.speed);
			point.put("ele", data.alt);
			point.put("hdop", data.hdop);
			point.put("bearing", data.bearing);
			point.put("tta", data.timeToArrival);                  // time to arrival/finish (ms)
			point.put("ttf", data.timeToIntermediateOrFinish);     // time to intermediate (ms)
			point.put("dta", data.distanceToArrivalOrMarker);      // distance to arrival/marker (m)
			point.put("dtf", data.distanceToIntermediateOrFinish); // distance to intermediate (m)
			point.put("battery", data.battery);                    // battery %
			byte[] plaintext = point.toString().getBytes(StandardCharsets.UTF_8);

			byte[] iv = new byte[IV_BYTES];
			RANDOM.nextBytes(iv);

			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(hexToBytes(keyHex), "AES"),
					new GCMParameterSpec(TAG_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext);

			byte[] combined = new byte[iv.length + ciphertext.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
			return Base64.encodeToString(combined, Base64.NO_WRAP);
		} catch (Exception e) {
			return null;
		}
	}

	private static byte[] hexToBytes(@NonNull String hex) {
		int len = hex.length();
		byte[] out = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
		}
		return out;
	}
}
