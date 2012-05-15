package tweetify;

public class Artist extends SpotifyItem {

    Artist(String id, String name, float popularity) {
        super(id, name, popularity);
    }

    Artist(String id, String name) {
        super(id, name);
    }

}
