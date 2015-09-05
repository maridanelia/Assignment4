import java.security.AllPermission;
import java.security.MessageDigest;
import java.util.Arrays;

public class Cracker {
	// Array of chars used to produce strings
	private static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789.,-!";
	public static final char[] CHARS = ALLOWED_CHARS
			.toCharArray();
	public static final byte [] CHARS_BYTES = ALLOWED_CHARS.getBytes();
	
	private boolean debugMode = false;
	private static final String HASHING_ALGO = "SHA";
	private volatile int passLength;
	private volatile byte [] hashToCrack;

	/*
	 * Given a byte[] array, produces a hex String, such as "234a6f". with 2
	 * chars for each byte in the array. (provided code)
	 */
	public static String hexToString(byte[] bytes) {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			int val = bytes[i];
			val = val & 0xff; // remove higher bits, sign
			if (val < 16)
				buff.append('0'); // leading 0
			buff.append(Integer.toString(val, 16));
		}
		return buff.toString();
	}

	/*
	 * Given a string of hex byte values such as "24a26f", creates a byte[]
	 * array of those values, one byte value -128..127 for each 2 chars.
	 * (provided code)
	 */
	public static byte[] hexToArray(String hex) {
		byte[] result = new byte[hex.length() / 2];
		for (int i = 0; i < hex.length(); i += 2) {
			result[i / 2] = (byte) Integer
					.parseInt(hex.substring(i, i + 2), 16);
		}
		return result;
	}

	// possible test values:
	// a 86f7e437faa5a7fce15d1ddcb9eaeaea377667b8
	// fm adeb6f2a18fe33af368d91b09587b68e3abcb9a7
	// a! 34800e15707fae815d7c90d49de44aca97e2d759
	// xyz 66b27417d37e024c46526c2f6d358a754fc552f3

	/**
	 * Generates hash string based in given string.
	 */
	public static String generateHash(String str) {
		try {
			//System.out.println(Arrays.toString(str.getBytes()));
			byte[] hash = generateHash(str.getBytes(),
					MessageDigest.getInstance(HASHING_ALGO));
			//System.out.println(Arrays.toString(hash));
			return hexToString(hash);
		} catch (Exception e) {
			return null;
		}

	}

	private static byte[] generateHash(byte[] str, MessageDigest md) {
		md.update(str);
		return md.digest();
	}

	/**
	 * prints all strings that have same hash value as passed hash.
	 */
	public void crack(String hash, int numThreads, int length) {
		int l = CHARS.length;

		int bucketSize = l / numThreads;

		int oddBuckets = l % numThreads;
		
		passLength = length;
		hashToCrack = hexToArray(hash);
		for (int i = 0; i < l; i += bucketSize) {
			CrackerThread thr = new CrackerThread(i, i + bucketSize - 1);
			if (oddBuckets > 0) {
				thr.rangeEnd++;
				i++;
				oddBuckets--;
			}
			if (debugMode) {
				System.out.println("Thread Range: " + thr.rangeStart + " "
						+ thr.rangeEnd);
			}
			thr.start();
		}
	}

	private class CrackerThread extends Thread {
		private int rangeStart;
		private int rangeEnd;
		private byte [] solution;
		private MessageDigest md;
		public CrackerThread(int rangeStart, int rangeEnd) {
		
			super();
			this.rangeStart = rangeStart;
			this.rangeEnd = rangeEnd;
			solution = new byte[passLength];
			try {
				this.md = MessageDigest.getInstance(HASHING_ALGO);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

		@Override
		public void run() {
			for(int i=rangeStart; i<=rangeEnd; i++){
				solution[0] = CHARS_BYTES[i];
				rec(1);
			}
		}
		
		private void rec(int cur){
			if(cur >= solution.length) {
				//System.out.print(Arrays.toString(solution)+" ");
				byte [] curHash = generateHash(solution, this.md);
				//System.out.println(Arrays.toString(curHash));
				//System.out.println(Arrays.toString(hashToCrack));
				if(Arrays.equals(curHash, hashToCrack)){
					printSolution();
				}
				return;
			}
			
			for(int i=0; i<CHARS_BYTES.length; i++){
				solution[cur] = CHARS_BYTES[i];
				rec(cur+1);				
			}
		}
		
		
		private void printSolution(){
			System.out.println(new String(solution));
		}
	}

	public static void main(String args[]) {
		String str = "lrtteoss";
		
		String hash = generateHash(str);
		System.out.println(hash);
		Cracker cr = new Cracker();
		cr.crack(hash, 4, str.length());
		
	}
}
