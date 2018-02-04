package exchange.mr.metaparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan("exchange.mr.metaparser")
public class App 
{
    public static void main( String[] args )
    {
  		ApplicationContext ctx = SpringApplication.run(App.class, args);
      System.out.println( "Metaparser Ready!" );
    }
}
