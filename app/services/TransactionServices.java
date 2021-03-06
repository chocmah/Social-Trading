package services;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Date;

import play.mvc.Controller;

import models.CommunityQuote;
import models.Portfolio;
import models.PortfolioEntry;
import models.Quote;
import models.PorfolioTransaction;

public class TransactionServices{

	
	public static PorfolioTransaction executeBuyTransaction(Portfolio portfolio, CommunityQuote communityquote, int qty){

		BigDecimal marketPrice = communityquote.quote.marketPrice;
		
		BigDecimal transactionPrice =  marketPrice.multiply(new BigDecimal(qty));
		BigDecimal newBalance = portfolio.balance.subtract(transactionPrice);

		PorfolioTransaction transaction = new PorfolioTransaction(portfolio,communityquote,marketPrice, qty,new Date(),"Buy");

		portfolio.addTransaction(transaction);
		
		portfolio.balance = newBalance;
	

		PortfolioEntry position =  portfolio.getPortfolioEntryBySymbol(communityquote.quote.symbol);
		
		if(position == null){
			position = new PortfolioEntry(communityquote,portfolio);
			position.averagePrice = communityquote.quote.marketPrice;
			position.shareQty = qty;
			portfolio.addPortfolioEntry(position);
		}else{
			BigDecimal previousAveragePrice = position.averagePrice; 
			int previousQuantity = position.shareQty;
			BigDecimal previousTotalPrice = previousAveragePrice.multiply(new BigDecimal(previousQuantity));
			position.averagePrice = (previousTotalPrice.add(transactionPrice)).divide(new BigDecimal(previousQuantity + qty),2,RoundingMode.HALF_UP );
			position.shareQty = previousQuantity + qty;

		}


		portfolio.save();
		
		return transaction;
		
	}

	public static PorfolioTransaction executeSellTransaction(Portfolio portfolio,
			CommunityQuote communityquote, int qty) {
		
		BigDecimal marketPrice = communityquote.quote.marketPrice;
		
		BigDecimal transactionPrice =  marketPrice.multiply(new BigDecimal(qty));
		BigDecimal newBalance = portfolio.balance.add(transactionPrice);
		
		PorfolioTransaction transaction = new PorfolioTransaction(portfolio,communityquote,marketPrice,qty,new Date(),"Sell");

		portfolio.addTransaction(transaction);
		
		portfolio.balance = newBalance;
		
		PortfolioEntry position =  portfolio.getPortfolioEntryBySymbol(communityquote.quote.symbol);
		
		int sharesOwned = position.shareQty;
		
		if(qty < sharesOwned){
			
			position.shareQty = sharesOwned - qty;
			
		}else if(qty == sharesOwned){
			
			if(!portfolio.removePortfolioEntry(position.id)){
				throw new RuntimeException("could not delete porfolio entry with id = " +position.id );
			}
			
		}else{
			throw new RuntimeException("qty should not excess # of shared owned.");
		}
		
		portfolio.save();
		
		return transaction;
	}
	
}
