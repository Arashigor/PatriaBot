package movies;

import java.text.SimpleDateFormat;
import java.util.Date;

abstract class Movie implements Showable {
    private String title;
    private Date premier;
    private String imgUrl;

    Movie(String title, Date premier, String imgUrl) {
        this.title = title;
        this.premier = premier;
        this.imgUrl = imgUrl;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getPremier() {
        return "Premiera: " + new SimpleDateFormat("dd.MM.yyyy").format(premier);
    }

    @Override
    public String getImgUrl() {
        return imgUrl;
    }

    @Override
    public String toString() {
        return title + "\n" + getPremier();
    }
}
