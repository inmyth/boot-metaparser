package exchange.mr.metaparser.model;

import java.util.ArrayList;
import java.util.List;

import com.ripple.core.coretypes.Amount;

import exchange.mr.metaparser.parser.RLOrder;

public class Trade {

	public final String hash;
	public final String date;
	public final String ledger_index;

	public final List<SimpleAmount> pay = new ArrayList<>();
	public final List<SimpleAmount> get = new ArrayList<>();
	public final List<SimpleAmount> fee = new ArrayList<>();
	public List<RLOrder> dirty = new ArrayList<>();
	
	public Trade(String hash, String date, String ledger_index) {
		super();
		this.hash = hash;
		this.date = date;
		this.ledger_index = ledger_index;
	}

	public void addPay(SimpleAmount a) {
		pay.add(a);
	}
	
	public void addGet(SimpleAmount a) {
		get.add(a);
	}
	
	public void addFee(SimpleAmount a) {
		fee.add(a);
	}
	
	public List<RLOrder> getDirty() {
		return dirty;
	}
	
	
}
