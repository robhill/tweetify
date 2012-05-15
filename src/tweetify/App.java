package tweetify;

import java.io.IOException;
import java.util.Scanner;
import javax.swing.JOptionPane;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
This is the implementation class for my app, Tweetify (a play on Spotify and Twitter),
which uses a small java library called jspot to pull Artist, Album, and Track data from Spotify's
API. Based on a user query, Spotify's API returns a unique ID for a given track as requested
by the user and then tweets a URL to a Spotify page where the track can be played if the
Spotify client is installed.
 */
public class App {

    public static void main(String[] args) throws TwitterException, IOException {
        
        System.out.println("WELCOME TO TWEETIFY!" 
                + "\n Follow the instructions below to begin \n");
        Scanner input = new Scanner(System.in);
        TwitterFactory tf = new TwitterFactory();
        Twitter twitter = tf.getInstance();
        
        // ask Twitter for a request token.
        RequestToken reqToken = twitter.getOAuthRequestToken();
        AccessToken accessToken = null;
        while (accessToken == null) {
            // If the user authorizes, then Twitter provides a PIN.
            System.out.print("\nOpen this URL in a browser: " +
                    "\n    " + reqToken.getAuthorizationURL() + "\n" +
                    "\nAuthorize the app, then enter the PIN here: ");
            String pin = input.nextLine();

            try {

                // use the provided PIN to get the access token.
                accessToken = twitter.getOAuthAccessToken(reqToken, pin);

            } catch (TwitterException te) {

                System.out.println(te.getMessage());
            }
        }
        System.out.println("Success!");
        
        // new Spotify object to perform operations and return metadata from API
        Spotify spotify = new Spotify();
        String artist = "";
        String track = "";
        Results<Track> results = null;
        boolean okayQuery = true;
        while (okayQuery) {
            // ask user for desired artist and track
            System.out.println();
            System.out.println();
            System.out.print("Enter desired Artist:  ");
            artist = input.nextLine();
            System.out.println();
            System.out.print("Enter desired Track:  ");
            track = input.nextLine();

            if (spotify.searchTrack(artist, track).getItems().size() <= 0) {
                System.out.println();
                System.out.println("Spotify could not find this artist or track. "
                        + "Please try a different search or check your spelling.");
            }
            else
                okayQuery = false;    
        }
        
        // return a list of results given the chosen artist and track
        results = spotify.searchTrack(artist, track);
        // pick the first, and by default most popular, result
        String songID = getSongID(results);
        
        // Store a string with desired URL to get the track
        String tweet = "I've been tweetified: " + "http://open.spotify.com/track/" + songID;
        // tweet the string to the user's account
        Status status = twitter.updateStatus(tweet);
        System.out.println();
        System.out.println("Just tweeted: \"" + status.getText() + "\"");
    }
    
    // isolate and return the unique song ID to be put in a URL
    public static String getSongID (Results<Track> results)
	{
		for (Track track : results.getItems()) { 
	       		track.getId();
		}
		String [] songID = results.getItems().get(0).getId().split(":", 3);
		return songID[2]; // this is the unique song ID
	}
}