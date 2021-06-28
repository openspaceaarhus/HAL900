package dk.dren.hal.ctrl.halclient;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.List;

public class SessionCookieJar implements CookieJar {
    private List<Cookie> cookies;

    @Override
    public void saveFromResponse(HttpUrl httpUrl, List<Cookie> cookies) {
        this.cookies = new ArrayList<>(cookies);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        if (cookies != null) {
            return cookies;
        } else {
            return new ArrayList<>();
        }
    }
}
