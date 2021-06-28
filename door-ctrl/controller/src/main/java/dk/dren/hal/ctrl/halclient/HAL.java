package dk.dren.hal.ctrl.halclient;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
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
                .addConverterFactory(JacksonConverterFactory.create())
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

    public List<HalUser> users() throws IOException {
        final Call<List<HalUser>> request = getApi().users();
        final Response<List<HalUser>> response = request.execute();
        return response.body();
    }



}
