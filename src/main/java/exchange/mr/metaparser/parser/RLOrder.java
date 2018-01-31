package exchange.mr.metaparser.parser;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;

import exchange.mr.metaparser.model.Cpair;
import io.reactivex.annotations.Nullable;

public final class RLOrder {

	public enum Direction {
		BUY("buy"), SELL("sell");

		private String text;

		Direction(String text) {
			this.text = text;
		}

		public String text() {
			return text;
		}
	}

	private final String direction;
	private final Amount quantity;
	private final Amount totalPrice;
	private final boolean passive;
	private final boolean fillOrKill;

	private final BigDecimal rate;
	private final Cpair cpair;

	private RLOrder(Direction direction, Amount quantity, Amount totalPrice, BigDecimal rate, Cpair cpair) {
		this.direction = direction.text;
		this.quantity = amount(quantity);
		this.totalPrice = amount(totalPrice);
		this.passive = false;
		this.fillOrKill = false;
		this.rate = rate;
		this.cpair = cpair;
	}

	public String getDirection() {
		return direction;
	}

	public Amount getQuantity() {
		return quantity;
	}

	public Amount getTotalPrice() {
		return totalPrice;
	}

	public boolean isPassive() {
		return passive;
	}

	public boolean isFillOrKill() {
		return fillOrKill;
	}

	public Cpair getCpair() {
		return cpair;
	}

	public RLOrder reverse() {
		Direction newDirection = this.direction.equals(Direction.BUY.text()) ? Direction.SELL : Direction.BUY;
		String newPair = cpair.toString();
		Amount newQuantity = totalPrice;
		Amount newTotalPrice = quantity;
		BigDecimal rate = newTotalPrice.value().divide(newQuantity.value(), MathContext.DECIMAL64);
		RLOrder res = new RLOrder(newDirection, newQuantity, newTotalPrice, rate, Cpair.newInstance(newPair));
		return res;
	}

	@Nullable
	public BigDecimal getRate() {
		if (rate != null) {
			return rate;
		}
		if (quantity.value().compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}
		return totalPrice.value().divide(quantity.value(), MathContext.DECIMAL128);
	}

	public static Amount amount(BigDecimal value, Currency currency, AccountID issuer) {
		if (currency.isNative()) {
			value = value.round(new MathContext(6, RoundingMode.HALF_DOWN)).setScale(6, RoundingMode.HALF_DOWN);
			return new Amount(value);
		}
		value = value.round(new MathContext(16, RoundingMode.HALF_DOWN)).setScale(16, RoundingMode.HALF_DOWN);
		return new Amount(value, currency, issuer);
	}

	public static Amount amount(Amount amount) {
		return amount(amount.value(), amount.currency(), amount.issuer());
	}

	public static Amount amount(BigDecimal value) {
		return amount(value, Currency.XRP, null);
	}

	/**
	 * Instantiate RLORder where ask rate is not needed or used for log. This
	 * object typically goes to submit or test.
	 * 
	 * @param direction
	 * @param quantity
	 * @param totalPrice
	 * @return
	 */
	public static RLOrder rateUnneeded(Direction direction, Amount quantity, Amount totalPrice) {
		Cpair cpair = direction == Direction.BUY ? Cpair.newInstance(totalPrice, quantity) : Cpair.newInstance(quantity, totalPrice);
		return new RLOrder(direction, quantity, totalPrice, null, cpair);
	}


	public static RLOrder fromOfferCreate(Transaction txn) {
		Amount gets = txn.get(Amount.TakerPays);
		Amount pays = txn.get(Amount.TakerGets);
		Cpair cpair = Cpair.newInstance(pays, gets); // flipped
		RLOrder res = new RLOrder(Direction.BUY, gets, pays, null, cpair);
		return res;
	}

	public static RLOrder fromOfferCreated(Offer offer) {
		BigDecimal ask = askFrom(offer);
		Amount pays = offer.takerPays();
		Amount gets = offer.takerGets();
		Cpair cpair = Cpair.newInstance(gets, pays);
		RLOrder res = new RLOrder(Direction.BUY, gets, pays, ask, cpair);
		return res;
	}

	public static RLOrder fromOfferExecuted(Offer offer, boolean isOCOwn) {
		// All OE's paid and got are negative
		BigDecimal ask = isOCOwn ? BigDecimal.ONE.divide(askFrom(offer), MathContext.DECIMAL64) : askFrom(offer);
		STObject executed = offer.executed(offer.get(STObject.FinalFields));
		Amount paid = executed.get(Amount.TakerPays);
		Amount got = executed.get(Amount.TakerGets);
		Amount rlGot = isOCOwn ? amount(paid.value(), paid.currency(), paid.issuer())
				: amount(got.value(), got.currency(), got.issuer());
		Amount rlPaid = isOCOwn ? amount(got.value(), got.currency(), got.issuer())
				: amount(paid.value(), paid.currency(), paid.issuer());
		Cpair cpair = Cpair.newInstance(isOCOwn ? got : paid, isOCOwn ? paid : got);
		RLOrder res = new RLOrder(Direction.BUY, rlGot, rlPaid, ask, cpair);
		return res;
	}



	public static List<RLOrder> fromAutobridge(Map<String, ArrayList<Offer>> map) {
		List<RLOrder> res = new ArrayList<>();

		ArrayList<Offer> majorities = null;
		ArrayList<Offer> minorities = null;

		for (ArrayList<Offer> offers : map.values()) {
			if (majorities == null && minorities == null) {
				majorities = offers;
				minorities = offers;
			} else {
				if (offers.size() > majorities.size()) {
					majorities = offers;
				} else {
					minorities = offers;
				}
			}
		}

		BigDecimal refAsk = oeAvg(minorities);
		STObject oeExecutedMinor = minorities.get(0).executed(minorities.get(0).get(STObject.FinalFields));
		boolean isXRPGotInMajority = majorities.get(0).getPayCurrencyPair().startsWith(Currency.XRP.toString());

		Direction direction = Direction.BUY;
		for (Offer oe : majorities) {
			STObject oeExecuted = oe.executed(oe.get(STObject.FinalFields));
			BigDecimal newAsk = refAsk.multiply(oe.directoryAskQuality(), MathContext.DECIMAL64);
			Amount oePaid = oeExecuted.get(Amount.TakerPays);
			Amount oeGot = oeExecuted.get(Amount.TakerGets);
			if (!isXRPGotInMajority) {
				Amount oePaidRef = oeExecutedMinor.get(Amount.TakerPays);
				Amount newPaid = new Amount(oePaid.value().multiply(refAsk, MathContext.DECIMAL64), oePaidRef.currency(), oePaidRef.issuer());
				Cpair cpair = Cpair.newInstance(newPaid, oeGot);
				Amount oeGotPositive = new Amount(oeGot.value(), oeGot.currency(), oeGot.issuer());
				res.add(new RLOrder(direction, oeGotPositive, newPaid, newAsk, cpair));
			} else {
				Amount oeGotRef = oeExecutedMinor.get(Amount.TakerGets);
				Amount newGot = new Amount(oeGot.value().divide(refAsk, MathContext.DECIMAL64), oeGotRef.currency(), oeGotRef.issuer());
				Amount oePaidPositive = new Amount(oePaid.value(), oePaid.currency(), oePaid.issuer());
				Cpair cpair = Cpair.newInstance(oePaid, newGot);
				res.add(new RLOrder(direction, newGot, oePaidPositive, newAsk, cpair));
			}
		}
		return res;
	}

	private static BigDecimal oeAvg(ArrayList<Offer> offers) {
		BigDecimal paids = new BigDecimal(0);
		BigDecimal gots = new BigDecimal(0);
		for (Offer oe : offers) {
			STObject executed = oe.executed(oe.get(STObject.FinalFields));
			paids = paids.add(executed.get(Amount.TakerPays).value(), MathContext.DECIMAL128);
			gots = gots.add(executed.get(Amount.TakerGets).value(), MathContext.DECIMAL128);
		}
		return paids.divide(gots, MathContext.DECIMAL64);
	}

	private static BigDecimal askFrom(Offer offer) {
		return offer.directoryAskQuality().stripTrailingZeros();
	}

	private static Amount clearXRPIssuer(Amount in) {
		if (in.issuer() != null && in.issuer().address.equals("rrrrrrrrrrrrrrrrrrrrrhoLvTp")) {
			return new Amount(in.value());
		}
		return in;
	}


}
