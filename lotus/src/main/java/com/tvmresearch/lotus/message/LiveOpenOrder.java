package com.tvmresearch.lotus.message;

import com.ib.controller.NewContract;
import com.ib.controller.NewOrder;
import com.ib.controller.NewOrderState;
import com.tvmresearch.lotus.Lotus;

/**
 * Tuple for passing around order data
 *
 * Created by horse on 3/04/2016.
 */
public class LiveOpenOrder extends IBMessage {
    public NewContract contract;
    public NewOrder order;
    public NewOrderState orderState;

    public LiveOpenOrder(NewContract contract, NewOrder order, NewOrderState orderState) {
        this.contract = contract;
        this.order = order;
        this.orderState = orderState;
    }


    @Override
    public void process(Lotus lotus) {
        lotus.processOpenOrder(contract, order, orderState);
        /*
        2016-05-31 14:00:09.462 [EReader] INFO  LiveOrderHandler - openOrder: 
contract=NewContract{m_conid=265598,
 m_symbol='AAPL',
 m_secType=STK,
 m_expiry='',
 m_strike=0.0,
 m_right=None,
 m_multiplier='null',
 m_exchange='SMART',
 m_primaryExch='null',
 m_currency='USD',
 m_localSymbol='AAPL',
 m_tradingClass='NMS',
 m_secIdType=None,
 m_secId='null',
 m_underComp=null,
 m_comboLegs=[]},

order=NewOrder{m_clientId=1,
 m_orderId=19,
 m_permId=539592039,
 m_parentId=0,
 m_account='DU289674',
 m_action=BUY,
 m_totalQuantity=1,
 m_displaySize=0,
 m_orderType=MKT,
 m_lmtPrice=0.0,
 m_auxPrice=0.0,
 m_tif=DAY,
 m_allOrNone=false,
 m_blockOrder=false,
 m_eTradeOnly=false,
 m_firmQuoteOnly=false,
 m_hidden=false,
 m_notHeld=false,
 m_optOutSmartRouting=false,
 m_outsideRth=false,
 m_sweepToFill=false,
 m_delta=1.7976931348623157E308,
 m_discretionaryAmt=0.0,
 m_nbboPriceCap=1.7976931348623157E308,
 m_percentOffset=1.7976931348623157E308,
 m_startingPrice=1.7976931348623157E308,
 m_stockRangeLower=1.7976931348623157E308,
 m_stockRangeUpper=1.7976931348623157E308,
 m_stockRefPrice=1.7976931348623157E308,
 m_trailingPercent=1.7976931348623157E308,
 m_trailStopPrice=1.7976931348623157E308,
 m_minQty=2147483647,
 m_goodAfterTime='null',
 m_goodTillDate='null',
 m_ocaGroup='null',
 m_orderRef='null',
 m_rule80A=None,
 m_ocaType=ReduceWithoutBlocking,
 m_triggerMethod=Default,
 m_faGroup='null',
 m_faMethod=None,
 m_faPercentage='null',
 m_faProfile='null',
 m_volatility=1.7976931348623157E308,
 m_volatilityType=None,
 m_continuousUpdate=false,
 m_referencePriceType=None,
 m_deltaNeutralOrderType=None,
 m_deltaNeutralAuxPrice=1.7976931348623157E308,
 m_deltaNeutralConId=0,
 m_scaleInitLevelSize=2147483647,
 m_scaleSubsLevelSize=2147483647,
 m_scalePriceIncrement=1.7976931348623157E308,
 m_scalePriceAdjustValue=1.7976931348623157E308,
 m_scalePriceAdjustInterval=2147483647,
 m_scaleProfitOffset=1.7976931348623157E308,
 m_scaleAutoReset=false,
 m_scaleInitPosition=2147483647,
 m_scaleInitFillQty=2147483647,
 m_scaleRandomPercent=false,
 m_scaleTable='null',
 m_hedgeType=None,
 m_hedgeParam='null',
 m_algoStrategy=None,
 m_algoParams=[],
 m_smartComboRoutingParams=[],
 m_orderComboLegs=[],
 m_whatIf=false,
 m_transmit=true,
 m_overridePercentageConstraints=false},
 
orderState=NewOrderState{m_status=PreSubmitted,
 m_initMargin='1.7976931348623157E308',
 m_maintMargin='1.7976931348623157E308',
 m_equityWithLoan='1.7976931348623157E308',
 m_commission=1.7976931348623157E308,
 m_minCommission=1.7976931348623157E308,
 m_maxCommission=1.7976931348623157E308,
 m_commissionCurrency='null',
 m_warningText='null'}

         */
        //investmentDao.find(order.orderId(), contract.symbol());
    }
}
