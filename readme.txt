This is a Java application I made for my Java 101 class. I demonstrate the use of both the twitter and Spotify APIs by inheriting from the following libraries jspot (for Spotify) and twitter4j (for Twitter). The app is called Tweetify and allows a client (from the command line) to authorize the app to access his/her twitter account and tweet a Spotify link to a track that they have chosen by choosing their desired artist and track.

If you want to pull/fork this make sure to fill out the twitter4j.properties file with your own consumer key and secret by creating an twitter app (dev.twitter.com).

The implementation for this app occurs in the Tweetify/src/tweetify/App.java. The rest of the files inside the tweetify package are part of the jspot library.