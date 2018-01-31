package exchange.mr.metaparser.model;

import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;

public class SimpleAmount {
	
	public final String currency, counterparty, value;

	public SimpleAmount(Amount a) {
		currency = a.currencyString();
		counterparty = !currency.equals(Currency.XRP.toString()) ?  a.issuerString() : null;
		value = a.valueText();
		
	}
	
	
	
	

}
