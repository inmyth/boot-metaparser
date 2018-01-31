package exchange.mr.metaparser.model;

public class MyAmount {
	
	public final String currency, counterparty, value;

	public MyAmount(String currency, String counterparty, String value) {
		this.currency = currency;
		this.counterparty = counterparty;
		this.value = value;
	}
	
	

}
