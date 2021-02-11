package com.plaid.platform.porcelain.network

import com.plaid.platform.porcelain.Global
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object LinkTokenRequester {
  // This value is setup to work with emulators. Modify this value to your PC's IP address if not.
//  private val baseUrl = " http://ec2-54-234-223-5.compute-1.amazonaws.com:80";
  private val baseUrl = Global.BASE_URL;

  private val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(OkHttpClient.Builder().build())
    .addConverterFactory(GsonConverterFactory.create())
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .build()

  private val api = retrofit.create(LinkSampleApi::class.java)

  /**
   * In production, make an API request to your server to fetch
   * a new link_token. Learn more at https://plaid.com/docs/#create-link-token.
   *
   * You can optionally curl for a link_token instead of running the node-quickstart server,
   * and copy and paste the link_token value here.
   */
  val token
    get() = api.getLinkToken()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .map { it.link_token }

  //Comment out the above and uncomment the below to use a curled Link Token
  //val token
  //  get() = Single.just("<GENERATED_LINK_TOKEN>")

}

