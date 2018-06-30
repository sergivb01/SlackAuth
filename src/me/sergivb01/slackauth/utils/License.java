package me.sergivb01.slackauth.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.MessageDigest;

public class License{

	public static boolean checkLicense(){
		try{
			final URLConnection openConnection = new URL("https://pastebin.com/raw/GyiFtnSz").openConnection();
			openConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
			openConnection.connect();
			final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(openConnection.getInputStream(), Charset.forName("UTF-8")));
			final StringBuilder sb = new StringBuilder();
			String line;
			while((line = bufferedReader.readLine()) != null){
				sb.append(line);
			}
			return sb.toString().contains(getHWID());
		}catch(IOException ex){
			ex.printStackTrace();
			return false;
		}
	}

	public static String getHWID(){
		String s = "";
		final String main = System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("COMPUTERNAME") + System.getProperty("user.name").trim();
		byte[] bytes = new byte[0];
		MessageDigest messageDigest = null;
		try{
			bytes = main.getBytes("UTF-8");
			messageDigest = MessageDigest.getInstance("MD5");
		}catch(Exception ex){
			ex.printStackTrace();
		}
		final byte[] md5 = messageDigest.digest(bytes);
		int i = 0;
		for(final byte b : md5){
			s += Integer.toHexString((b & 0xFF) | 0x300).substring(0, 3);
			if(i != md5.length - 1){
				s += "-";
			}
			i++;
		}
		return s;
	}

}
