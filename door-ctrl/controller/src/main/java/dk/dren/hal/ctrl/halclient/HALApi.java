package dk.dren.hal.ctrl.halclient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;

public interface HALApi {

    @POST("/hal/login?gogogo=1")
    Call<ResponseBody> login(@Query("username")String user, @Query("passwd")String password);

    @GET("/hal/admin/api/users")
    Call<List<HalUser>> users();
}
