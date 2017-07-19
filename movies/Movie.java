package movies;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Movie {
    private String title;
    private Date premier;
    private String imgUrl;

    Movie(String title, Date premier, String imgUrl) {
        this.title = title;
        this.premier = premier;
        this.imgUrl = imgUrl;
    }

    String getTitle() {
        return title;
    }

    String getPremier() {
        return "Premiera: " + new SimpleDateFormat("dd.MM.yyyy").format(premier);
    }

    public String getImgUrl() {
        return imgUrl;
    }

    @Override
    public String toString() {
        return title + "\n" + getPremier();
    }
}
