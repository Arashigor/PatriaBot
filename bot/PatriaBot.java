package bot;

import movies.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import java.util.List;
import java.util.concurrent.*;


public final class PatriaBot extends TelegramLongPollingBot {

    private static PatriaBot instance;

    private final List<OnScreenMovie> onScreenMoviesROEN = new CopyOnWriteArrayList<>();
    private final List<OnScreenMovie> onScreenMoviesRU = new CopyOnWriteArrayList<>();
    private final List<SoonMovie> soonMoviesROEN = new CopyOnWriteArrayList<>();
    private final List<SoonMovie> soonMoviesRU = new CopyOnWriteArrayList<>();

    private static ExecutorService messageSender = new ThreadPoolExecutor(0,10,
            500,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(64));

    private final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

    public static PatriaBot getInstance() {
        if (instance == null) {
            instance = new PatriaBot();
        }
        return instance;
    }

    private PatriaBot() {
        KeyboardRow row0 = new KeyboardRow();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();

        row0.add("НА ЭКРАНАХ [RU]");
        row1.add("PE ECRANE [RO]");
        row2.add("СКОРО [RU]");
        row3.add("ÎN CURÂND [RO]");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row0);
        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(()->{
            try {
                onScreenMoviesROEN.clear();
                onScreenMoviesRU.clear();
                soonMoviesROEN.clear();
                soonMoviesRU.clear();

                Document pageWithMovies = Jsoup.connect("http://patria.md/movies/?mode=normal").timeout(30000).get();
                Elements movies_item =  pageWithMovies.select("div.movies-item");

                for (Element movie_container : movies_item.select("figure")) {

                    String title = movie_container.select("a").attr("title");
                    String movieUrl = movie_container.select("a").attr("href") +
                            "?mode=normal";

                    Document moviePage = Jsoup.connect(movieUrl).timeout(30000).get();
                    String premier = moviePage.select("div.premiere").text()
                            .replace("3D Film","")
                            .replace("Premiera:","");
                    String imageUrl = moviePage.select("div#image-block").select("figure")
                            .select("ul").select("li").select("img").attr("src");

                    Elements showTimesTable = moviePage.select("table");

                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
                    Date premierDate = formatter.parse(premier);
                    Date currentDate = formatter.parse(formatter.format(Calendar.getInstance().getTime()));

                    if (!(premierDate.compareTo(currentDate) > 0)) {
                        Elements tableRows = showTimesTable.select("tbody").select("tr");
                        StringBuilder showTimes = new StringBuilder();
                        for (Element tableData : tableRows.select("td")) {
                            if (tableData.hasClass("cinema") && tableData.text().length() > 0) {
                                showTimes.append("[");
                                showTimes.append(tableData.text());
                                showTimes.append("]\n");
                            } else if (tableData.hasClass("hall")) {
                                showTimes.append(tableData.text());
                                showTimes.append(" ");
                            } else if (tableData.hasClass("time")) {
                                showTimes.append(tableData.text());
                                showTimes.append("\n");
                            }
                        }
                        if (title.contains("(RU)")) {
                            onScreenMoviesRU.add(new OnScreenMovie(title, premierDate, imageUrl, showTimes.toString()));
                        } else if (title.contains("(EN-RO SUB)") || title.contains("RO")) {
                            onScreenMoviesROEN.add(new OnScreenMovie(title, premierDate, imageUrl, showTimes.toString()));
                        }

                    } else {
                        if (title.contains("(RU)")) {
                            soonMoviesRU.add(new SoonMovie(title, premierDate, imageUrl));
                        } else if (title.contains("(EN-RO SUB)") || title.contains("RO")) {
                            soonMoviesROEN.add(new SoonMovie(title, premierDate, imageUrl));
                        }
                    }
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }

        },1,770, TimeUnit.MINUTES);
    }

    @Override
    public String getBotToken() {
        return "";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            messageSender.submit(()->{
                String message = update.getMessage().getText();

                switch (message) {
                    case "НА ЭКРАНАХ [RU]" : sendReply(onScreenMoviesRU,update);   break;
                    case "PE ECRANE [RO]"  : sendReply(onScreenMoviesROEN,update); break;
                    case "СКОРО [RU]"      : sendReply(soonMoviesRU,update);       break;
                    case "ÎN CURÂND [RO]"  : sendReply(soonMoviesROEN, update);    break;
                    default                : sendKeyBoard(update);
                }
            });
        }
    }

    private void sendKeyBoard(Update update) {
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId())
                .setText("Select from: ")
                .setReplyMarkup(keyboardMarkup);
        try {
            sendMessage(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendReply(List<? extends Movie> movies, Update update) {
        SendMessage message = new SendMessage();
        SendPhoto sendPhotoRequest = new SendPhoto();

        for (Movie movie : movies) {
            sendPhotoRequest.setChatId(update.getMessage().getChatId())
                    .setPhoto(movie.getImgUrl());
            message.setChatId(update.getMessage().getChatId())
                    .setText(movie.toString());
            try {
                sendPhoto(sendPhotoRequest);
                sendMessage(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "CinemaPatriaBot";
    }

    @Override
    public void onClosing() {

    }
}
