import java.io.*;
import java.net.*;
import java.util.*;
import redis.clients.jedis.Jedis;

public class tinyURL {

	static final String longUrlToShortUrlKey = "long_urls_to_short_urls";
	static final String shortUrlToLongUrlKey = "short_urls_to_long_urls";
	static final String shortUrlToUsernameKey = "short_url_to_username";
	static final String shortUrlCount = "short_url_count";
	static final String usernameUrlCount = "username_short_url_count";
	static Random rnd = new Random();

	public static String randomString( String long_url ,int len ) {
		StringBuilder sb = new StringBuilder( len );
		for( int i = 0; i < len; i++ ) 
			sb.append( long_url.charAt( rnd.nextInt(long_url.length()) ) );
			return sb.toString();
	}

	public static Boolean checkIfLongUrlExists (String long_url, Jedis jedis) {
		return jedis.hexists(longUrlToShortUrlKey, long_url);
	}

	public static void main (String [] args) throws Exception {

		String replyFromUser;
		Jedis jedis = new Jedis();

		// read from the input
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); 
		System.out.println("Please enter your username:");
		String username = inFromUser.readLine();

		while (true){
			System.out.println("(I)nsert a long URL | (Q)uery a short URL | (S)tatistics | e(X)it");
			replyFromUser = inFromUser.readLine(); //read the reply

			// Insert
			if (replyFromUser.equals("I")) {	
				System.out.println("Please provide a long URL");
				String input_user_long_url = inFromUser.readLine();
				if (checkIfLongUrlExists(input_user_long_url, jedis)){
					System.out.println("The long URL " + input_user_long_url + " already exists!");
				}  
				else {
					String shortened_url = randomString(input_user_long_url, 6);
					System.out.println("The shortened url is " + shortened_url);
					jedis.hset(longUrlToShortUrlKey, input_user_long_url, shortened_url);
					jedis.hset(shortUrlToLongUrlKey, shortened_url, input_user_long_url);
					jedis.hset(shortUrlToUsernameKey, shortened_url, username);
					jedis.hincrBy(usernameUrlCount, username, 1);
				}
			} 
			// Query
			else if (replyFromUser.equals("Q")) {
				System.out.println("Please provide a short URL");
				String input_user_short_url = inFromUser.readLine();
				String long_url_from_redis = jedis.hget(shortUrlToLongUrlKey, input_user_short_url);
				if(long_url_from_redis != null){
					System.out.println("The long URL is: " + long_url_from_redis);
					jedis.hincrBy(shortUrlCount, input_user_short_url, 1);
				} 
				else {
					System.out.println("The short URL does not exist!");
				}
			} 
			// Statistics
			else if (replyFromUser.equals("S")) {
				// Print entries per user
				Map<String, String> inputs_per_user = jedis.hgetAll(usernameUrlCount);
				if(inputs_per_user.isEmpty()){
					System.out.println("No users have created any short URLs!");
				}
				else{
					System.out.println("User statistics:");
					for (Map.Entry<String, String> entry : inputs_per_user.entrySet()) {
						String stat_username = entry.getKey();
						String stat_count = entry.getValue();
						System.out.println("User: " + stat_username + " has created " + stat_count + " URLs");
					}
				}
				// Print average of URL requests
				Map<String, String> count_per_url = jedis.hgetAll(shortUrlCount);
				if(count_per_url.isEmpty()){
					System.out.println("There are no visits to any short URLs");
				}
				else {
					int sum = 0;
					for (String value : count_per_url.values()) {
						int intValue = Integer.parseInt(value);
						sum += intValue;
					}
					double average = (double) sum / count_per_url.size();
					System.out.println("Average of shortened URL visits: " + average);
				}
			} 
			// Exit
			else if (replyFromUser.equals("X")) {
				System.out.println("Goodbye");
				jedis.close();
				System.exit(1);
			} 
			// Invalid
			else {
				System.out.println(replyFromUser + "is not a valid choice, retry");
			}
		}
	}
}
