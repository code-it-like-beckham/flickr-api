package sanghoon.flickr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApiKey {

	public static ApiKey load(InputStream inputStream) {
		
		Properties prop = new Properties();
		try {
			prop.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		ApiKey key = new ApiKey();
		
		key.key = prop.getProperty("KEY");
		key.secret = prop.getProperty("SECRET");
		
		return key;
	}
	
	private String key;
	private String secret;

	public String getKey() {
		return key;
	}

	public String getSecret() {
		return secret;
	}

}