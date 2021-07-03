package dk.dren.hal.ctrl.halclient;

import dk.dren.hal.ctrl.storage.DeviceState;
import dk.dren.hal.ctrl.storage.State;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;

public interface HALApi {

    @POST("/hal/login?gogogo=1")
    Call<ResponseBody> login(@Query("username")String user, @Query("passwd")String password);

    @POST("/hal/api/events")
    Call<ResponseBody> events(@Body RequestBody body);

    @GET("/hal/api/state")
    Call<State> state();

    @POST("/hal/api/createDevices")
    Call<ResponseBody> createDevices(@Body List<DeviceState> devices);
}
