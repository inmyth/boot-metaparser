package exchange.mr.metaparser.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import exchange.mr.metaparser.model.SimpleAmount;
import exchange.mr.metaparser.model.SimpleOffer;
import exchange.mr.metaparser.model.Trade;
import exchange.mr.metaparser.parser.Common;
import exchange.mr.metaparser.parser.RLOrder;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.fastjson.FastJsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

@Component
public class DataapiService {
  public interface DataapiInterface {
	
    @GET("/v2/accounts/{account}/transactions")
    Call<JSONObject> getAccountTx(
    		  @Path("account") String account,
        @Query("result") String result,
        @Query("limit") int limit,
        @Query("start") String start,
        @Query("end") String end,
        @Query("descending") boolean descending,
        @Query("marker") String marker
    );
  }

	private static DataapiInterface makeWebRest(String url) {
//		Gson gson = new GsonBuilder().setLenient().create();

		Retrofit retrofit = new Retrofit.Builder().baseUrl(url)
//				.addConverterFactory(GsonConverterFactory.create(gson))
			  .addConverterFactory(FastJsonConverterFactory.create())
				.build();

		DataapiInterface rest = retrofit.create(DataapiInterface.class);
		return rest;
	}
	
	
	public List<Trade> webPipe(String host, String account, String result, int limit, String start, String end, boolean descending, Optional<String> marker) throws IOException {
		DataapiInterface rest = makeWebRest("https://"+ host);
		Call<JSONObject> call = rest.getAccountTx(account, result, limit, start, end, descending, marker.isPresent() ? marker.get() : null);
		JSONObject json = call.execute().body();
		
		return json.getJSONArray("transactions").stream()
		.map(r -> {
			return ((JSONObject) r);
		})
		.map(r -> {
			String hash = r.getString("hash");
			String ledgerIndex = r.getString("ledger_index");
			String date = r.getString("date");
			Trade t = new Trade(hash, date, ledgerIndex);
//			org.json.JSONObject metaJson = new org.json.JSONObject(r.getJSONObject("meta").toJSONString());
//			t.dirty.addAll(Common.filterOfferExecuted(metaJson, account));
			org.json.JSONObject transaction = new org.json.JSONObject(r.toJSONString());
			t.dirty.addAll(Common.filterOfferExecuted(transaction, account));
			return t ;
		})
		.filter(r -> {
			return !r.dirty.isEmpty();
		})
		.map(t -> {			
			for(RLOrder rl : t.dirty) {				
				t.addData(new SimpleOffer(new SimpleAmount(rl.getQuantity().abs()), new SimpleAmount(rl.getTotalPrice().abs())));
			}
			return t;
		})
		.collect(Collectors.toList());
		
		
		
	}
	

	
}
