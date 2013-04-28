package de.anddisa.remotebackup.utils;

import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

public class MD5UtilsTest {

	@Test
	public void test() {
		try {
			Assert.assertEquals("", MD5Utils.md5sum("src/test/resources/testfile"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void test2() {
		try {
			StringWriter sw = new StringWriter();
			for (int i = 0; i < 1000; i++) {
				System.out.println(MD5Utils.md5sumFromString(sw.toString()));
				sw.write("0");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
