package exchange.mr.metaparser.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ripple.core.coretypes.Amount;

import exchange.mr.metaparser.parser.RLOrder;

public class Trade {

	public final String hash;
	public final String date;
	public final String ledger_index;

	public final List<SimpleOffer> data = new ArrayList<>();
  
	@JsonIgnore 
	public List<RLOrder> dirty = new ArrayList<>();
	
	public Trade(String hash, String date, String ledger_index) {
		super();
		this.hash = hash;
		this.date = date;
		this.ledger_index = ledger_index;
	}

	public void addData(SimpleOffer a) {
		data.add(a);
	}
	
	public List<RLOrder> getDirty() {
		return dirty;
	}
	
	
}
