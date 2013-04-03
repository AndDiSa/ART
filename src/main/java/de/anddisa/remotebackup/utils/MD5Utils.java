package de.anddisa.remotebackup.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {

	/**
	 * calculates the md5 sum for the file passed as srcFileName and
	 * compares it to the md5sum stored in md5FileName
	 * 
	 * @param srcFileName {@link String} path to the file for which md5 needs to be calculated
	 * @param md5FileName {@link String} path to the file which contains the md5sum to be matched
	 * @return {@link Boolean} true if md5sum matches, false otherwise
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static boolean compareMD5(String srcFileName, String md5FileName) throws IOException, NoSuchAlgorithmException {
		String md5sum = md5sum(srcFileName);
	    File f = new File(md5FileName);
	
	    //
	    // md5sum is written within the first 32 byte, ignore the rest
	    //
	    InputStream is = new FileInputStream(f);
	    byte[] buffer = new byte[32];
	    is.read(buffer);
	    String s = new String(buffer);
	    is.close();
	
	    boolean result = md5sum.equals(s);
	    if (!result) {
	    	// print out some error message
	    	System.err.println(srcFileName + "(" + md5sum + ") <-> " + md5FileName + "(" + s + ")");
	    }
	    return result;
	}

	/**
	 * calculates the md5 sum of a file
	 * 
	 * @param fileName {@link String} path to the file for which md5 needs to be calculated
	 * @return md5sum {@link String} of the file
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static String md5sum(String fileName) throws IOException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
	
		// read file and calculate md5
		File f = new File(fileName);
	    InputStream is = new FileInputStream(f);
	    byte[] buffer = new byte[8192];
	    int read = 0;
	    while( (read = is.read(buffer)) > 0)
	            md.update(buffer, 0, read);
	    is.close();
	
	    // format md5
	    byte[] md5 = md.digest();
	    BigInteger bi = new BigInteger(1, md5);
	    
	    return bi.toString(16);	// print out as HEX number
	}

}
