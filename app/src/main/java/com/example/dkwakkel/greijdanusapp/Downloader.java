package com.example.dkwakkel.greijdanusapp;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Downloader {

    private static final String WEBSITE_URL = "https://Xuw.greijdanus.nl/";

    private final ContextActivity context;

    private String cookie;

    public Downloader(ContextActivity context) {
        this.context = context;
    }

    public String getData() {
        String downloadURL = WEBSITE_URL + "_KIND_/Rooster";
        String expectedValue = "<TABLE";
        String response = download(downloadURL, expectedValue);

        if(response != null) {
            ScheduleParser scheduleParser = new ScheduleParser(response);
            Schedule schedule = scheduleParser.getSchedule();
            schedule.toStorage(context);
        }

        return Schedule.fromStorage(context).toString();
    }


    private static class ScheduleParser {
        private final String data;
        private int currentIdx;
        private final Schedule schedule = new Schedule();

        public Schedule getSchedule() {
            return schedule;
        }

        private ScheduleParser(String data) {
            this.data = data;
            this.currentIdx = checkedIndexOf("<TABLE", 0);
            parseDays();
        }

        private void parseDays() {
            currentIdx = checkedIndexOf("<TD rowspan=12 align=\"center\" nowrap=\"1\"><TABLE>", currentIdx);

            String day = getValue("\">", "</font>");
            parseDay(day);
        }

        private void parseDay(String dayData) {
            // System.err.println("Day Data: " + dayData);
            String[] dayWithDate = dayData.substring(3, dayData.length() - 4).split(" ");
            Day day = new Day(dayWithDate[0], dayWithDate[1]);
            schedule.days.add(day);

            try {
                for (int i = 0; i < 40; ++i) {
                    String colspan = getValue("<TD colspan=", " rowspan=12 align=\"center\" nowrap=\"1\"><TABLE><TR><TD");
                    String value = getValue("\">", "</font>");
                    if (value.startsWith("<B>")) {
                        parseDay(value);
                        return;
                    }
                    String subject = getValue("\">", "</font>");
                    String teacher = getValue("\">", "</font>");

                    for (int iCol = 0; iCol < Integer.valueOf(colspan) / 2; ++iCol) {
                        // System.err.print(" Location:" + value + " Subject:" + subject + " Teacher:" + teacher);
                        Lesson lesson = new Lesson(value, subject, teacher);
                        day.lessons.add(lesson);
                    }
                    // System.err.println("");
                }
            } catch (EndOfDataException e) {
                if (!day.name.equals("Vrijdag")) {
                    throw e;
                }
            }
        }

        private String getValue(String startMarker, String endMarker) {
            currentIdx = checkedIndexOf(endMarker, currentIdx + 1);
            int startIdx = checkedLastIndexOf(startMarker, currentIdx);
            return data.substring(startIdx + startMarker.length(), currentIdx).trim();
        }

        interface IndexOf {
            int getIndex();
        }

        private int checkedIndexOf(final String value, final int idx) {
            return checkedIndexOf(new IndexOf() {
                @Override
                public int getIndex() {
                    return data.indexOf(value, idx);
                }
            }, value, idx);
        }

        private int checkedLastIndexOf(final String value, final int idx) {
            return checkedIndexOf(new IndexOf() {
                @Override
                public int getIndex() {
                    return data.lastIndexOf(value, idx);
                }
            }, value, idx);
        }

        private int checkedIndexOf(IndexOf function, String value, int idx) {
            try {
                int foundIdx = function.getIndex();
                if (foundIdx == -1) {
                    throw new EndOfDataException("Could not find '" + value + "' from idx " + idx + ". Data:\n" + data.substring(idx));
                }
                return foundIdx;
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalStateException("Unexpected exception at idx " + idx + ". Data:\n" + data.substring(idx), e);
            }
        }

    }

    @SuppressWarnings("serial")
    private static class EndOfDataException extends RuntimeException {
        public EndOfDataException(String string) {
            super(string);
        }
    }

    private String download(String downloadURL, String expectedValue) {
        int nTries = 3;
        while (true) {
            try {
                --nTries;
                doLogin();

                String response = doDownload(downloadURL, null);
                if (response.contains(expectedValue)) {
                    return response;
                }
                Log.w("Download", "Download did not contain '" + expectedValue + ": " + downloadURL);
            } catch (Exception e) {
                if (nTries == 0) {
                    Log.e("Download", "Error during download of: " + downloadURL, e);
                    return null;
                }
                Log.w("Download", "Error during download of: " + downloadURL, e);
            }
            sleep();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private void doLogin() throws IOException {
        cookie = null;

        doDownload(WEBSITE_URL, null);

        String postData = "wu_loginname=" + URLEncoder.encode(context.getUsername()) + "&wu_password=" + URLEncoder.encode(context.getPassword()) + "&Login=Inloggen&path=%2F%3Fgonext%3D1";
        doDownload(WEBSITE_URL + "Login?passAction=login", postData);
    }

    private String doDownload(String url, String postData) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        setHeader(connection, "Accept-Charset", "UTF-8");
        connection.setDoOutput(true);

        setHeader(connection, "Connection", "keep-alive");
        setHeader(connection, "Cache-Control", "max-age=0");
        setHeader(connection, "Origin", "https://uw.greijdanus.nl");
        setHeader(connection, "Upgrade-Insecure-Requests", "1");
        setHeader(connection, "User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36");
        setHeader(connection, "Content-Type", "application/x-www-form-urlencoded");
        setHeader(connection, "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        setHeader(connection, "Referer", WEBSITE_URL + "Login?path=%2F%3Fgonext%3D1");
        // TODO: use compression? : setHeader(connection, "Accept-Encoding", "gzip, deflate, br");
        setHeader(connection, "Accept-Language", "nl-NL,nl;q=0.8,en-US;q=0.6,en;q=0.4");
        if (cookie != null) {
            setHeader(connection, "Cookie", cookie);
        }
        setHeader(connection, "_gat_newTracker=1", "_ga=GA1.3.210010078.1477862144; _gat_newTracker2=1");

        if (postData != null) {
            connection.setRequestMethod("POST");
            connection.setDoInput(true);

            OutputStream output = connection.getOutputStream();
            try {
                output.write(postData.getBytes());
                output.flush();
            } finally {
                output.close();
            }
        }
        if(connection.getResponseCode() != 200) {
            Log.e("Download", readStream(connection.getErrorStream()));
        }

        List<String> setCookies = connection.getHeaderFields().get("Set-Cookie");
        if (setCookies != null) {
            for (String setCookie : setCookies) {
                String cookieValue = setCookie.split(";")[0];
                if (cookie == null) {
                    cookie = cookieValue;
                } else {
                    cookie += "; " + cookieValue;
                }
            }
        }

        return readStream(connection.getInputStream());
    }

    private String readStream(InputStream inputStream) throws IOException {
        try {
            Scanner scanner = new Scanner(inputStream, "UTF-8");
            try {
                return scanner.useDelimiter("\\A").next();
            } finally {
                scanner.close();
            }
        } finally {
            inputStream.close();
        }
    }

    private static void setHeader(URLConnection connection, String key, String value) {
        connection.setRequestProperty(key, value);
    }

    public static class Schedule implements Serializable {
        private static final long serialVersionUID = 1L;
        private static final String FILENAME = "schedule.serialized";

        final List<Day> days = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("Schedule:\n");
            for (Day day : days) {
                result.append(day).append(": ").append(day.lessons).append('\n');
            }
            return result.toString();
        }

        public void toStorage(Context applicationContext) {
            try {
                FileOutputStream fos = applicationContext.openFileOutput(FILENAME, Context.MODE_PRIVATE);
                try {
                    ObjectOutputStream os = new ObjectOutputStream(fos);
                    try {
                        os.writeObject(this);
                    } finally {
                        os.close();
                    }
                } finally {
                    fos.close();
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        public static Schedule fromStorage(Context applicationContext) {
            try {
                FileInputStream fis = applicationContext.openFileInput(FILENAME);
                try {
                    ObjectInputStream is = new ObjectInputStream(fis);
                    try {
                        return (Schedule) is.readObject();
                    } finally {
                        is.close();
                    }
                } finally {
                    fis.close();
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static class Day implements Serializable {
        private static final long serialVersionUID = 1L;

        final String name;
        final String date;
        final List<Lesson> lessons = new ArrayList<>();

        public Day(String name, String date) {
            this.name = name;
            this.date = date;
        }

        @Override
        public String toString() {
            return name + ' ' + date;
        }
    }

    public static class Lesson implements Serializable {
        private static final long serialVersionUID = 1L;

        final String location;
        final String subject;
        final String teacher;

        public Lesson(String location, String subject, String teacher) {
            this.location = location;
            this.subject = subject;
            this.teacher = teacher;
        }

        @Override
        public String toString() {
            return location + ' ' + subject + ' ' + teacher;
        }
    }

}
