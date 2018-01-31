package exchange.mr.metaparser.parser;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.fields.Field;
import com.ripple.core.fields.UInt16Field;
import com.ripple.core.serialized.enums.EngineResult;
import com.ripple.core.serialized.enums.LedgerEntryType;
import com.ripple.core.types.known.sle.LedgerEntry;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.result.AffectedNode;
import com.ripple.core.types.known.tx.result.TransactionMeta;

import exchange.mr.metaparser.model.Cpair;
import exchange.mr.metaparser.model.Trade;

public class Common  {


	public static List<RLOrder> filterOfferExecuted(JSONObject transactionJson, String myAddress) {
		List<RLOrder> res = new ArrayList<>();
		
//		if (!raw.contains(myAddress)) {
//			return Optional.empty();
//		}

//		ArrayList<RLOrder>  oes  = new ArrayList<>(); // Offer Executeds
//		ArrayList<BefAf> 	 ors  = new ArrayList<>(); // Before and After
		Offer offerCreated 			= null;
//		JSONObject transaction 	= new JSONObject(raw);

		JSONObject metaJSON = (JSONObject) transactionJson.remove("meta");
		TransactionMeta meta = (TransactionMeta) STObject.fromJSONObject(metaJSON);
//		Transaction txn = (Transaction) STObject.fromJSONObject(transaction.getJSONObject("transaction"));
		
	  Transaction txn = (Transaction) STObject.fromJSONObject(transactionJson.getJSONObject("tx"));

		AccountID txnAccId = txn.account();
//		Hash256 txnHash = txn.hash();
//		String ledgerIndex = txn.lastLedgerSequence().toString();
//		String date = txn.get(Field.CloseTime).toString();
		UInt32 txnSequence = txn.sequence();

		if (meta.engineResult().compareTo(EngineResult.tesSUCCESS) != 0) {
			return res;
		}

//		ArrayList<AffectedNode> deletedNodes 			= new ArrayList<>();
		ArrayList<Offer> offersExecuteds		 			= new ArrayList<>();
//		ArrayList<OfferEdited> ofEditeds 					= new ArrayList<>();
		
		String txType = txn.get(Field.TransactionType).toString();


		
		UInt32 txnOfferSequence = txn.get(UInt32.OfferSequence); // Offer Edit flag, indicates old order's sequence
		
		for (AffectedNode node : meta.affectedNodes()) {
			if (!node.isCreatedNode()) {
				LedgerEntry asPrevious = (LedgerEntry) node.nodeAsPrevious();

				if (node.isDeletedNode()) {
					if (asPrevious instanceof Offer) {
						AccountID nodeAccount  = asPrevious.get(AccountID.Account);
						Hash256 previousTxnId  = asPrevious.get(Hash256.PreviousTxnID);
						UInt32 prevSeq	 			 = asPrevious.get(UInt32.Sequence);
						Offer o 							 = (Offer) asPrevious;
						//FinalFields.Flag can be 0 or  131072, this is unclear
						if (nodeAccount != null && nodeAccount.address.equals(myAddress) && asPrevious.get(Field.LedgerEntryType) == LedgerEntryType.Offer) {
					
							if (node.nested.get(Field.PreviousFields) != null) {
								offersExecuteds.add(o);
							}
						}
					} 

				}			
				else if (node.isModifiedNode()) { // partially filled 
					if (asPrevious instanceof Offer) {
						LedgerEntry le = (LedgerEntry) node.nodeAsFinal();
						AccountID nodeAccount = le.get(AccountID.Account);
						Offer o = (Offer) asPrevious;
						if (nodeAccount != null && nodeAccount.address.equals(myAddress) && le.get(Field.LedgerEntryType) == LedgerEntryType.Offer) {
							offersExecuteds.add(o);
						}
					}
				}
			} 
			else { // createdNode goes here
				LedgerEntry asFinal = (LedgerEntry) node.nodeAsPrevious();
				if (asFinal instanceof Offer) {
					Offer offer = (Offer) asFinal;
					offerCreated = offer;
				}
			}
		}

	
		RLOrder ourOfferCreate = null;
		if (txType.equals("OfferCreate")) {
			RLOrder offerCreate = RLOrder.fromOfferCreate(txn);
//			log("OFFER CREATE Account: " + txnAccId + " Hash " + txnHash + " Sequence " + txnSequence + "\n" + offerCreate.stringify());
			// OnOfferCreate event is only needed to increment sequence.
			if (txn.account().address.equals(myAddress)) {
				ourOfferCreate = offerCreate;
//				bus.send(new OnOfferCreate(txnAccId, txnHash, txnSequence));
			}

			if (txn.account().address.equals(myAddress)) {
				FilterAutobridged fa = new FilterAutobridged();
				for (Offer offer : offersExecuteds) {
					STObject finalFields = offer.get(STObject.FinalFields);
					if (finalFields != null && isTakersExist(finalFields)) {
						fa.push(offer);
					}
				}
				res.addAll(fa.process());
				Amount takerPays = offerCreated != null ? offerCreated.takerPays() : null;
				Amount takerGets = offerCreated != null ? offerCreated.takerGets() : null;
//				ors.add(RLOrder.toBA(txn.get(Amount.TakerPays), txn.get(Amount.TakerGets), takerPays, takerGets, txnSequence, txnHash, ourOfferCreate));
			} 
			else {
				for (Offer offer : offersExecuteds) {
					STObject finalFields = offer.get(STObject.FinalFields);
					UInt32 affectedSeq = offer.get(UInt32.Sequence);
					if (finalFields != null && isTakersExist(finalFields)) {
						res.add(RLOrder.fromOfferExecuted(offer, true));
//						ors.add(RLOrder.toBA(offer.takerPays(), offer.takerGets(), finalFields.get(Amount.TakerPays), finalFields.get(Amount.TakerGets), affectedSeq, txnHash, ourOfferCreate));
					}
				}
			}
		} 
		else if (txType.equals("Payment") && !txn.account().address.equals(myAddress)) {
			// we only care about payment not from ours.
			for (Offer offer : offersExecuteds) {
				STObject finalFields = offer.get(STObject.FinalFields);
				UInt32 affectedSeq = offer.get(UInt32.Sequence);

				if (finalFields != null && isTakersExist(finalFields)) {
					res.add(RLOrder.fromOfferExecuted(offer, true));
				}
			}
		}
		
		return res;
	}

	private static class FilterAutobridged {
		// pretty sure autobridge happens on OE belonging to others
		ArrayList<Offer> cache = new ArrayList<>();
		HashMap<String, ArrayList<Offer>> map = new HashMap<>();

		void push(Offer offer) {
			Cpair cpair = Cpair.newInstance(offer);
			if (map.get(cpair.toString()) == null) {
				map.put(cpair.toString(), new ArrayList<>());
			}
			map.get(cpair.toString()).add(offer);
			cache.add(offer);
		}

		List<RLOrder> process() {
			List<RLOrder> res = new ArrayList<>();
			if (map.size() <= 1) { // No Autobridge
				cache.stream().forEach(oe -> {
					res.add(RLOrder.fromOfferExecuted(oe, false));
				});
				return res;
			}
			res.addAll(RLOrder.fromAutobridge(map));
			return res;
		}
	}

	
	public static boolean isTakersExist(STObject finalFields) {
		Amount testTakerPays = finalFields.get(Amount.TakerPays);
		Amount testTakerGets = finalFields.get(Amount.TakerGets);
		return testTakerGets != null && testTakerPays != null;
	}



	
}