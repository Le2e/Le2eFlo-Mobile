package com.le2e.le2etruckstop.injection.module;

import com.le2e.le2etruckstop.BuildConfig;
import com.le2e.le2etruckstop.data.remote.request.ApiContentHelper;
import com.le2e.le2etruckstop.data.remote.request.TruckServiceApi;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Module(includes = NetworkModule.class)
public class StationServiceModule {
    @Provides
    @Singleton
    TruckServiceApi getServiceApi(OkHttpClient client){
        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .baseUrl(BuildConfig.BASE_URL)
                .build();

        return retrofit.create(TruckServiceApi.class);
    }

    @Provides
    @Singleton
    ApiContentHelper getContentHelper(TruckServiceApi truckServiceApi){
        return new ApiContentHelper(truckServiceApi);
    }
}
