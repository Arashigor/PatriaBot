package movies;

import java.util.Date;

public class OnScreenMovie extends Movie {

    private String showTimes;

    public OnScreenMovie(String title, Date premier, String imgUrl, String showTimes) {
        super(title, premier, imgUrl);
        this.showTimes = showTimes;
    }

    public String getShowTimes() {
        return showTimes;
    }

    @Override
    public String toString() {
        return getTitle() + "\n" +
                getPremier() + "\n" + getShowTimes();
    }
}
