package exchange.mr.metaparser.web;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import exchange.mr.metaparser.model.Trade;
import exchange.mr.metaparser.service.DataapiService;

@RestController
public class DataapiController {

	@Autowired
	private DataapiService service;
	
	@RequestMapping("/accounts/{account}/transactions")
	@ResponseBody
	public List<Trade> accountTx(
			@PathVariable String account,
			@RequestParam String result,
			@RequestParam int limit, 
			@RequestParam String start,
			@RequestParam String end,
			@RequestParam boolean descending,
			@RequestParam String host,
			@RequestParam Optional<String> marker
			) throws IOException 
	{
		
		return service.webPipe(host, account, result, limit, start, end, descending, marker);
		
	}
	
  @RequestMapping("/test3")
  public
  @ResponseBody
  WebAsyncTask<String> handleRequest (HttpServletRequest r) {
      System.out.println("asyncSupported: " + r.isAsyncSupported());
      System.out.println(Thread.currentThread().getName());

      Callable<String> callable = () -> {
          System.out.println(Thread.currentThread().getName());
          Thread.sleep(10000l);
          return "WebAsyncTask test";
      };

      ConcurrentTaskExecutor t = new ConcurrentTaskExecutor(
                Executors.newFixedThreadPool(1));
      return new WebAsyncTask<>(10000L, t, callable);
  }
}
