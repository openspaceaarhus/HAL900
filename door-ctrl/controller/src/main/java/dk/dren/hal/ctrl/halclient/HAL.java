package dk.dren.hal.ctrl.halclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dk.dren.hal.ctrl.storage.State;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * HAL API for the bits we need
 */
@Log
@RequiredArgsConstructor
public class HAL {
    private final URI uri;
    private final String user;
    private final String password;

    @Getter(lazy = true)
    private final Retrofit retrofit = createRetrofit();

    @Getter(lazy = true)
    private final HALApi api = createHalApi();

    @SneakyThrows
    private Retrofit createRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(uri.toURL())
                .addConverterFactory(JacksonConverterFactory.create(new ObjectMapper(new YAMLFactory())))
                .client(
                        new OkHttpClient().newBuilder()
                                .cookieJar(new SessionCookieJar()).build())
                .build();
    }

    private HALApi createHalApi() {
        return getRetrofit().create(HALApi.class);
    }

    public void login() throws IOException {
        final Call<ResponseBody> login = getApi().login(user, password);
        final Response<ResponseBody> response = login.execute();
        if (response.code() != 200 || !response.raw().request().url().toString().endsWith("/hal/account")) {
            throw new IOException("Failed to log on: "+response);
        }
    }

    public boolean sendEvents(byte[] contentToSend) throws IOException {
        final RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), contentToSend);
        final Response<ResponseBody> response = getApi().events(requestBody).execute();
        if (!response.isSuccessful()) {
            return false;
        }
        final String responseBody = response.body().string();
        if (responseBody.equals("Ok")) {
            return true;
        }

        log.warning("HAL did not accept the log entries: "+responseBody);
        return false;
    }

    public State state() throws IOException {
        return getApi().state().execute().body();
    }
}
