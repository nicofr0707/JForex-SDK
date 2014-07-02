package NicoSources;

import NicoSources.*;
import singlejartest.*;
import NicoSources.*;
import com.dukascopy.api.IEngine.OrderCommand;
import java.io.IOException;
import java.util.*;
import java.util.Arrays;
import java.util.Comparator;
import java.text.DateFormat;

import com.dukascopy.api.*;
import com.dukascopy.api.IIndicators.*;
import com.dukascopy.api.drawings.ILabelChartObject;
import com.dukascopy.api.drawings.IChartObjectFactory;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

import org.jdom.*;
import org.jdom.output.*;
import org.jdom.input.*;
import org.jdom.filter.*;
import java.util.List;

import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Library("G:\\Programmes\\Programmation\\jdom\\jdom\\build\\jdom.jar")
@RequiresFullAccess
public class R_CurrencyIndexRobotFX20140102_mvn_reseau implements IStrategy {

    @Configurable("usedSLConfig : ") // Stoploss type
    public int usedSLConfig = 0;
    @Configurable("atrSLDEVMultiplier =  : ") // default 5
    public int atrSLDEVMultiplier = 0;
    @Configurable("stopLossMultiplier =  : ") // default 5
    public double stopLossMultiplier = 1;
    @Configurable("Used periods :")
    public int usedPeriodsConfig = 3;
    @Configurable("0=no read,no write XML; 1=read XML; 2=write XML :")// 0 = no read, no write XML; 1 = read XML; 2 = backtest Mode, write XML
    public int robotOperation = 2;
    // @Configurable("XML lookback (days) :")
    public int XMLLookbackDays = 52;
    // @Configurable("Top parameter :")
    public int topParameter = 0;
    // @Configurable("XMLparamProfitPctTresh :")
    public double XMLparamProfitPctTresh = -10;

    private double globalInstrumentStrenghtTresh = 0;
    // @Configurable("Pivot periods :")
    public Period pivotPeriod = Period.DAILY;
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    private IAccount account;
    private IDataService dataService;
    private static final Currency USD = Currency.getInstance("USD");
    private static HashMap<Currency, Instrument> pairs = new HashMap<Currency, Instrument>();
//
    private int enablePrint = 0;
    private int drawOnChart = 0;
    private int restrictPairsToTrade = 0;
    private String londonStartTime = "08:00:00";// First trade allowed 15 minutes later
    private String londonLastTradeAlowedTime = "20:00:00";
    private String londonTradeCloseTime = "20:00:00"; // Trade is passed 30 min later
    private String closeTradesTime = "07:00:00";
    private String fridayCloseTime = "20:00:00";
    private Period stopLossPeriod = Period.FIFTEEN_MINS;
    private int fractalBarsOnside = 3;
    // Risk per trade in pct
    private double pctRisq = 0.1;
    private double maxPctEquityLoss = -20;
    private double pctProfitLoss = 0;
    private int monthCounter = 0;
    // Stop management
    // "Lock-in Pips for Breakeven")
    public int lockPip = 2;
    // Stop
    public boolean moveBE = true;
    //
    private PrintOut printOut;
    // Standard periods for indicators
    //public int[] usedPeriodsConfigTab = {1,3,5};
    //public Period[] UsedPeriods = {/*Period.ONE_MIN, Period.FIVE_MINS, Period.FIFTEEN_MINS, Period.THIRTY_MINS, */Period.ONE_HOUR, Period.FOUR_HOURS, Period.DAILY_SUNDAY_IN_MONDAY/*, Period.WEEKLY, Period.MONTHLY, Period.createCustomPeriod(Unit.Month, 6)*/};
    // Custom periods for calculating shift
    // private Period[] CustomUsedPeriods = {Period.FIVE_MINS, Period.FIFTEEN_MINS, Period.THIRTY_MINS, Period.ONE_HOUR, Period.FOUR_HOURS, Period.DAILY_SUNDAY_IN_MONDAY, Period.WEEKLY, Period.MONTHLY, Period.createCustomPeriod(Unit.Month, 6)};
    private String[] CurrenciesToWatch = {"USD", "EUR", "GBP", /*"CHF",*/ "JPY", "CAD", "AUD", "NZD"};
    private Instrument[] PairsToTrade = {
        Instrument.AUDCAD, Instrument.AUDJPY, Instrument.AUDNZD, Instrument.AUDUSD,
        /*Instrument.CADJPY,*/
        Instrument.EURUSD, Instrument.EURAUD, Instrument.EURCAD, Instrument.EURGBP,/* Instrument.EURJPY,*/ Instrument.EURNZD,
        Instrument.GBPAUD, Instrument.GBPCAD, /*Instrument.GBPJPY,*/ Instrument.GBPNZD, Instrument.GBPUSD,
        Instrument.NZDCAD, /*Instrument.NZDJPY,*/ Instrument.NZDUSD,
        Instrument.USDCAD, Instrument.USDJPY};
    /* {
     Instrument.AUDUSD,
     Instrument.EURUSD,
     Instrument.EURAUD,
     Instrument.EURNZD,
     };*/

    /* {
     Instrument.AUDNZD, Instrument.AUDUSD,
     Instrument.EURUSD, Instrument.EURAUD, Instrument.EURJPY, Instrument.EURNZD,
     Instrument.GBPUSD,
     Instrument.NZDJPY, Instrument.NZDUSD,
     Instrument.USDCAD, Instrument.USDJPY};*/
    // for exemple, 2 means it takes the 2 best and the 2 worst currencies
    private int bestCurrenciesToTrade = 2;
    private int maxOpenedOrdersPerPair = 1;
    private int maxOpenedOrdersOrders = 4;
    private List<String> strongCurrencyList;
    private List<String> weakCurrencyList;
    Double lastFractalMax = Double.NaN;
    Double lastFractalMin = Double.NaN;
    //
    private IChart chart;
    private String chartCommentString = "";
    private long londonDSTOffset;
    private SimpleDateFormat dateFormatter;
    private SimpleDateFormat formatToTime;
    private SimpleDateFormat dayFormatter;
    private SimpleDateFormat chronoFormat;
    private GregorianCalendar cal;
    private long startChronoTimeMillisec = 0;
    private long endChronoTimeMillisec = 0;
    String correlationOutString = "Global result : ";
    private double stdLotValue = 100000;
    Runtime runtime;
    private List<Integer> strongCurrencyListStrength;
    private List<Integer> weakCurrencyListStrength;
    // XML 
    private Element rootNode;
    private Element dayNode;
    private org.jdom.Document XMLDocument;
    private String xmlHistoryFile;
    private String parametersString;
    private double XMLprofit;
    private double XMLparamProfitPct;
    public String[] destFolders = {"C:/Users/Nicolas/Documents/JForex/Strategies/JForexStrategies/reports/currencyIndex2014/XMLWriteTmp_MVN/"};
    public String destReadFolder = "C:/Users/Nicolas/Documents/JForex/Strategies/JForexStrategies/reports/currencyIndex2014/XMLRead_20070502_20140116/";

    // initialise currency pairs in order to calculate an accurate money management for each pair.
    private void initializeCurrencyPairs() {

        Set<Instrument> instruments = new HashSet<Instrument>();

        // Looping into periods and instruments
        //for (int nbPeriod = 0; nbPeriod < UsedPeriods.length; nbPeriod++) {
        // Bulding the instruments to test
        for (int nbCurrency1 = 0; nbCurrency1 < CurrenciesToWatch.length; nbCurrency1++) {
            for (int nbCurrency2 = nbCurrency1 + 1; nbCurrency2 < CurrenciesToWatch.length; nbCurrency2++) {
                String instrumentStr = CurrenciesToWatch[nbCurrency1] + "/" + CurrenciesToWatch[nbCurrency2];
                String correctInstrumentStr = "";
                // Verifying if the pair is named correctly
                if (Instrument.isInverted(instrumentStr)) {
                    correctInstrumentStr = CurrenciesToWatch[nbCurrency2] + "/" + CurrenciesToWatch[nbCurrency1];
                } else {
                    correctInstrumentStr = instrumentStr;
                }
                instruments.add(Instrument.fromString(correctInstrumentStr));
            }
        }
        // }

        context.setSubscribedInstruments(instruments);

// wait max 1 second for the instruments to get subscribed
        int i = 10;
        while (!context.getSubscribedInstruments().containsAll(instruments) && i > 0) {
            try {
                // printOut.print("Instruments not subscribed yet " + i);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                console.getOut().println("error : " + e.getMessage());
            }
            i--;
        }

        pairs.put(Currency.getInstance("AUD"), Instrument.AUDUSD);
        pairs.put(Currency.getInstance("CAD"), Instrument.USDCAD);
        pairs.put(Currency.getInstance("CHF"), Instrument.USDCHF);
        pairs.put(Currency.getInstance("DKK"), Instrument.USDDKK);
        pairs.put(Currency.getInstance("EUR"), Instrument.EURUSD);
        pairs.put(Currency.getInstance("GBP"), Instrument.GBPUSD);
        pairs.put(Currency.getInstance("HKD"), Instrument.USDHKD);
        pairs.put(Currency.getInstance("JPY"), Instrument.USDJPY);
        pairs.put(Currency.getInstance("NOK"), Instrument.USDNOK);
        pairs.put(Currency.getInstance("NZD"), Instrument.NZDUSD);
        pairs.put(Currency.getInstance("SEK"), Instrument.USDSEK);
        pairs.put(Currency.getInstance("SGD"), Instrument.USDSGD);

    }

    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();
        this.account = context.getAccount();
        this.dataService = context.getDataService();
        //
        runtime = Runtime.getRuntime();
        //
        cal = new GregorianCalendar();
        dateFormatter = new SimpleDateFormat("yyyy/MM/dd");
        formatToTime = new SimpleDateFormat("HH:mm:ss");
        dayFormatter = new SimpleDateFormat("EE", Locale.US);
        chronoFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        formatToTime.setTimeZone(TimeZone.getTimeZone("GMT"));
        dayFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        //  getUsedPeriods(usedPeriodsConfig);
        //public Period[] UsedPeriods = {/*Period.ONE_MIN, Period.FIVE_MINS, Period.FIFTEEN_MINS, Period.THIRTY_MINS, */Period.ONE_HOUR, Period.FOUR_HOURS, Period.DAILY_SUNDAY_IN_MONDAY/*, Period.WEEKLY, Period.MONTHLY, Period.createCustomPeriod(Unit.Month, 6)*/};
        printOut = new PrintOut();
        initializeCurrencyPairs();
        londonDSTOffset = getDSToffset(history.getLastTick(Instrument.EURUSD).getTime(), 0);

        // if robot is in read XML mode
        if (robotOperation == 1) {
            String[] robotParamsList = getRobotParams(destReadFolder, history.getLastTick(Instrument.EURUSD).getTime(), XMLLookbackDays);

            // for (int i = 0; i < nbTopParameters; i++) {
            String robotParam = robotParamsList[topParameter];
            String[] listPlit = robotParam.split("_");

            usedPeriodsConfig = Integer.parseInt(listPlit[1]);
            stopLossMultiplier = Double.parseDouble(listPlit[2]);
            usedSLConfig = Integer.parseInt(listPlit[3]);
            atrSLDEVMultiplier = Integer.parseInt(listPlit[4]);

            printOut.print("FINAL params : " + usedPeriodsConfig + "_" + stopLossMultiplier + "_" + usedSLConfig + "_" + atrSLDEVMultiplier);
            //  }

            // if robot is in backtest XML write mode
        } else if (robotOperation == 2) {
            parametersString = usedPeriodsConfig + "_" + stopLossMultiplier + "_" + usedSLConfig + "_" + atrSLDEVMultiplier;
            rootNode = new Element("currencyIndex_" + parametersString);
            XMLDocument = new Document(rootNode);
            xmlHistoryFile = "currencyIndex_" + parametersString + ".xml";
            XMLWrite(xmlHistoryFile, XMLDocument);

        }

        //analyzeMarket();
    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {

        if (message.getType() == IMessage.Type.ORDER_FILL_OK) {
            IOrder order = message.getOrder();
            Instrument orderInstrument = order.getInstrument();

            // If robot is in backtesting mode add the opened order to the current day
            if (robotOperation == 2) {
                OrderCommand orderCommand = order.getOrderCommand();
                double orderOpenPrice = order.getOpenPrice();
                double orderSLPrice = order.getStopLossPrice();
                double orderTPPrice = order.getTakeProfitPrice();
                long orderFillTimeMillisec = order.getFillTime();
                String orderComment = order.getComment();
                String orderId = order.getId();
                String orderLabel = order.getLabel();

                Element orderNode = new Element("orderOpen");
                dayNode.addContent(orderNode);

                Attribute orderInstrumentAtr = new Attribute("orderInstrument", orderInstrument.toString());
                orderNode.setAttribute(orderInstrumentAtr);
                Attribute orderCommandAtr = new Attribute("orderCommand", orderCommand.toString());
                orderNode.setAttribute(orderCommandAtr);
                Attribute orderOpenPriceAtr = new Attribute("orderOpenPrice", String.valueOf(orderOpenPrice));
                orderNode.setAttribute(orderOpenPriceAtr);
                Attribute orderSLPriceAtr = new Attribute("orderSLPrice", String.valueOf(orderSLPrice));
                orderNode.setAttribute(orderSLPriceAtr);
                Attribute orderTPPriceAtr = new Attribute("orderTPPrice", String.valueOf(orderTPPrice));
                orderNode.setAttribute(orderTPPriceAtr);
                Attribute orderFillTimeStringAtr = new Attribute("orderFillTimeString", dateFormatter.format(orderFillTimeMillisec));
                orderNode.setAttribute(orderFillTimeStringAtr);
                Attribute orderFillTimeMillisecAtr = new Attribute("orderFillTimeMillisec", String.valueOf(orderFillTimeMillisec));
                orderNode.setAttribute(orderFillTimeMillisecAtr);
                Attribute orderCommentAtr = new Attribute("orderComment", orderComment);
                orderNode.setAttribute(orderCommentAtr);
                Attribute orderIdAtr = new Attribute("orderId", orderId);
                orderNode.setAttribute(orderIdAtr);
                Attribute orderLabelAtr = new Attribute("orderLabel", orderLabel);
                orderNode.setAttribute(orderLabelAtr);
            }

        }

        if (message.getType() == IMessage.Type.ORDER_CLOSE_OK) {

            IOrder order = message.getOrder();
            Instrument orderInstrument = order.getInstrument();

            // If robot is in backtesting mode add the closed order to the current day
            if (robotOperation == 2) {
                OrderCommand orderCommand = order.getOrderCommand();
                double orderProfitUSD = order.getProfitLossInUSD();
                double orderProfitPips = order.getProfitLossInPips();
                double orderOpenPrice = order.getOpenPrice();
                double orderSLPrice = order.getStopLossPrice();
                double orderTPPrice = order.getTakeProfitPrice();
                double orderClosePrice = order.getClosePrice();
                long orderCloseTimeMillisec = order.getCloseTime();
                long orderFillTimeMillisec = order.getFillTime();
                String orderComment = order.getComment();
                String orderId = order.getId();
                String orderLabel = order.getLabel();

                Element orderNode = new Element("orderClose");
                dayNode.addContent(orderNode);

                Attribute orderInstrumentAtr = new Attribute("orderInstrument", orderInstrument.toString());
                orderNode.setAttribute(orderInstrumentAtr);
                Attribute orderCommandAtr = new Attribute("orderCommand", orderCommand.toString());
                orderNode.setAttribute(orderCommandAtr);
                Attribute orderProfitUSDAtr = new Attribute("orderProfitUSD", String.valueOf(orderProfitUSD));
                orderNode.setAttribute(orderProfitUSDAtr);
                Attribute orderProfitPipsAtr = new Attribute("orderProfitPips", String.valueOf(orderProfitPips));
                orderNode.setAttribute(orderProfitPipsAtr);
                Attribute orderOpenPriceAtr = new Attribute("orderOpenPrice", String.valueOf(orderOpenPrice));
                orderNode.setAttribute(orderOpenPriceAtr);
                Attribute orderSLPriceAtr = new Attribute("orderSLPrice", String.valueOf(orderSLPrice));
                orderNode.setAttribute(orderSLPriceAtr);
                Attribute orderTPPriceAtr = new Attribute("orderTPPrice", String.valueOf(orderTPPrice));
                orderNode.setAttribute(orderTPPriceAtr);
                Attribute orderClosePriceAtr = new Attribute("orderClosePrice", String.valueOf(orderClosePrice));
                orderNode.setAttribute(orderClosePriceAtr);
                Attribute orderFillTimeStringAtr = new Attribute("orderFillTimeString", dateFormatter.format(orderFillTimeMillisec));
                orderNode.setAttribute(orderFillTimeStringAtr);
                Attribute orderCloseTimeStringAtr = new Attribute("orderCloseTimeString", dateFormatter.format(orderCloseTimeMillisec));
                orderNode.setAttribute(orderCloseTimeStringAtr);
                Attribute orderFillTimeMillisecAtr = new Attribute("orderFillTimeMillisec", String.valueOf(orderFillTimeMillisec));
                orderNode.setAttribute(orderFillTimeMillisecAtr);
                Attribute orderCloseTimeMillisecAtr = new Attribute("orderCloseTimeMillisec", String.valueOf(orderCloseTimeMillisec));
                orderNode.setAttribute(orderCloseTimeMillisecAtr);
                Attribute orderCommentAtr = new Attribute("orderComment", orderComment);
                orderNode.setAttribute(orderCommentAtr);
                Attribute orderIdAtr = new Attribute("orderId", orderId);
                orderNode.setAttribute(orderIdAtr);
                Attribute orderLabelAtr = new Attribute("orderLabel", orderLabel);
                orderNode.setAttribute(orderLabelAtr);
            }
            // drawImage(orderInstrument);
        }

    }

    public void onStop() throws JFException {

        for (IOrder order : engine.getOrders()) {

            String openDate = dateFormatter.format(order.getFillTime()) + " " + formatToTime.format(order.getFillTime());
            String closeDate = dateFormatter.format(order.getCloseTime()) + " " + formatToTime.format(order.getCloseTime());
            String Instrument = order.getInstrument().toString();
            String OrderDirection = order.getOrderCommand().toString();
            double amount = order.getAmount();
            double stoplossValue = order.getStopLossPrice();
            double takeProfitValue = order.getTakeProfitPrice();
            double openPrice = order.getOpenPrice();
            double closePrice = order.getClosePrice();
            double profitInPips = order.getProfitLossInPips();
            double profitInCurrency = order.getProfitLossInAccountCurrency();
            double commissons = 36;
            double profitInCurrencyLessCom = profitInCurrency - (amount * commissons);

        }
        /*  FileWrite fileWriter = new FileWrite();
         fileWriter.writeToFile(cal.getTimeInMillis() + ".log", processReport());
         PrintToConsole("Stopped");*/

    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
        // Stoploss manager
       /* for (IOrder order : engine.getOrders(instrument)) {
         if (order.getState() == IOrder.State.FILLED) {
         boolean isLong;
         double open, stop, diff, newStop, tpPrice;
         String label = order.getLabel();
         // IChart chart;

         isLong = order.isLong();
         open = order.getOpenPrice();
         stop = order.getStopLossPrice();
         //                tpPrice = order.getTakeProfitPrice();
         diff = (open - stop);      // <span class="posthilit">stop</span> loss distance

         if (isLong) {         // long side order
         if (moveBE && diff > 0 && tick.getBid() > (open + diff)) {
         // make it breakeven trade + lock in a few pips
         newStop = open + instrument.getPipValue() * lockPip;
         order.setStopLossPrice(newStop);
         // console.getOut().println(label + ": Moved stop to breakeven");

         //chart = this.context.getChart(instrument);
         //chart.draw(label + "_BE", IChart.Type.SIGNAL_UP, tick.getTime(), newStop);
         }
         } else {               // short side order
         // Move to breakeven
         if (moveBE && diff < 0 && tick.getAsk() < (open + diff)) {   // diff is negative
         // make it breakeven trade + lock in a few pips
         newStop = open - (instrument.getPipValue() * lockPip);
         order.setStopLossPrice(newStop);
         // console.getOut().println(label + ": Moved stop to breakeven");

         //chart = this.context.getChart(instrument);
         //chart.draw(label + "_BE", IChart.Type.SIGNAL_DOWN, tick.getTime(), newStop);
         }
         }
         }
         }*/
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {

        double fixedEquityValue = 10000;
        double pctFixedProfitLossTotal = 0;
        double pctProfitLoss = 0;
        double pctProfitLossEquity = 0;
        double totalProfitLoss = 0;
        double pctProfitLossTotal = 0;
        double pctProfitLossBalance = 0;
        double baseAccountEquity = 0;
        double currentAccountEquity = 0;
        double calculatedBaseAccountEquity = 0;
        double currentBalance = 0;
        double nbOpenedOrders = 0;
        String currentCurrenciesOpened = "";
        double averagePctProfitLossPerOrder = 0;

        if (period.equals(Period.DAILY_SUNDAY_IN_MONDAY) && instrument == Instrument.EURUSD) {
            //  londonDSTOffset = getDSToffset(history.getLastTick(Instrument.EURUSD).getTime(), 0);
            // Writing the backtest to XML file
            // adding a node every day
            if (robotOperation == 2) {
                // We ajust the time to the next open time
                long currentTime = bidBar.getTime() + 1000 * 60 * 60 * 24;
                dayNode = new Element("day");
                rootNode.addContent(dayNode);
                Attribute dayOfWeekAtr = new Attribute("dayOfWeek", String.valueOf(getDayOfTheWeek(currentTime)));
                dayNode.setAttribute(dayOfWeekAtr);
                Attribute dayOfWeekStrAtr = new Attribute("dayOfWeekStr", dayFormatter.format(currentTime));
                dayNode.setAttribute(dayOfWeekStrAtr);
                Attribute dateMillisecAtr = new Attribute("dateMillisec", String.valueOf(currentTime));
                dayNode.setAttribute(dateMillisecAtr);
                Attribute dateStringAtr = new Attribute("dateString", dateFormatter.format(currentTime));
                dayNode.setAttribute(dateStringAtr);

                XMLWrite(xmlHistoryFile, XMLDocument);
                console.getOut().println("//////////////////// Daily : " + dateStringAtr + " usedPeriodsConfig : " + usedPeriodsConfig);

                // PRinting out the time the progamm need to calculate each day
                cal = (GregorianCalendar) Calendar.getInstance();
                endChronoTimeMillisec = cal.getTimeInMillis();
                console.getOut().println("ProcessLength: " + chronoFormat.format(endChronoTimeMillisec - startChronoTimeMillisec));

                cal = (GregorianCalendar) Calendar.getInstance();
                startChronoTimeMillisec = cal.getTimeInMillis();

            }
        }

        // sells all avery period
   /*     if (period.equals(Period.MONTHLY) && instrument == Instrument.EURUSD) {
         monthCounter++;
         if(monthCounter>=3){
         for (IOrder order : engine.getOrders()) {
         order.close();
         }
         monthCounter = 0;
         }
         }*/
        // claculate % loss of equity
        if (period.equals(Period.FIVE_MINS) && instrument == Instrument.EURUSD) {

            currentBalance = account.getBalance();
            baseAccountEquity = account.getBaseEquity();
            currentAccountEquity = account.getEquity();
            //
            // 1st Method, calculating % of floating profit/loss comparing getBaseEquity() and getEquity();
            pctProfitLoss = roundFloor(((currentAccountEquity - baseAccountEquity) / baseAccountEquity) * 100, 1000);

            //
            // 2nd Method, calculating % of floating profit/loss comparing getBalance() and getEquity();
            pctProfitLossBalance = roundFloor(((currentAccountEquity - currentBalance) / currentBalance) * 100, 1000);
            //
            // 3rd method, manualy calculating % of floating profit/loss by adding all opened orders floating profit/losses and comparing them to getEquity();
            for (IOrder order : engine.getOrders()) {
                if (order.getState() == IOrder.State.FILLED) {
                    nbOpenedOrders++;
                    totalProfitLoss = totalProfitLoss + order.getProfitLossInAccountCurrency();
                    currentCurrenciesOpened = currentCurrenciesOpened + ", " + order.getInstrument();
                }
            }
            calculatedBaseAccountEquity = roundFloor(currentAccountEquity - totalProfitLoss, 100);
            pctProfitLossTotal = roundFloor(((currentAccountEquity - calculatedBaseAccountEquity) / calculatedBaseAccountEquity) * 100, 1000);
            pctFixedProfitLossTotal = roundFloor(((currentAccountEquity - calculatedBaseAccountEquity) / calculatedBaseAccountEquity) * 100, 1000);
            //
            pctProfitLossEquity = roundFloor((totalProfitLoss / currentAccountEquity) * 100, 1000);
            averagePctProfitLossPerOrder = roundFloor(pctFixedProfitLossTotal / nbOpenedOrders, 1000);
            //
            // Closing the opened orders based on the 1st method calculations
            if (averagePctProfitLossPerOrder < maxPctEquityLoss) {
                for (IOrder order : engine.getOrders()) {
                    order.close();
                }
            }
        }

        // drawing on the charts a comment with all calculation methods
        if (period.equals(Period.FIVE_MINS)) {
            commentOnChart(instrument, "currentAccountEquity : " + String.valueOf(currentAccountEquity) + "\n"
                    //  + "baseAccountEquity : " + String.valueOf(baseAccountEquity) + "\n"
                    //  + "calculatedBaseAccountEquity : " + String.valueOf(calculatedBaseAccountEquity) + "\n"
                    //  + "Equity P/L (1st method) : " + String.valueOf(pctProfitLoss) + "%\n"
                    // + "Balance P/L (2nd method) : " + String.valueOf(pctProfitLossBalance) + "%\n"
                    //  + "Total P/L (3rd method) : " + String.valueOf(pctProfitLossTotal) + "%\n"
                    + "pctProfitLossEquity P/L : " + String.valueOf(pctProfitLossEquity) + "%\n"
                    + "averagePctProfitLossPerOrder P/L : " + String.valueOf(averagePctProfitLossPerOrder) + "% (nbOrders = " + nbOpenedOrders + ")\n"
                    + "Instruments opened : " + currentCurrenciesOpened + "\n"
                    + "totalProfitLoss € (3rd method) : " + String.valueOf(roundFloor(totalProfitLoss, 100)) + "€\n"
                    + "usedPeriodsConfig : " + usedPeriodsConfig + " stopLossMultiplier : " + stopLossMultiplier + " usedSLConfig : " + usedSLConfig + " atrSLDEVMultiplier : " + atrSLDEVMultiplier + " XMLprofit : " + XMLprofit + "XMLparamProfitPct : " + XMLparamProfitPct);
        }

// Every day method
        if (instrument == Instrument.EURUSD && (getDayOfTheWeek(bidBar.getTime()) == Calendar.TUESDAY || getDayOfTheWeek(bidBar.getTime()) == Calendar.WEDNESDAY || getDayOfTheWeek(bidBar.getTime()) == Calendar.THURSDAY) && period.equals(Period.FIFTEEN_MINS) && getOrderDetails(null)[2] < maxOpenedOrdersOrders) {

            long currentTime = bidBar.getTime();

            if (currentTime >= (convertStringToMillisec(currentTime, londonTradeCloseTime)) && currentTime < (convertStringToMillisec(currentTime, londonTradeCloseTime) + 900000)) {

                // for (IOrder order : engine.getOrders()) {
                // if (order.getState() != IOrder.State.FILLED) {
                // order.close();
                // }
                //  }
                // if robot is in read XML mode
                if (robotOperation == 1) {
                    String[] robotParamsList = getRobotParams(destReadFolder, history.getLastTick(Instrument.EURUSD).getTime(), XMLLookbackDays);
                    String robotParam01 = robotParamsList[topParameter];
                    String[] listPlit = robotParam01.split("_");

                    usedPeriodsConfig = Integer.parseInt(listPlit[1]);
                    stopLossMultiplier = Double.parseDouble(listPlit[2]);
                    usedSLConfig = Integer.parseInt(listPlit[3]);
                    atrSLDEVMultiplier = Integer.parseInt(listPlit[4]);
                    XMLprofit = Double.parseDouble(listPlit[6]);
                    XMLparamProfitPct = Double.parseDouble(listPlit[8]);

                    printOut.print("FINAL Daily : " + usedPeriodsConfig + "_" + stopLossMultiplier + "_" + usedSLConfig + "_" + atrSLDEVMultiplier + "_" + XMLprofit + "_" + XMLparamProfitPct);

                    if (XMLparamProfitPct > XMLparamProfitPctTresh) {
                        printOut.print("////////////analyzeMarket();/////////");
                        analyzeMarket();
                    }
                } else if (robotOperation == 0 || robotOperation == 2) {
                    printOut.print("////////////analyzeMarket();/////////");
                    analyzeMarket();
                }

            }

            /*    if (currentTime >= (convertStringToMillisec(currentTime, closeTradesTime)) && currentTime < (convertStringToMillisec(currentTime, closeTradesTime) + 900000)) {


             //
             for (IOrder order : engine.getOrders()) {
             order.close();

             }
             }*/
            //     if (currentTime >= (convertStringToMillisec(currentTime, fridayCloseTime)) && currentTime < (convertStringToMillisec(currentTime, fridayCloseTime) + 1800000)) {
            //if (dayOfTheWeek == Calendar.FRIDAY) {
            // closing all orders before we
            //for (IOrder order : engine.getOrders()) {
            // order.close();
            // }
            // }
            // }
        }

    }

    // Searching for position opportunities
    private void analyzeMarket() {

        String tmpCorrelationString = "";
        //
        // Trying random used period config

        /*   Random rand = new Random();
         int start = 3;
         int end = 4;
         usedPeriodsConfig = rand.nextInt(end - start + 1) + start;*/
        //
        int[][] correlationTable = getCurrencyCorrelations(usedPeriodsConfig);
        strongCurrencyList = new ArrayList<String>();
        weakCurrencyList = new ArrayList<String>();

        strongCurrencyListStrength = new ArrayList<Integer>();
        weakCurrencyListStrength = new ArrayList<Integer>();

        // String [] bestPairsToBuy = "";
        long currentTime = 0;
        try {
            currentTime = history.getLastTick(Instrument.EURUSD).getTime();
        } catch (JFException ex) {
            printOut.print(ex);
        }

        // Outputing the result
        correlationOutString = dateFormatter.format(currentTime) + "_" + formatToTime.format(currentTime) + " : ";

        for (int i = 0; i < bestCurrenciesToTrade; i++) {
            strongCurrencyList.add(CurrenciesToWatch[correlationTable[i][0]]);
            strongCurrencyListStrength.add(correlationTable[i][1]);
            correlationOutString = correlationOutString + " " + CurrenciesToWatch[correlationTable[i][0]] + " " + correlationTable[i][1] + " | ";
        }

        for (int i = CurrenciesToWatch.length - bestCurrenciesToTrade; i < CurrenciesToWatch.length; i++) {
            weakCurrencyList.add(CurrenciesToWatch[correlationTable[i][0]]);
            weakCurrencyListStrength.add(correlationTable[i][1]);
            correlationOutString = correlationOutString + " " + CurrenciesToWatch[correlationTable[i][0]] + " " + correlationTable[i][1] + " | ";
        }

//        printOut.print(":: ");
//        printOut.print(":: " + correlationOutString);
        Collections.reverse(weakCurrencyList);
        Collections.reverse(weakCurrencyListStrength);

        int cpt = 0;
        String orderCmdString = "";
        OrderCommand orderCmd;
        double entryPrice = 0;
        double fibEntry23;
        double fibEntry38;
        double fibEntry50;
        double fibEntry68;

        for (int i = 0; i < strongCurrencyList.size(); i++) {
            String buyCurrency = strongCurrencyList.get(i);
            int buyCurrencyStrength = strongCurrencyListStrength.get(i);

            for (int j = 0; j < weakCurrencyList.size(); j++) {
                String sellCurrency = weakCurrencyList.get(j);
                int sellCurrencyStrength = weakCurrencyListStrength.get(j);
                /* for (String buyCurrency : strongCurrencyList) {
                 for (String sellCurrency : weakCurrencyList) {*/

                cpt++;

                String instrumentStr = buyCurrency + "/" + sellCurrency;
                double globalInstrumentStrenght = Math.abs(buyCurrencyStrength) + Math.abs(sellCurrencyStrength);

                // printOut.print("instrumentStr : " + instrumentStr);
                //  printOut.print("instrumentStrStrength : " + buyCurrencyStrength + "/" + sellCurrencyStrength);
                // Verifying if the pair is named correctly
                if (Instrument.isInverted(instrumentStr)) {

                    instrumentStr = sellCurrency + "/" + buyCurrency;
                    Instrument currentInstrument = Instrument.fromString(instrumentStr);

                    try {
                        /*int currentTrend = getAlligatorTrend(currentInstrument, getUsedPeriods(8)[0]);
                         int currentSAR = getSAR(currentInstrument, getUsedPeriods(8)[0]);*/
                        int nbLongOpenedOrders = getOrderDetails(currentInstrument)[0];
                        int nbShortOpenedOrders = getOrderDetails(currentInstrument)[1];
                        int totalOpenedOrders = nbLongOpenedOrders + nbShortOpenedOrders;
                        if (getOrderDetails(null)[2] < maxOpenedOrdersOrders && totalOpenedOrders < maxOpenedOrdersPerPair/* && nbLongOpenedOrders >= nbShortOpenedOrders */ && globalInstrumentStrenght >= globalInstrumentStrenghtTresh/* && getIchimokuTrend(currentInstrument, Period.ONE_HOUR) == -30 || getIchimokuTrend(currentInstrument, Period.FOUR_HOURS) == -30/* && getIchimokuTrend(currentInstrument, Period.DAILY_SUNDAY_IN_MONDAY) == -30 *//*(Math.abs(buyCurrencyStrength) >= 200 || Math.abs(sellCurrencyStrength) >= 200) &&*//* testOpenedOrders(currentInstrument) < 100*//* && currentTrend < 0 && currentSAR < 0*/) {

                            if (restrictPairsToTrade == 1) {
                                // We sell only if we are below the pivot
                                for (Instrument authorisedPair : PairsToTrade) {
                                    if (currentInstrument == authorisedPair) {
                                        /* double currentPivot = getPivot(currentInstrument, pivotPeriod)[0];
                                         double currentPriceAsk = history.getLastTick(currentInstrument).getAsk();
                                         double currentPivotSupport = getPivot(currentInstrument, pivotPeriod)[1];

                                         if (currentPriceAsk < currentPivot && currentPriceAsk > currentPivotSupport) {*/
                                        //  printOut.print(correlationOutString);
                                       /* if (i == 0 && j == 0) {
                                         orderCmdString = "Sell";
                                         } else if (i == 0 && j == 1) {
                                         orderCmdString = "Buy";
                                         } else if (i == 1 && j == 0) {
                                         orderCmdString = "Sell";
                                         } else if (i == 1 && j == 1) {
                                         orderCmdString = "Buy";
                                         }*/

                                        orderCmdString = "Sell";
                                        //entryPrice = history.getLastTick(currentInstrument).getBid();
                                        entryPrice = history.getBar(currentInstrument, Period.ONE_MIN, OfferSide.BID, 0).getClose();
                                        //
                                     /*   fibEntry23 = getFibLevels(currentInstrument, orderCmdString)[0];
                                         fibEntry38 = getFibLevels(currentInstrument, orderCmdString)[1];
                                         fibEntry50 = getFibLevels(currentInstrument, orderCmdString)[2];
                                         fibEntry68 = getFibLevels(currentInstrument, orderCmdString)[3];*/

                                        for (int iValue = 0; iValue < 1; iValue++) {

                                            double tpValue;
                                            if (iValue == 0) {
                                                tpValue = 2;
                                            } else {
                                                tpValue = iValue;
                                            }

                                            printOut.print("globalInstrumentStrenght : " + globalInstrumentStrenght);
                                            placeOrder(orderCmdString, entryPrice, IEngine.OrderCommand.SELL, currentInstrument, iValue, tpValue, correlationOutString + ";;" + parametersString);

                                        }

                                        /* placeOrder(orderCmdString, fibEntry23, IEngine.OrderCommand.SELLLIMIT, currentInstrument, 1, correlationOutString);
                                         placeOrder(orderCmdString, fibEntry38, IEngine.OrderCommand.SELLLIMIT, currentInstrument, 2, correlationOutString);
                                         placeOrder(orderCmdString, fibEntry50, IEngine.OrderCommand.SELLLIMIT, currentInstrument, 3, correlationOutString);
                                         placeOrder(orderCmdString, fibEntry68, IEngine.OrderCommand.SELLLIMIT, currentInstrument, 4, correlationOutString);*/
                                        /* } else {
                                         //printOut.print(correlationOutString + " not in pivot, don't trade");
                                         }*/
                                    }
                                }
                            } else {
                                /* double currentPivot = getPivot(currentInstrument, pivotPeriod)[0];
                                 double currentPriceAsk = history.getLastTick(currentInstrument).getAsk();
                                 double currentPivotSupport = getPivot(currentInstrument, pivotPeriod)[1];

                                 if (currentPriceAsk < currentPivot && currentPriceAsk > currentPivotSupport) {*/
                                // printOut.print(correlationOutString);
                               /* if (i == 0 && j == 0) {
                                 orderCmdString = "Sell";
                                 } else if (i == 0 && j == 1) {
                                 orderCmdString = "Buy";
                                 } else if (i == 1 && j == 0) {
                                 orderCmdString = "Sell";
                                 } else if (i == 1 && j == 1) {
                                 orderCmdString = "Buy";
                                 }*/

                                orderCmdString = "Sell";
                                //entryPrice = history.getLastTick(currentInstrument).getBid();
                                entryPrice = history.getBar(currentInstrument, Period.ONE_MIN, OfferSide.BID, 0).getClose();
                                //
                               /* fibEntry23 = getFibLevels(currentInstrument, orderCmdString)[0];
                                 fibEntry38 = getFibLevels(currentInstrument, orderCmdString)[1];
                                 fibEntry50 = getFibLevels(currentInstrument, orderCmdString)[2];
                                 fibEntry68 = getFibLevels(currentInstrument, orderCmdString)[3];*/

                                for (int iValue = 0; iValue < 1; iValue++) {

                                    double tpValue;
                                    if (iValue == 0) {
                                        tpValue = 2;
                                    } else {
                                        tpValue = iValue;
                                    }

                                    printOut.print("globalInstrumentStrenght : " + globalInstrumentStrenght);
                                    placeOrder(orderCmdString, entryPrice, IEngine.OrderCommand.SELL, currentInstrument, iValue, tpValue, correlationOutString + ";;" + parametersString);

                                }

                                /*placeOrder(orderCmdString, fibEntry23, IEngine.OrderCommand.SELLLIMIT, currentInstrument, 1, correlationOutString);
                                 placeOrder(orderCmdString, fibEntry38, IEngine.OrderCommand.SELLLIMIT, currentInstrument, 2, correlationOutString);
                                 placeOrder(orderCmdString, fibEntry50, IEngine.OrderCommand.SELLLIMIT, currentInstrument, 3, correlationOutString);
                                 placeOrder(orderCmdString, fibEntry68, IEngine.OrderCommand.SELLLIMIT, currentInstrument, 4, correlationOutString);*/

                                /* } else {
                                 //printOut.print(correlationOutString + " not in pivot, don't trade");
                                 }*/
                            }
                        }
                    } catch (JFException ex) {
                        printOut.print(ex);
                    }
                    //printOut.print("SELL : " + Instrument.fromString(instrumentStr));

                } else {

                    Instrument currentInstrument = Instrument.fromString(instrumentStr);
                    try {
                        int nbLongOpenedOrders = getOrderDetails(currentInstrument)[0];
                        int nbShortOpenedOrders = getOrderDetails(currentInstrument)[1];
                        int totalOpenedOrders = nbLongOpenedOrders + nbShortOpenedOrders;

                        /* int currentTrend = getAlligatorTrend(currentInstrument, getUsedPeriods(8)[0]);
                         int currentSAR = getSAR(currentInstrument, getUsedPeriods(8)[0]);*/
                        if (getOrderDetails(null)[2] < maxOpenedOrdersOrders && totalOpenedOrders < maxOpenedOrdersPerPair/* && nbLongOpenedOrders <= nbShortOpenedOrders */ && globalInstrumentStrenght >= globalInstrumentStrenghtTresh /*&& getIchimokuTrend(currentInstrument, Period.ONE_HOUR) == 30 || getIchimokuTrend(currentInstrument, Period.FOUR_HOURS) == 30 && getIchimokuTrend(currentInstrument, Period.DAILY_SUNDAY_IN_MONDAY) == 30*//*(Math.abs(buyCurrencyStrength) >= 200 || Math.abs(sellCurrencyStrength) >= 200) &&*/ /*testOpenedOrders(currentInstrument) < 100*/ /*&& currentTrend > 0 && currentSAR > 0*/) {

                            if (restrictPairsToTrade == 1) {
                                // We Buy only if we are above the pivot
                                for (Instrument authorisedPair : PairsToTrade) {
                                    if (currentInstrument == authorisedPair) {
                                        /* double currentPivot = getPivot(currentInstrument, pivotPeriod)[0];
                                         double currentPriceBid = history.getLastTick(currentInstrument).getBid();
                                         double currentPivotResistance = getPivot(currentInstrument, pivotPeriod)[2];

                                         if (currentPriceBid > currentPivot && currentPriceBid < currentPivotResistance) {*/

                                        // printOut.print(correlationOutString);
                                      /*  if (i == 0 && j == 0) {
                                         orderCmdString = "Buy";
                                         } else if (i == 0 && j == 1) {
                                         orderCmdString = "Sell";
                                         } else if (i == 1 && j == 0) {
                                         orderCmdString = "Buy";
                                         } else if (i == 1 && j == 1) {
                                         orderCmdString = "Sell";
                                         }*/
                                        orderCmdString = "Buy";
                                        //entryPrice = history.getLastTick(currentInstrument).getAsk();
                                        entryPrice = history.getBar(currentInstrument, Period.ONE_MIN, OfferSide.ASK, 0).getClose();
                                        //
                                      /*  fibEntry23 = getFibLevels(currentInstrument, orderCmdString)[0];
                                         fibEntry38 = getFibLevels(currentInstrument, orderCmdString)[1];
                                         fibEntry50 = getFibLevels(currentInstrument, orderCmdString)[2];
                                         fibEntry68 = getFibLevels(currentInstrument, orderCmdString)[3];*/

                                        for (int iValue = 0; iValue < 1; iValue++) {

                                            double tpValue;
                                            if (iValue == 0) {
                                                tpValue = 2;
                                            } else {
                                                tpValue = iValue;
                                            }

                                            printOut.print("globalInstrumentStrenght : " + globalInstrumentStrenght);
                                            placeOrder(orderCmdString, entryPrice, IEngine.OrderCommand.BUY, currentInstrument, iValue, tpValue, correlationOutString + ";;" + parametersString);

                                        }

                                        /* placeOrder(orderCmdString, fibEntry23, IEngine.OrderCommand.BUYLIMIT, currentInstrument, 1, correlationOutString);
                                         placeOrder(orderCmdString, fibEntry38, IEngine.OrderCommand.BUYLIMIT, currentInstrument, 2, correlationOutString);
                                         placeOrder(orderCmdString, fibEntry50, IEngine.OrderCommand.BUYLIMIT, currentInstrument, 3, correlationOutString);
                                         placeOrder(orderCmdString, fibEntry68, IEngine.OrderCommand.BUYLIMIT, currentInstrument, 4, correlationOutString);*/
                                        /* } else {
                                         //printOut.print(correlationOutString + " not in pivot, don't trade");
                                         }*/
                                    }
                                }
                            } else {
                                /* double currentPivot = getPivot(currentInstrument, pivotPeriod)[0];
                                 double currentPriceBid = history.getLastTick(currentInstrument).getBid();
                                 double currentPivotResistance = getPivot(currentInstrument, pivotPeriod)[2];

                                 if (currentPriceBid > currentPivot && currentPriceBid < currentPivotResistance) {*/

                                //  printOut.print(correlationOutString);
                                /*if (i == 0 && j == 0) {
                                 orderCmdString = "Buy";
                                 } else if (i == 0 && j == 1) {
                                 orderCmdString = "Sell";
                                 } else if (i == 1 && j == 0) {
                                 orderCmdString = "Buy";
                                 } else if (i == 1 && j == 1) {
                                 orderCmdString = "Sell";
                                 }*/
                                orderCmdString = "Buy";
                                //entryPrice = history.getLastTick(currentInstrument).getAsk();
                                entryPrice = history.getBar(currentInstrument, Period.ONE_MIN, OfferSide.ASK, 0).getClose();
                                //
                              /*  fibEntry23 = getFibLevels(currentInstrument, orderCmdString)[0];
                                 fibEntry38 = getFibLevels(currentInstrument, orderCmdString)[1];
                                 fibEntry50 = getFibLevels(currentInstrument, orderCmdString)[2];
                                 fibEntry68 = getFibLevels(currentInstrument, orderCmdString)[3];*/

                                for (int iValue = 0; iValue < 1; iValue++) {

                                    double tpValue;
                                    if (iValue == 0) {
                                        tpValue = 2;
                                    } else {
                                        tpValue = iValue;
                                    }

                                    printOut.print("globalInstrumentStrenght : " + globalInstrumentStrenght);
                                    placeOrder(orderCmdString, entryPrice, IEngine.OrderCommand.BUY, currentInstrument, iValue, tpValue, correlationOutString + ";;" + parametersString);

                                }

                                /*  placeOrder(orderCmdString, fibEntry23, IEngine.OrderCommand.BUYLIMIT, currentInstrument, 1, correlationOutString);
                                 placeOrder(orderCmdString, fibEntry38, IEngine.OrderCommand.BUYLIMIT, currentInstrument, 2, correlationOutString);
                                 placeOrder(orderCmdString, fibEntry50, IEngine.OrderCommand.BUYLIMIT, currentInstrument, 3, correlationOutString);
                                 placeOrder(orderCmdString, fibEntry68, IEngine.OrderCommand.BUYLIMIT, currentInstrument, 4, correlationOutString);*/
                                /* } else {
                                 //printOut.print(correlationOutString + " not in pivot, don't trade");
                                 }*/
                            }

                        }
                    } catch (JFException ex) {
                        printOut.print(ex);
                    }

                    //printOut.print("BUY : " + Instrument.fromString(instrumentStr));
                }
            }
        }
    }

    private void drawImage(Instrument _instrument) throws JFException {

        IChart currentChart = context.getChart(_instrument);
        BufferedImage image = currentChart.getImage();

        String fileString01 = "C:/Users/Nicolas/Documents/JForex/Strategies/JForexStrategies/reports/currencyIndex2014/tradeCapture/";
        String fileString02 = _instrument + "_" + dateFormatter.format(history.getLastTick(_instrument).getTime()) + "_" + formatToTime.format(history.getLastTick(_instrument).getTime());

        fileString02 = fileString02.replace("/", "_");
        fileString02 = fileString02.replace(":", "_");

        String fileString = fileString01 + fileString02 + ".png";

        try {
            ImageIO.write(image, "png", new File(fileString));
        } catch (IOException ex) {
            printOut.print("Fail");
        }
    }

    // get the chart Opened in JForex client
    private Set<IChart> getOpenedCharts() {

        Set<IChart> openedCharts = new HashSet<IChart>();

        for (Instrument instrument : context.getSubscribedInstruments()) {
            // printOut.print("instrument : " + instrument);

            Set<IChart> setOfCharts = context.getCharts(instrument);
            for (IChart currentChart : setOfCharts) {

                if (currentChart == null) {
                } else {
                    openedCharts.add(currentChart);

                }
            }
        }
        return openedCharts;
    }

    private void placeOrder(String direction, double entryPrice, IEngine.OrderCommand orderCmd, Instrument instrument, int cpt, double _stopLossMultiplier, String comment) throws JFException {

        //drawImage(instrument);
        //double entryPrice = 0;
        double takeProfitPrice = 0;
        double ATRStopLossPrice = 0;
        double stopLossPrice = 0;
        int authorizeTrade = 0;
        // double lastFractalSwing = getLastSwing(direction, instrument, stopLossPeriod, fractalBarsOnside, 1);
        // double stopLossPrice = 0;

        /*  double getPivotSR = 0;

         if (direction.equals("Buy")) {

         getPivotSR = getPivot(instrument, pivotPeriod)[1];

         } else if (direction.equals("Sell")) {

         getPivotSR = getPivot(instrument, pivotPeriod)[2];

         }

         double stopLossPrice = getPivotSR;*/
        /*
         Random rand = new Random();
         int start = 0;
         int end = 5;
         atrSLDEVMultiplier = rand.nextInt(end - start + 1) + start;

         Random rand2 = new Random();
         int start2 = 10;
         int end2 = 12;
         int nombreAleatoire = rand.nextInt(end2 - start2 + 1) + start2;

         double d = new Integer(nombreAleatoire).doubleValue();
         stopLossMultiplier = d / 10;
         */
        double ATRSstopLossDev = getAtrStopLoss(instrument, Period.FOUR_HOURS, direction) * atrSLDEVMultiplier;

        /*  double finalStopLossPrice = roundFloor(getIchimokuValues(instrument, Period.FOUR_HOURS)[1], getInstrumentDecimalMultiplier(instrument));
         double largeStopLossPrice = roundFloor(getIchimokuValues(instrument, Period.DAILY_SUNDAY_IN_MONDAY)[1], getInstrumentDecimalMultiplier(instrument));*/
        if (direction.equals("Buy")) {
            double ichimokuStopLossPrice = roundFloor(getIchimokuValues(instrument, Period.FOUR_HOURS, OfferSide.BID)[2], getInstrumentDecimalMultiplier(instrument));
            /* orderCmd = IEngine.OrderCommand.BUY;
             entryPrice = history.getLastTick(instrument).getAsk();*/

            /* double fibEntry = getFibLevels(instrument, direction)[0];

             if (entryPrice > fibEntry) {
             entryPrice = fibEntry;
             orderCmd = IEngine.OrderCommand.BUYLIMIT;
             }*/
            //stopLossPrice = roundFloor(lastFractalSwing - instrument.getPipValue() * 5, getInstrumentDecimalMultiplier(instrument));
            //stopLossPrice = roundFloor((entryPrice + stopLossPrice) / 2, getInstrumentDecimalMultiplier(instrument));
            //stopLossPrice = roundFloor(history.getBar(instrument, Period.DAILY_SUNDAY_IN_MONDAY, OfferSide.BID, 1).getLow() + instrument.getPipValue() * 10, getInstrumentDecimalMultiplier(instrument));
            ATRStopLossPrice = roundFloor(entryPrice - ATRSstopLossDev, getInstrumentDecimalMultiplier(instrument));
            //stopLossPrice = roundFloor(entryPrice - ATRSstopLoss, getInstrumentDecimalMultiplier(instrument));
            stopLossPrice = getStoplossPrice(instrument, direction, entryPrice, usedSLConfig);
            //stopLossPrice = roundFloor(history.getBar(instrument, Period.DAILY_SUNDAY_IN_MONDAY, OfferSide.BID, 0).getLow() - getAtrStopLoss(instrument, Period.FIFTEEN_MINS, "Buy"), getInstrumentDecimalMultiplier(instrument));
            takeProfitPrice = roundFloor((entryPrice + (entryPrice - stopLossPrice) * stopLossMultiplier), getInstrumentDecimalMultiplier(instrument));

            // Atr SL should always be < ichimoku SL else we are in a range
            if (ichimokuStopLossPrice < ATRStopLossPrice) {
                authorizeTrade = 1;
            }

        } else if (direction.equals("Sell")) {

            double ichimokuStopLossPrice = roundFloor(getIchimokuValues(instrument, Period.FOUR_HOURS, OfferSide.ASK)[2], getInstrumentDecimalMultiplier(instrument));
            /* orderCmd = IEngine.OrderCommand.SELL;
             entryPrice = history.getLastTick(instrument).getBid();*/

            /* double fibEntry = getFibLevels(instrument, direction)[0];

             if (entryPrice < fibEntry) {
             entryPrice = fibEntry;
             orderCmd = IEngine.OrderCommand.SELLLIMIT;
             }*/
            // stopLossPrice = roundFloor(lastFractalSwing + instrument.getPipValue() * 5, getInstrumentDecimalMultiplier(instrument));
            //stopLossPrice = roundFloor((entryPrice + stopLossPrice) / 2, getInstrumentDecimalMultiplier(instrument));
            //stopLossPrice = roundFloor(history.getBar(instrument, Period.DAILY_SUNDAY_IN_MONDAY, OfferSide.ASK, 1).getHigh() + instrument.getPipValue() * 10, getInstrumentDecimalMultiplier(instrument));
            ATRStopLossPrice = roundFloor(entryPrice + ATRSstopLossDev, getInstrumentDecimalMultiplier(instrument));
            //stopLossPrice = roundFloor(entryPrice + ATRSstopLoss, getInstrumentDecimalMultiplier(instrument));
            stopLossPrice = getStoplossPrice(instrument, direction, entryPrice, usedSLConfig);
            // stopLossPrice = roundFloor(history.getBar(instrument, Period.DAILY_SUNDAY_IN_MONDAY, OfferSide.ASK, 0).getHigh() + getAtrStopLoss(instrument, Period.FIFTEEN_MINS, "Buy"), getInstrumentDecimalMultiplier(instrument));
            takeProfitPrice = roundFloor((entryPrice - (stopLossPrice - entryPrice) * stopLossMultiplier), getInstrumentDecimalMultiplier(instrument));

            // Atr SL should always be > ichimoku SL else we are in a range
            if (ichimokuStopLossPrice > ATRStopLossPrice) {
                authorizeTrade = 1;

            }
        }

        if (authorizeTrade == 1) {
            // try {
            double amount = calculateMoneyManagement(instrument, direction, pctRisq, entryPrice, stopLossPrice);
            // double amount = 0.01;
            //

            String orderString = instrument.getPrimaryCurrency().toString() + instrument.getSecondaryCurrency().toString() + history.getTimeOfLastTick(Instrument.EURUSD) + cpt;
            long currentTime = history.getTimeOfLastTick(Instrument.EURUSD);
            printOut.print(":: " + direction + formatToTime.format(currentTime) + " - ORDER : " + direction + "_" + instrument + ", " + orderCmd + "@" + entryPrice + ", " + amount + ", " + 10 + ", " + stopLossPrice + ", " + takeProfitPrice);

            //IOrder myOrder = engine.submitOrder(instrument.getPrimaryCurrency().toString() + instrument.getSecondaryCurrency().toString() + history.getTimeOfLastTick(Instrument.EURUSD), instrument, orderCmd, amount, 0, 10, stopLossPrice, takeProfitPrice);
            IOrder myOrder = engine.submitOrder(orderString, instrument, orderCmd, amount, entryPrice, 10, stopLossPrice, takeProfitPrice, 0, comment);
            // IOrder myOrder = engine.submitOrder(direction + history.getTimeOfLastTick(Instrument.EURUSD), instrument, orderCmd, amount, 0, 10, 0, takeProfitPrice);
            myOrder.waitForUpdate(1000);

            // Drawing the comment on the chart
            if (drawOnChart == 1) {
                IChart currentChart = context.getChart(instrument);
                IChartObjectFactory factory = currentChart.getChartObjectFactory();
                ILabelChartObject objectLabel = factory.createLabel("L" + orderString, currentTime, history.getLastTick(instrument).getBid());
                objectLabel.setText(comment);
                objectLabel.setColor(Color.MAGENTA);
                currentChart.addToMainChart(objectLabel);
            }
            printOut.print("usedPeriodsConfig : " + usedPeriodsConfig + ", stopLossMultiplier : " + stopLossMultiplier + ", atrSLDEVMultiplier : " + atrSLDEVMultiplier);
        }
    }

    // Getting the SL prices
    private double getStoplossPrice(Instrument instrument, String orderDirection, double entryPrice, int typeOfStoploss) throws JFException {

        double SLPrice = 0;
        double securityMargin;
        double ATRSstopLoss;

        switch (typeOfStoploss) {
            // SL at daily HI/Low + 15MIN ATR secutity margin
            case 0:
                securityMargin = getAtrStopLoss(instrument, Period.FIFTEEN_MINS, "Buy");
                if (orderDirection.equals("Buy")) {
                    SLPrice = roundFloor(history.getBar(instrument, Period.DAILY_SUNDAY_IN_MONDAY, OfferSide.BID, 0).getLow() - securityMargin, getInstrumentDecimalMultiplier(instrument));
                } else {
                    SLPrice = roundFloor(history.getBar(instrument, Period.DAILY_SUNDAY_IN_MONDAY, OfferSide.ASK, 0).getHigh() + securityMargin, getInstrumentDecimalMultiplier(instrument));
                }
                break;
            // H4 ATR SL + 10MIN ATR secutity margin
            case 1:
                ATRSstopLoss = getAtrStopLoss(instrument, Period.FOUR_HOURS, orderDirection) * 1;
                securityMargin = getAtrStopLoss(instrument, Period.TEN_MINS, "Buy");
                if (orderDirection.equals("Buy")) {
                    SLPrice = roundFloor(entryPrice - ATRSstopLoss - securityMargin, getInstrumentDecimalMultiplier(instrument));
                } else {
                    SLPrice = roundFloor(entryPrice + ATRSstopLoss + securityMargin, getInstrumentDecimalMultiplier(instrument));
                }
                break;
            // H4 ATR * 2 SL + 10MIN ATR secutity margin
            case 2:
                ATRSstopLoss = getAtrStopLoss(instrument, Period.FOUR_HOURS, orderDirection) * 2;
                securityMargin = getAtrStopLoss(instrument, Period.TEN_MINS, "Buy");
                if (orderDirection.equals("Buy")) {
                    SLPrice = roundFloor(entryPrice - ATRSstopLoss - securityMargin, getInstrumentDecimalMultiplier(instrument));
                } else {
                    SLPrice = roundFloor(entryPrice + ATRSstopLoss + securityMargin, getInstrumentDecimalMultiplier(instrument));
                }
                break;
            // SL at Ichimoku Ki-Jun Sen (blue) adapted to the current period + 5MIN ATR secutity margin
            case 3:
                securityMargin = getAtrStopLoss(instrument, Period.FIVE_MINS, "Buy");
                if (orderDirection.equals("Buy")) {
                    SLPrice = roundFloor(getIchimokuValues(instrument, getUsedPeriods(usedPeriodsConfig)[0], OfferSide.BID)[1] - securityMargin, getInstrumentDecimalMultiplier(instrument));
                } else {
                    SLPrice = roundFloor(getIchimokuValues(instrument, getUsedPeriods(usedPeriodsConfig)[0], OfferSide.ASK)[1] + securityMargin, getInstrumentDecimalMultiplier(instrument));
                }
                break;
            // SL at Ichimoku SenkuA (red) adapted to the current period + 5MIN ATR secutity margin
            case 4:
                securityMargin = getAtrStopLoss(instrument, Period.FIVE_MINS, "Buy");
                if (orderDirection.equals("Buy")) {
                    SLPrice = roundFloor(getIchimokuValues(instrument, getUsedPeriods(usedPeriodsConfig)[0], OfferSide.BID)[2] - securityMargin, getInstrumentDecimalMultiplier(instrument));
                } else {
                    SLPrice = roundFloor(getIchimokuValues(instrument, getUsedPeriods(usedPeriodsConfig)[0], OfferSide.ASK)[2] + securityMargin, getInstrumentDecimalMultiplier(instrument));
                }
                break;
            // SL at daily ATR + 10MIN ATR secutity margin
            case 5:
                ATRSstopLoss = getAtrStopLoss(instrument, Period.DAILY_SUNDAY_IN_MONDAY, orderDirection);
                securityMargin = getAtrStopLoss(instrument, Period.TEN_MINS, "Buy");
                if (orderDirection.equals("Buy")) {
                    SLPrice = roundFloor(entryPrice - ATRSstopLoss - securityMargin, getInstrumentDecimalMultiplier(instrument));
                } else {
                    SLPrice = roundFloor(entryPrice + ATRSstopLoss + securityMargin, getInstrumentDecimalMultiplier(instrument));
                }
                break;
            case 6:

                break;
            case 7:

                break;
            case 8:

                // ALL THIS TO DEBUG !!! Check the 2 fractal calculations
                SLPrice = roundFloor(getLastSwing(orderDirection, instrument, Period.FOUR_HOURS, 3, 1), getInstrumentDecimalMultiplier(instrument));

                // SL at last fractal swing HI/Low + 15MIN ATR secutity margin
                //  securityMargin = getAtrStopLoss(instrument, Period.FIVE_MINS, "Buy");
                if (orderDirection.equals("Buy")) {
                    SLPrice = getLastMinMaxFractals(instrument, Period.FOUR_HOURS, OfferSide.BID, 3, com.dukascopy.api.Filter.ALL_FLATS, 1, "min")[0];
                } else {
                    SLPrice = getLastMinMaxFractals(instrument, Period.FOUR_HOURS, OfferSide.ASK, 3, com.dukascopy.api.Filter.ALL_FLATS, 1, "max")[0];
                }

                double ATRSstopLoss2 = getAtrStopLoss(instrument, Period.DAILY_SUNDAY_IN_MONDAY, orderDirection) * 1;
                if (orderDirection.equals("Buy")) {
                    SLPrice = roundFloor(entryPrice - ATRSstopLoss2, getInstrumentDecimalMultiplier(instrument));
                } else {
                    SLPrice = roundFloor(entryPrice + ATRSstopLoss2, getInstrumentDecimalMultiplier(instrument));
                }

                //UsedPeriods[1] = Period.DAILY_SUNDAY_IN_MONDAY;
                break;
            case 9:

                break;
        }

        return SLPrice;
    }

    // Finding if there are orders opened for the given instrument
    private int testOpenedOrders(Instrument instrument) throws JFException {

        int result = 0;
        int maxNbPairs = strongCurrencyList.size() * weakCurrencyList.size();

        //printOut.print(engine.getOrders().size() +"<"+ maxNbPairs+" && "+engine.getOrders(instrument).isEmpty());
        if (engine.getOrders().size() < maxNbPairs && engine.getOrders(instrument).isEmpty()) {
            result = 0;
        } else {
            result = 1;
        }
        return result;
    }

    // Finding if there are orders opened
    private int[] getOrderDetails(Instrument instrument) throws JFException {

        int orderDetails[] = new int[3];
        List<IOrder> orderList;
        int nbLong = 0;
        int nbShort = 0;
        int nbTotal = 0;

        if (instrument != null) {
            orderList = engine.getOrders(instrument);
        } else {
            orderList = engine.getOrders();
        }

        for (IOrder order : orderList) {
            if (order.getState() == IOrder.State.FILLED) {
                if (order.isLong()) {
                    nbLong++;
                    orderDetails[0] = nbLong;
                } else {
                    nbShort++;
                    orderDetails[1] = nbShort;
                }
            }
        }
        nbTotal = nbLong + nbShort;
        orderDetails[2] = nbTotal;
        return orderDetails;
    }

    //
    // getLastMinMaxFractals
    private double[] getLastMinMaxFractals(Instrument instrument, Period period, OfferSide side, int barsOnSide, com.dukascopy.api.Filter filter, int shiftStart, String minMax) {

        lastFractalMax = Double.NaN;
        lastFractalMin = Double.NaN;

        int i = shiftStart;
        int minShift = shiftStart;
        int maxShift = shiftStart;
        double[][] fractal = null;
        IBar currentFractalBar = null;
        //
        // nicoForexUtils.print(enablePrint, "getLastMinMaxFractals shiftStart : " + shiftStart);
        //
            /*try {
         IChart chart = context.getChart(instrument);
         chart.draw(history.getBar(instrument, period, OfferSide.BID, shiftStart).getTime() + "BUY", IChart.Type.SIGNAL_UP, history.getBar(instrument, period, OfferSide.BID, shiftStart).getTime(), history.getLastTick(instrument).getBid());

         } catch (JFException ex) {
         print(enablePrint, "msg : " + ex);
         }*/

        while (lastFractalMax.isNaN() || lastFractalMin.isNaN()) {

            try {
                currentFractalBar = history.getBar(instrument, period, OfferSide.BID, i);
                // nicoForexUtils.print(enablePrint, "currentFractalBar : " + currentFractalBar.getClose());

                fractal = indicators.fractal(instrument, period, side, barsOnSide, filter, 1, currentFractalBar.getTime(), 0);
                /* nicoForexUtils.print(enablePrint, "*****" + i);
                 nicoForexUtils.print(enablePrint, "fractal : " + fractal);
                 nicoForexUtils.print(enablePrint, "fractal[0].length : " + fractal[0].length);
                 nicoForexUtils.print(enablePrint, "fractal[1].length : " + fractal[1].length);*/

                // fractal = indicators.fractal(instrument, period, side, barsOnSide, i);
            } catch (JFException ex) {
                printOut.print("msg : " + ex);
            }

            if (fractal[0].length > 0 && !Double.isNaN(fractal[0][0])) {
                lastFractalMax = fractal[0][0];
                maxShift = i + 1;
            }
            if (fractal[1].length > 0 && !Double.isNaN(fractal[1][0])) {
                lastFractalMin = fractal[1][0];
                minShift = i + 1;
            }
            i++;
        }
        if (minMax.equals("min")) {
            double[] result = {(double) lastFractalMin, (double) minShift};
            return result;
        } else {
            double[] result = {(double) lastFractalMax, (double) maxShift};
            return result;
        }
    }

    private double getLastSwing(String __orderDirection, Instrument instrument, Period period, int barsOnSide, int nbFractalsBack) throws JFException {

        double lastFractalMax = Double.NaN;
        double lastFractalMin = Double.NaN;
        int i = 0;
        int fractalCount = 0;

        //fractalCount ++;
        if (__orderDirection.equals("Buy")) {

            while (fractalCount < nbFractalsBack) {
                double[] fractal = null;

//               IBar currentBar = history.getBar(instrument, period, OfferSide.BID, i);
//               IBar currentBarMinus = history.getBar(instrument, period, OfferSide.BID, i + 1);
                try {
                    fractal = indicators.fractal(instrument, period, OfferSide.BID, barsOnSide, i);
                    // fractal = indicators.fractal(instrument, period, OfferSide.BID, barsOnSide, com.dukascopy.api.Filter.NO_FILTER, history.getBarStart(period, currentBar.getTime()), history.getBarStart(period, currentBarMinus.getTime()));
                    // fractal = indicators.fractal(instrument, period, OfferSide.BID, barsOnSide, com.dukascopy.api.Filter.ALL_FLATS, i, history.getStartTimeOfCurrentBar(instrument, period), 0);
                } catch (JFException ex) {
                    printOut.print("msg : " + ex);
                }
                if (!Double.isNaN(fractal[1])) {
                    lastFractalMin = fractal[1];
                    fractalCount++;
                }
                i++;
            }
            return lastFractalMin;

        } else {

            while (fractalCount < nbFractalsBack) {
                double[] fractal = null;
                try {
                    //IBar currentBar = history.getBar(instrument, period, OfferSide.BID, i);
                    // IBar currentBarMinus = history.getBar(instrument, period, OfferSide.BID, i + 1);
                    fractal = indicators.fractal(instrument, period, OfferSide.ASK, barsOnSide, i);
                    //fractal = indicators.fractal(instrument, period, OfferSide.BID, barsOnSide, com.dukascopy.api.Filter.NO_FILTER, history.getBarStart(period, currentBar.getTime()), history.getBarStart(period, currentBarMinus.getTime()));
                    // fractal = indicators.fractal(instrument, period, OfferSide.BID, barsOnSide, com.dukascopy.api.Filter.ALL_FLATS, i, history.getStartTimeOfCurrentBar(instrument, period), 0);
                } catch (JFException ex) {
                    printOut.print("msg : " + ex);
                }
                if (!Double.isNaN(fractal[0])) {
                    lastFractalMax = fractal[0];
                    fractalCount++;
                }
                i++;
            }
            return lastFractalMax;
        }
    }

    // Calculating the correlation between currency pairs
    private int[][] getCurrencyCorrelations(int _usedPeriodIndex) {

        // printOut.print("getCurrencyCorrelations()");
        int correlationResults[][] = new int[getUsedPeriods(_usedPeriodIndex).length][CurrenciesToWatch.length];
        int timeFrameCorrelationResult[][] = new int[CurrenciesToWatch.length][2];
        int globalCorrelationResult[][] = new int[CurrenciesToWatch.length][2];
        int trendPonderation = 0;
        int SARPonderation = 0;
        int pairShiftPonderation = 0;
        int pivotPonderation = 0;
        int totalPonderation = 0;
        int cpt = 0;

        // Looping into periods and instruments
        for (int nbPeriod = 0; nbPeriod < getUsedPeriods(_usedPeriodIndex).length; nbPeriod++) {
            // printOut.print("_");
            // printOut.print("**************************************************** Period : " + UsedPeriods[nbPeriod] + "****************************************************");
            cpt++;

            // Bulding the instruments to test
            for (int nbCurrency1 = 0; nbCurrency1 < CurrenciesToWatch.length; nbCurrency1++) {
                trendPonderation = 0;
                SARPonderation = 0;
                pairShiftPonderation = 0;
                pivotPonderation = 0;
                totalPonderation = 0;

                for (int nbCurrency2 = 0; nbCurrency2 < CurrenciesToWatch.length; nbCurrency2++) {

                    if (!CurrenciesToWatch[nbCurrency1].equals(CurrenciesToWatch[nbCurrency2])) {

                        String instrumentStr = CurrenciesToWatch[nbCurrency1] + "/" + CurrenciesToWatch[nbCurrency2];

                        String correctInstrumentStr = "";
                        int invertPonderation = 0;
                        // Verifying if the pair is named correctly
                        if (Instrument.isInverted(instrumentStr)) {
                            correctInstrumentStr = CurrenciesToWatch[nbCurrency2] + "/" + CurrenciesToWatch[nbCurrency1];
                            invertPonderation = 1;
                        } else {
                            correctInstrumentStr = instrumentStr;
                            invertPonderation = 0;
                        }

                        Instrument correctInstrument = Instrument.fromString(correctInstrumentStr);

                        int currentMATrend01 = 0;
                        int currentMATrend02 = 0;
                        int currentMATrend03 = 0;
                        int currentMATrend = 0;
                        int currentTrend = 0;
                        int currentSAR = 0;
                        int currentPivot = 0;
                        double currentPairShift = 0;
                        double dailyPairShift = 0;
                        int currentPairShiftRound = 0;
                        double currentPivotValue = 0;
                        double currentPrice = 0;

                        try {
                            currentTrend = getIchimokuTrend(correctInstrument, getUsedPeriods(_usedPeriodIndex)[nbPeriod]);
                            //currentTrend = getAlligatorTrend(correctInstrument, getUsedPeriods(_usedPeriodIndex)[nbPeriod]);
                            // currentSAR = getSAR(correctInstrument, getUsedPeriods(_usedPeriodIndex)[nbPeriod]);

                            if (nbPeriod == getUsedPeriods(_usedPeriodIndex).length - 1) {
                                //currentPairShift = getPairShift(correctInstrument, Period.DAILY_SUNDAY_IN_MONDAY);
                                //currentPairShift = getDailyPairShift(correctInstrument, 0);
                                //printOut.print("correctInstrument shift = " + correctInstrument + " = " + currentPairShift + " %");
                                if (currentPairShift > 0) {
                                    currentPairShiftRound = 10;
                                } else if (currentPairShift < 0) {
                                    currentPairShiftRound = -10;
                                } else {
                                    currentPairShiftRound = 0;
                                }
                            }

                            // currentPivotValue = getPivot(correctInstrument, Period.DAILY)[0];
                            //currentPrice = history.getLastTick(correctInstrument).getBid();
                            //currentMATrend01 = getMATrend(correctInstrument, getUsedPeriods(_usedPeriodIndex)[nbPeriod], 20);
                            // currentMATrend02 = getMATrend(correctInstrument, getUsedPeriods(_usedPeriodIndex)[nbPeriod], 100);
                            // currentMATrend03 = getMATrend(correctInstrument, getUsedPeriods(_usedPeriodIndex)[nbPeriod], 200);
                            //currentMATrend = currentMATrend01;
                            // Testing if the price is above or under the daily pivot
                          /*  if (currentPrice > currentPivotValue) {
                             currentPivot = 10;
                             } else if (currentPrice < currentPivotValue) {
                             currentPivot = -10;
                             } else {
                             currentPivot = 0;
                             }*/
                            //  printOut.print(" - Current trend : " + currentTrend);
                        } catch (JFException ex) {
                            printOut.print("Exception : " + ex);
                        }

                        if (invertPonderation == 1) {
                            //trendPonderation = trendPonderation - currentMATrend;
                            trendPonderation = trendPonderation - currentTrend - currentMATrend;
                            SARPonderation = SARPonderation - currentSAR;
                            pairShiftPonderation = pairShiftPonderation - currentPairShiftRound;
                            // pivotPonderation = pivotPonderation - currentPivot;
                        } else {
                            //trendPonderation = trendPonderation + currentMATrend;
                            trendPonderation = trendPonderation + currentTrend + currentMATrend;
                            SARPonderation = SARPonderation + currentSAR;
                            pairShiftPonderation = pairShiftPonderation + currentPairShiftRound;
                            //  pivotPonderation = pivotPonderation + currentPivot;
                        }

                        // Filtering only the pairs on the good side of the pivot
                       /* double tmpPonderation = currentTrend + currentSAR + currentPairShiftRound;
                         if ((tmpPonderation > 0 && currentPivot < 0) || (tmpPonderation < 0 && currentPivot > 0)) {
                         // printOut.print("Don't ponderate");
                         } else {*/
                        totalPonderation = trendPonderation + SARPonderation + pairShiftPonderation;
                        // }

//                        try {
//                            printOut.print(getUsedPeriods(_usedPeriodIndex)[nbPeriod] + " - " + correctInstrument + " - currentTrend : " + currentTrend + " - currentSAR : " + currentSAR + " - currentPairShift : " + currentPairShift + "% - dailyPairShift : " + dailyPairShift + "% - PRICE : " + history.getLastTick(correctInstrument).getBid()/* + " , PIVOT : " + getPivot(correctInstrument)[0] + " CurrentPivot : " + currentPivot*/);
//                        } catch (JFException ex) {
//                            printOut.print("Exception : " + ex);
//                        }
                    }
                }
                correlationResults[nbPeriod][nbCurrency1] = totalPonderation;
                //  printOut.print("////////////////////////////////////////////  " + CurrenciesToWatch[nbCurrency1] + " PONDERATION :  trendPonderation " + trendPonderation + " - SARPonderation " + SARPonderation + " - pairShiftPonderation " + pairShiftPonderation + " - pivotPonderation " + pivotPonderation + " ----> totalPonderation " + totalPonderation);
            }
        }

        // Creating the final ordered array from the strongest to the weakest Currency
        int tmpGlobalPondaration = 0;
        for (int j = 0; j < CurrenciesToWatch.length; j++) {
            tmpGlobalPondaration = 0;
            for (int i = 0; i < getUsedPeriods(_usedPeriodIndex).length; i++) {
                tmpGlobalPondaration = tmpGlobalPondaration + correlationResults[i][j];
                // printOut.print(CurrenciesToWatch[j] +" : "+ tmpGlobalPondaration);
                //  printOut.print("correlationResults[" + i + "][" + j + "]" + correlationResults[i][j]);
                //  printOut.print("correlationResults[" + getUsedPeriods(_usedPeriodIndex)[i] + "][" + CurrenciesToWatch[i] + "]" + correlationResults[i][j]);
            }
            globalCorrelationResult[j][0] = j;
            globalCorrelationResult[j][1] = tmpGlobalPondaration;
        }

        Arrays.sort(globalCorrelationResult, new Comparator<int[]>() {

            @Override
            public int compare(int[] o1, int[] o2) {
                return ((Integer) o2[1]).compareTo(o1[1]);
            }
        });

        // Outputing the result on the different timeframes
        int tmpPonderation = 0;

        for (int i = 0; i < getUsedPeriods(_usedPeriodIndex).length; i++) {
            for (int j = 0; j < CurrenciesToWatch.length; j++) {
                tmpPonderation = 0;

                tmpPonderation = tmpPonderation + correlationResults[i][j];
                //printOut.print(CurrenciesToWatch[j] + " : " + tmpPonderation);
                // printOut.print("correlationResults[" + i + "][" + j + "]" + correlationResults[i][j]);
                // printOut.print("correlationResults[" + getUsedPeriods(_usedPeriodIndex)[i] + "][" + CurrenciesToWatch[j] + "]" + correlationResults[i][j]);
                timeFrameCorrelationResult[j][0] = j;
                timeFrameCorrelationResult[j][1] = tmpPonderation;
            }
            Arrays.sort(timeFrameCorrelationResult, new Comparator<int[]>() {

                @Override
                public int compare(int[] o1, int[] o2) {
                    return ((Integer) o2[1]).compareTo(o1[1]);
                }
            });

            /*    for (int i = 0; i < bestCurrenciesToTrade; i++) {
             strongCurrencyList.add(CurrenciesToWatch[correlationTable[i][0]]);
             strongCurrencyListStrength.add(correlationTable[i][1]);
             correlationOutString = correlationOutString + " " + CurrenciesToWatch[correlationTable[i][0]] + " " + correlationTable[i][1] + " | ";
             }

             for (int i = CurrenciesToWatch.length - bestCurrenciesToTrade; i < CurrenciesToWatch.length; i++) {
             weakCurrencyList.add(CurrenciesToWatch[correlationTable[i][0]]);
             weakCurrencyListStrength.add(correlationTable[i][1]);
             correlationOutString = correlationOutString + " " + CurrenciesToWatch[correlationTable[i][0]] + " " + correlationTable[i][1] + " | ";
             }*/
            // Outputing the global result
            String outString = "";
            for (int x = 0; x < CurrenciesToWatch.length; x++) {
                outString = outString + " " + CurrenciesToWatch[timeFrameCorrelationResult[x][0]] + " " + timeFrameCorrelationResult[x][1] + " | ";
            }
            // printOut.print(outString + " ----> " + getUsedPeriods(_usedPeriodIndex)[i]);

        }

        // Outputing the global result
        String outString = "";
        // printOut.print("-");
        for (int i = 0; i < CurrenciesToWatch.length; i++) {
            outString = outString + " " + CurrenciesToWatch[globalCorrelationResult[i][0]] + " " + globalCorrelationResult[i][1] + " | ";
        }
        //  printOut.print(outString + " ----> GLOBAL");
        //  printOut.print("-");
        return globalCorrelationResult;
    }

    // Getting the shift in a pair for a time period
    public double getPairShift(Instrument instrument, Period period) throws JFException {

        // Converting the time in millisconds
        Unit unitValue = period.getUnit();
        double unitMillisec = unitValue.getInterval();
        int nbUnits = period.getNumOfUnits();
        double lookBackinTime = unitMillisec * nbUnits;

        // printOut.print("****** lookBackinTime : " + lookBackinTime);
        double currentPrice = history.getLastTick(instrument).getBid();
        double currentTime = history.getTimeOfLastTick(instrument);

        //
        // ATTENTION !!! POUR LE DAILY METTRE UN LOOKBACK A L'OUVERTURE DE LA BOUGIE, a voir.... pas sur que c'est bien...
        //
        IBar lookBackBar = history.getBar(instrument, Period.TEN_SECS, OfferSide.BID, (int) Math.floor(lookBackinTime / 10000));
        double lookBackPrice = lookBackBar.getOpen();

        double shiftInPrice = roundFloor(((currentPrice - lookBackPrice) / lookBackPrice) * 100, 100);

        //printOut.print(instrument + " getPairShift lookBackPriceOpen : "+currentPrice + " lookBackPriceClose : "+lookBackPrice + "shiftInPrice : "+shiftInPrice);
        if (drawOnChart == 1) {
            chart = context.getChart(instrument);
            chart.draw(period + "_" + instrument, IChart.Type.SIGNAL_UP, lookBackBar.getTime(), lookBackPrice);
        }

        return shiftInPrice; // printOut.print("****** unitValue : " + unitValue + " - unitMillisec : " + unitMillisec + " - nbUnits : " + nbUnits);
    }

    // Getting the shift in a pair for a time period
    public double getDailyPairShift(Instrument instrument, int _shift) throws JFException {

        IBar lookBackBar = history.getBar(instrument, Period.DAILY_SUNDAY_IN_MONDAY, OfferSide.BID, _shift);
        double lookBackPriceOpen = lookBackBar.getOpen();
        double lookBackPriceClose = lookBackBar.getClose();

        double shiftInPrice = roundFloor(((lookBackPriceClose - lookBackPriceOpen) / lookBackPriceOpen) * 100, 100);

        //printOut.print(instrument + " getDailyPairShift lookBackPriceOpen : " + lookBackPriceOpen + " lookBackPriceClose : " + lookBackPriceClose + "shiftInPrice : " + shiftInPrice);
        return shiftInPrice; // printOut.print("****** unitValue : " + unitValue + " - unitMillisec : " + unitMillisec + " - nbUnits : " + nbUnits);
    }

    public double[] getIchimokuValues(Instrument instrument, Period _period, OfferSide _offerside) throws JFException {

        double[] ichimokuValues = new double[10];

        ITick lastTick = history.getLastTick(instrument);

        double[][] myIchimuku = indicators.ichimoku(instrument, _period, _offerside, 9, 26, 52, com.dukascopy.api.Filter.WEEKENDS, 1, lastTick.getTime(), 0);
        double[][] myIchimuku26 = indicators.ichimoku(instrument, _period, _offerside, 9, 26, 52, com.dukascopy.api.Filter.WEEKENDS, 27, lastTick.getTime(), 0);
        double[][] myIchimuku52 = indicators.ichimoku(instrument, _period, _offerside, 9, 26, 52, com.dukascopy.api.Filter.WEEKENDS, 53, lastTick.getTime(), 0);

        double tenkanSen = myIchimuku[0][0];
        double kiJunSen = myIchimuku[1][0];
        double senkouA26 = myIchimuku26[3][0];
        double senkouB26 = myIchimuku26[4][0];
        double senkouA52 = myIchimuku52[3][0];
        double senkouB52 = myIchimuku52[4][0];
        double chinkuSpan = myIchimuku[2][0];
        double senkouA = myIchimuku[3][0];
        double senkouB = myIchimuku[4][0];
        double cloud = myIchimuku[5][0];

        ichimokuValues[0] = tenkanSen; // Yellow
        ichimokuValues[1] = kiJunSen; // blue
        ichimokuValues[2] = senkouA26; // red
        ichimokuValues[3] = senkouB26; // green
        ichimokuValues[4] = senkouA52; // red
        ichimokuValues[5] = senkouB52; // green
        ichimokuValues[6] = chinkuSpan; // pink
        ichimokuValues[7] = senkouA; // red
        ichimokuValues[8] = senkouB; // green
        ichimokuValues[9] = cloud;

        /*
         print("currentPrice : " + currentPrice);
         print("currentPriceShift : " + currentPriceShift);
         print("tenkanSen : " + myIchimuku[0][0]);
         print("kiJunSen : " + myIchimuku[1][0]);
         print("senkouA26 : " + myIchimuku26[3][0]);
         print("senkouB26 : " + myIchimuku26[4][0]);
         print("senkouA52 : " + myIchimuku52[3][0]);
         print("senkouB52 : " + myIchimuku52[4][0]);
         print("chinkuSpan : " + myIchimuku[2][0]);
         */
        return ichimokuValues;
    }

    public int getIchimokuTrend(Instrument instrument, Period _period) throws JFException {

        int trendResult = 0;

        ITick lastTick = history.getLastTick(instrument);
        double currentPrice = lastTick.getBid();
        double currentPriceShift = history.getBar(instrument, _period, OfferSide.BID, 26).getClose();

        //double[] myMa = indicators.sma(instrument, _period, OfferSide.BID, IIndicators.AppliedPrice.MEDIAN_PRICE, maPeriod, com.dukascopy.api.Filter.ALL_FLATS, 1, lastTick.getTime(), 0);
        double[][] myIchimuku = indicators.ichimoku(instrument, _period, OfferSide.BID, 9, 26, 52, com.dukascopy.api.Filter.WEEKENDS, 1, lastTick.getTime(), 0);
        double[][] myIchimuku26 = indicators.ichimoku(instrument, _period, OfferSide.BID, 9, 26, 52, com.dukascopy.api.Filter.WEEKENDS, 27, lastTick.getTime(), 0);
        double[][] myIchimuku52 = indicators.ichimoku(instrument, _period, OfferSide.BID, 9, 26, 52, com.dukascopy.api.Filter.WEEKENDS, 53, lastTick.getTime(), 0);

        double tenkanSen = myIchimuku[0][0];
        double kiJunSen = myIchimuku[1][0];
        double senkouA26 = myIchimuku26[3][0];
        double senkouB26 = myIchimuku26[4][0];
        double senkouA52 = myIchimuku52[3][0];
        double senkouB52 = myIchimuku52[4][0];
        double chinkuSpan = myIchimuku[2][0];
        double senkouA = myIchimuku[3][0];
        double senkouB = myIchimuku[4][0];
        double cloud = myIchimuku[5][0];

        if (tenkanSen > kiJunSen) {
            trendResult = trendResult + 10;
        } else if (tenkanSen < kiJunSen) {
            trendResult = trendResult - 10;
        }
        if (currentPrice > senkouA26 && currentPrice > senkouB26) {
            trendResult = trendResult + 10;
        } else if (currentPrice < senkouA26 && currentPrice < senkouB26) {
            trendResult = trendResult - 10;
        }
        if (chinkuSpan > senkouA52 && chinkuSpan > senkouB52 && chinkuSpan > currentPriceShift) {
            trendResult = trendResult + 10;
        } else if (chinkuSpan < senkouA52 && chinkuSpan < senkouB52 && chinkuSpan < currentPriceShift) {
            trendResult = trendResult - 10;
        }

        // Buy signal
       /* if ((tenkanSen > kiJunSen) && (currentPrice > senkouA26 && currentPrice > senkouB26) && (chinkuSpan > senkouA52 && chinkuSpan > senkouB52 && chinkuSpan > currentPriceShift)) {
         trendResult = 10;
         } // Sell signal
         else if ((tenkanSen < kiJunSen) && (currentPrice < senkouA26 && currentPrice < senkouB26) && (chinkuSpan < senkouA52 && chinkuSpan < senkouB52 && chinkuSpan < currentPriceShift)) {
         trendResult = -10;
         } else {
         trendResult = 0;
         }*/
        /*
         print("currentPrice : " + currentPrice);
         print("currentPriceShift : " + currentPriceShift);
         print("tenkanSen : " + myIchimuku[0][0]);
         print("kiJunSen : " + myIchimuku[1][0]);
         print("senkouA26 : " + myIchimuku26[3][0]);
         print("senkouB26 : " + myIchimuku26[4][0]);
         print("senkouA52 : " + myIchimuku52[3][0]);
         print("senkouB52 : " + myIchimuku52[4][0]);
         print("chinkuSpan : " + myIchimuku[2][0]);
         */
        return trendResult;
    }

    public int getAlligatorTrend(Instrument instrument, Period _period) throws JFException {

        //Period period = Period.getBasicPeriodForCustom(_period);
        // printOut.print("****** getAlligatorTrend period : " + period);
        int trendResult = 0;

        double[] myAlligator1 = indicators.alligator(instrument, _period, OfferSide.BID, IIndicators.AppliedPrice.MEDIAN_PRICE, 13, 8, 5, 1);
        //double[] myAlligator1 = indicators.alligator(instrument, _period, OfferSide.BID, IIndicators.AppliedPrice.MEDIAN_PRICE, 6, 4, 2, 1);

        double alligatorJaw1 = myAlligator1[0];
        double alligatorTeeth1 = myAlligator1[1];
        double alligatorLips1 = myAlligator1[2];

        if (alligatorJaw1 < alligatorTeeth1 && alligatorTeeth1 < alligatorLips1) {

            // printOut.print("alligatorJaw1:"+alligatorJaw1+" < alligatorTeeth1:"+alligatorTeeth1+" && alligatorTeeth1:"+alligatorTeeth1 +" < alligatorLips1:"+alligatorLips1);
            trendResult = 10;
        } else if (alligatorJaw1 > alligatorTeeth1 && alligatorTeeth1 > alligatorLips1) {

            // printOut.print("alligatorJaw1:"+alligatorJaw1+" > alligatorTeeth1:"+alligatorTeeth1+" && alligatorTeeth1:"+alligatorTeeth1 +" > alligatorLips1:"+alligatorLips1);
            trendResult = -10;
        } else {
            trendResult = 0;
        }

        // printOut.print("getAlligatorTrend() : Period" + _period + " - Instrument : " + instrument + " - trendResult : " + trendResult);
        return trendResult;
    }

    public int getMATrend(Instrument instrument, Period _period, int maPeriod) throws JFException {

        int trendResult = 0;

        ITick lastTick = history.getLastTick(instrument);

        double[] myMa = indicators.sma(instrument, _period, OfferSide.BID, IIndicators.AppliedPrice.MEDIAN_PRICE, maPeriod, com.dukascopy.api.Filter.ALL_FLATS, 1, lastTick.getTime(), 0);

        if (lastTick.getBid() > myMa[0]) {
            trendResult = 10;
        } else {
            trendResult = -10;
        }
        return trendResult;
    }

    public int getSAR(Instrument instrument, Period _period) throws JFException {

        //Period period = Period.getBasicPeriodForCustom(_period);
        // printOut.print("****** getSAR() period : " + period);
        int trendResult = 0;
        double mySAR01 = 0;

        mySAR01 = indicators.sar(instrument, _period, OfferSide.BID, 0.02, 0.2, 0);

        if (mySAR01 > history.getLastTick(instrument).getBid()) {
            trendResult = -10;
        } else if (mySAR01 < history.getLastTick(instrument).getBid()) {
            trendResult = 10;
        }

        return trendResult;
    }

    // MACD Acceleration Indicator
    private int getATRSMA(Instrument _instrument, Period _period) throws JFException {

        int volatilityON = 0;

        Object[] vals = context.getIndicators().calculateIndicator(
                _instrument,
                _period,
                new OfferSide[]{OfferSide.BID},
                "ATRSMA",
                new AppliedPrice[]{AppliedPrice.CLOSE},
                new Object[]{14, 20},
                com.dukascopy.api.Filter.ALL_FLATS,
                1, history.getLastTick(_instrument).getTime(), 0);

        double[] ATRValue = (double[]) vals[0];
        double[] SMAValue = (double[]) vals[1];

        // Verifying if an acceleration has occured
        if (ATRValue[0] > SMAValue[0]) {
            volatilityON = 10;
        } else if (ATRValue[0] < SMAValue[0]) {
            volatilityON = -10;
        }

        //printOut.print("volatilityON : " + volatilityON);
        return volatilityON;
    }

    //
    // Calculation of pivot points
    private double[] getPivot(Instrument instrument, Period period) throws JFException {

        IBar currentBar = this.history.getBar(instrument, period, OfferSide.BID, 0);

        GregorianCalendar currentCalMillisec = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        currentCalMillisec.setTimeInMillis(currentBar.getTime());
        int currentDayOfWeek = currentCalMillisec.get(Calendar.DAY_OF_WEEK);

        List<IBar> lastSessionBars;
        // Ignoring sundays for pivot calculation
        if (period == Period.DAILY && currentDayOfWeek == Calendar.MONDAY) {
            lastSessionBars = this.history.getBars(instrument, period, OfferSide.BID, com.dukascopy.api.Filter.ALL_FLATS, 3, currentBar.getTime(), 0);
        } else {
            lastSessionBars = this.history.getBars(instrument, period, OfferSide.BID, com.dukascopy.api.Filter.ALL_FLATS, 2, currentBar.getTime(), 0);
        }

        IBar lastSessionBar = lastSessionBars.get(0);

        double lastSessionHigh = roundFloor(lastSessionBar.getHigh(), getInstrumentDecimalMultiplier(instrument));
        double lastSessionLow = roundFloor(lastSessionBar.getLow(), getInstrumentDecimalMultiplier(instrument));
        double lastSessionClose = roundFloor(lastSessionBar.getClose(), getInstrumentDecimalMultiplier(instrument));

        double pivot = roundFloor((lastSessionHigh + lastSessionLow + lastSessionClose) / 3, getInstrumentDecimalMultiplier(instrument));
        double s1Pivot = roundFloor((2 * pivot) - lastSessionHigh, getInstrumentDecimalMultiplier(instrument));
        double r1Pivot = roundFloor((2 * pivot) - lastSessionLow, getInstrumentDecimalMultiplier(instrument));
        double s2Pivot = roundFloor(pivot - (r1Pivot - s1Pivot), getInstrumentDecimalMultiplier(instrument));
        double r2Pivot = roundFloor((pivot - s1Pivot) + r1Pivot, getInstrumentDecimalMultiplier(instrument));


        /*console.getOut().println("getPivot() : " + pivot);
         console.getOut().println("getPivot()s1Pivot : "+s1Pivot);
         console.getOut().println("getPivot()r1Pivot : "+r1Pivot);
         console.getOut().println("getPivot()s2Pivot : "+s2Pivot);
         console.getOut().println("getPivot()r2Pivot : "+r2Pivot);*/
        double[] pivotPoints = {pivot, s1Pivot, r1Pivot, s2Pivot, r2Pivot};

        return pivotPoints;
    }
    //
    //

    private Period[] getUsedPeriods(int _usedPeriodsConfig) {
        //
        Period[] UsedPeriods = null;
        //
        switch (_usedPeriodsConfig) {
            case 0:
                UsedPeriods = new Period[1];
                UsedPeriods[0] = Period.FIVE_MINS;
                break;
            case 1:
                UsedPeriods = new Period[1];
                UsedPeriods[0] = Period.FIFTEEN_MINS;
                break;
            case 2:
                UsedPeriods = new Period[1];
                UsedPeriods[0] = Period.THIRTY_MINS;
                break;
            case 3:
                UsedPeriods = new Period[1];
                UsedPeriods[0] = Period.ONE_HOUR;
                break;
            case 4:
                UsedPeriods = new Period[1];
                UsedPeriods[0] = Period.FOUR_HOURS;
                break;
            case 5:
                UsedPeriods = new Period[1];
                UsedPeriods[0] = Period.DAILY_SUNDAY_IN_MONDAY;
                break;
            case 6:
                UsedPeriods = new Period[2];
                UsedPeriods[0] = Period.FIFTEEN_MINS;
                UsedPeriods[1] = Period.ONE_HOUR;
                break;
            case 7:
                UsedPeriods = new Period[3];
                UsedPeriods[0] = Period.FIFTEEN_MINS;
                UsedPeriods[1] = Period.ONE_HOUR;
                UsedPeriods[2] = Period.FOUR_HOURS;
                //UsedPeriods[1] = Period.DAILY_SUNDAY_IN_MONDAY;
                break;
            case 8:
                UsedPeriods = new Period[2];
                UsedPeriods[0] = Period.ONE_HOUR;
                UsedPeriods[1] = Period.FOUR_HOURS;
                break;
        }
        return UsedPeriods;
    }

    // Get stoploss via ATR
    private double getAtrStopLoss(Instrument instrument, Period period, String direction) throws JFException {

        double[] stopLossATR = null;

        IBar currentBar = this.history.getBar(instrument, period, OfferSide.BID, 0);

        if (direction.equals("Buy")) {
            stopLossATR = indicators.atr(instrument, period, OfferSide.BID, 14, com.dukascopy.api.Filter.ALL_FLATS, 1, currentBar.getTime(), 0);
        } else {
            stopLossATR = indicators.atr(instrument, period, OfferSide.ASK, 14, com.dukascopy.api.Filter.ALL_FLATS, 1, currentBar.getTime(), 0);
        }
        return stopLossATR[0];
    }

    private double[] getFibLevels(Instrument instrument, String direction) throws JFException {


        /* IBar currentBar = history.getBar(instrument, Period.TEN_SECS, OfferSide.BID, 0);
         double currentPrice = currentBar.getClose();*/
        IBar dailyBar = history.getBar(instrument, Period.DAILY_SUNDAY_IN_MONDAY, OfferSide.BID, 0);
        double dailyHi = dailyBar.getHigh();
        double dailyLow = dailyBar.getLow();

        double fib01, fib02, fib03, fib04;

        if (direction.equals("Buy")) {
            fib01 = roundFloor(dailyHi - (dailyHi - dailyLow) * 0.236, getInstrumentDecimalMultiplier(instrument));
            fib02 = roundFloor(dailyHi - (dailyHi - dailyLow) * 0.382, getInstrumentDecimalMultiplier(instrument));
            fib03 = roundFloor(dailyHi - (dailyHi - dailyLow) * 0.5, getInstrumentDecimalMultiplier(instrument));
            fib04 = roundFloor(dailyHi - (dailyHi - dailyLow) * 0.618, getInstrumentDecimalMultiplier(instrument));
        } else {
            fib01 = roundFloor(dailyLow - (dailyLow - dailyHi) * 0.236, getInstrumentDecimalMultiplier(instrument));
            fib02 = roundFloor(dailyLow - (dailyLow - dailyHi) * 0.382, getInstrumentDecimalMultiplier(instrument));
            fib03 = roundFloor(dailyLow - (dailyLow - dailyHi) * 0.5, getInstrumentDecimalMultiplier(instrument));
            fib04 = roundFloor(dailyLow - (dailyLow - dailyHi) * 0.618, getInstrumentDecimalMultiplier(instrument));
        }

        double[] fibLevels = {fib01, fib02, fib03, fib04};

        /*  console.getOut().println("-");
         console.getOut().println("-");
         console.getOut().println("instrument : " + instrument);
         console.getOut().println("dailyHi : " + dailyHi);
         console.getOut().println("dailyLow : " + dailyLow);

         console.getOut().println("fib01 : " + fibLevels[0]);
         console.getOut().println("fib02 : " + fibLevels[1]);
         console.getOut().println("fib03 : " + fibLevels[2]);*/
        return fibLevels;
    }

    private long convertStringToMillisec(long _timeMillisec, String _timeString) {

        // Decomposition of current Millisecond time
        GregorianCalendar currentCalMillisec = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        currentCalMillisec.setTimeInMillis(_timeMillisec);

        int currentYear = currentCalMillisec.get(Calendar.YEAR);
        int currentMonth = currentCalMillisec.get(Calendar.MONTH);
        int currentDay = currentCalMillisec.get(Calendar.DAY_OF_MONTH);
        int currentHour = currentCalMillisec.get(Calendar.HOUR_OF_DAY);
        int currentMinute = currentCalMillisec.get(Calendar.MINUTE);
        int currentSecond = currentCalMillisec.get(Calendar.SECOND);

        Calendar timeStringCal = null;

        try {
            // Creating a millisecond time from a string to facilitate the trading times comparisons
            timeStringCal = convertStringToCal(_timeString, "GMT", "HH:mm:ss");

        } catch (ParseException ex) {
            console.getOut().println(ex);
        }

        GregorianCalendar fullCal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        fullCal.set(currentYear, currentMonth, currentDay, timeStringCal.get(Calendar.HOUR_OF_DAY), timeStringCal.get(Calendar.MINUTE), timeStringCal.get(Calendar.SECOND));
        long fullCalMillisec = fullCal.getTimeInMillis();

        /*  console.getOut().println("05 - currentCalMillisec : " + currentCalMillisec.get(Calendar.YEAR) + "-"
         + currentCalMillisec.get(Calendar.MONTH) + "-"
         + currentCalMillisec.get(Calendar.DAY_OF_MONTH) + " "
         + currentCalMillisec.get(Calendar.HOUR_OF_DAY) + ":"
         + currentCalMillisec.get(Calendar.MINUTE) + ":"
         + currentCalMillisec.get(Calendar.SECOND));

         console.getOut().println("06 - fullCal : " + fullCal.get(Calendar.YEAR) + "-"
         + fullCal.get(Calendar.MONTH) + "-"
         + fullCal.get(Calendar.DAY_OF_MONTH) + " "
         + fullCal.get(Calendar.HOUR_OF_DAY) + ":"
         + fullCal.get(Calendar.MINUTE) + ":"
         + fullCal.get(Calendar.SECOND));*/
        return fullCalMillisec;

    }

    private double calculateStopLossPips(Instrument instrument, String orderDirection, double entryValue, double stopValue) {

        double stopValuePips;
        double stopValuePipsTmp;

        if (orderDirection.equals("Buy")) {
            stopValuePipsTmp = roundFloor(entryValue - stopValue, getInstrumentDecimalMultiplier(instrument));
        } else {
            stopValuePipsTmp = roundFloor(stopValue - entryValue, getInstrumentDecimalMultiplier(instrument));
        }
        if (entryValue < 10) {
            stopValuePips = roundFloor(stopValuePipsTmp * 10000, 10);
        } else {
            stopValuePips = roundFloor(stopValuePipsTmp * 100, 10);
        }
        return stopValuePips;
    }

    private double calculateMoneyManagement(Instrument instrument, String orderDirection, double riskValue, double entryValue, double stopValue) {

        double lotValue = 0;
        //double equityValue = account.getEquity();
        double equityValue = 100000;
        double stopValuePips = calculateStopLossPips(instrument, orderDirection, entryValue, stopValue);

        double transitionalPrice;
        Instrument transitionalInstrument;

        double entryAmount = entryValue * stdLotValue;
        double stopAmount = stopValue * stdLotValue;
        double riskInInitialCurrency;
        double riskMoneyStdLot = 0;

        if (orderDirection.equals("Buy")) {
            riskInInitialCurrency = entryAmount - stopAmount;
        } else {
            riskInInitialCurrency = stopAmount - entryAmount;
        }

        // printOut.print("calculate 00 riskInInitialCurrency: " + riskInInitialCurrency);
        if (instrument.getSecondaryCurrency().equals(account.getCurrency())) {
            riskMoneyStdLot = riskInInitialCurrency;
            // printOut.print("calculate 01 Profit: " + riskMoneyStdLot);

        } else if (instrument.getPrimaryCurrency().equals(account.getCurrency())) {
            try {
                riskMoneyStdLot = riskInInitialCurrency / history.getLastTick(instrument).getBid();
                // printOut.print("calculate 02 Profit: " + riskMoneyStdLot);

            } catch (JFException ex) {
                // printOut.print("Ex : " + ex);
            }
        } else if (instrument.getSecondaryCurrency().equals(USD)) {
            // Secondary instrument is USD, convert to account currency.
            try {
                transitionalInstrument = pairs.get(account.getCurrency());
                // printOut.print("calculate 03-01 transitionalInstrument: " + transitionalInstrument);
                transitionalPrice = history.getLastTick(transitionalInstrument).getBid();
                // printOut.print("calculate 03-02 transitionalPrice: " + transitionalPrice);
                riskMoneyStdLot = riskInInitialCurrency / transitionalPrice;
                // printOut.print("calculate 03-03 Profit: " + riskMoneyStdLot);

            } catch (JFException ex) {
                printOut.print("Ex : " + ex);
            }
        } else {
            // Secondary instrument is not USD, must convert to USD, then to Account currency
            transitionalInstrument = pairs.get(instrument.getSecondaryCurrency());
            Currency workInstrumentSecondaryCurrency = instrument.getSecondaryCurrency();
            // printOut.print("calculate 04 Secondary currency: " + workInstrumentSecondaryCurrency);
            // USD is primary currency in transitional instrument
            if (transitionalInstrument.getPrimaryCurrency().equals(USD)) {
                try {
                    // Sell Primary(USD) currency
                    // printOut.print("calculate 03-00 pairs.get(workInstrumentSecondaryCurrency): " + pairs.get(workInstrumentSecondaryCurrency));
                    transitionalPrice = history.getLastTick(pairs.get(workInstrumentSecondaryCurrency)).getBid();
                    // printOut.print("calculate 05-01 transitionalPrice: " + transitionalPrice);
                    if (account.getCurrency().equals(USD)) {
                        riskMoneyStdLot = riskInInitialCurrency / transitionalPrice;
                    } else {
                        // printOut.print("calculate 05-02 pairs.get(account.getCurrency()) : " + pairs.get(account.getCurrency()));
                        riskMoneyStdLot = (riskInInitialCurrency / transitionalPrice) / (history.getLastTick(pairs.get(account.getCurrency())).getBid());
                    }
                    // printOut.print("calculate 05-03 USD is primary. Profit: " + riskMoneyStdLot);
                } catch (JFException ex) {
                    printOut.print("Ex : " + ex);
                }
                // USD is secondary currency in transitional instrument
            } else {
                try {
                    transitionalPrice = history.getLastTick(pairs.get(workInstrumentSecondaryCurrency)).getBid();
                    // Not able to test... so riskMoneyStdLot = 0 in this case to see when it comes.
                    riskMoneyStdLot = riskInInitialCurrency * transitionalPrice;
                    //riskMoneyStdLot = 0;
                    // printOut.print("calculate 06 USD is secondary. Profit: " + riskMoneyStdLot);
                } catch (JFException ex) {
                    printOut.print("Ex : " + ex);
                }
            }
        }
        //
        // printOut.print("calculate 07 BUY NB LOTS : "+roundFloor((equityValue * riskValue / 100) / riskMoneyStdLot * 0.1, 1000));
        // printOut.print("calculate 07 BUY NB LOTS : " + roundFloor((100000 * 0.1 / 100) / riskMoneyStdLot * 0.1, 1000));
        lotValue = roundFloor((equityValue * riskValue / 100) / riskMoneyStdLot * 0.1, 1000);
        double pipValue = roundFloor(riskMoneyStdLot / stopValuePips, 100);

        //myPanel.pipLabelValue.setText("Pip value : " + pipValue);
        //myPanel.lotLabel.setText("Lot size (" + roundFloor(pipValue * lotValue, 100) + ") :");
        return lotValue;
    }

    private Calendar convertStringToCal(String _time, String _timeZone, String _format) throws ParseException {

        DateFormat formatter = new SimpleDateFormat(_format);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date myDate = (Date) formatter.parse(_time);

        GregorianCalendar myCal = new GregorianCalendar(TimeZone.getTimeZone(_timeZone));
        myCal.setTime(myDate);

        //  console.getOut().println("03 - myDate : " + formatter.format(myDate));
        //  console.getOut().println("04 - convertStringToDate date : " + myCal.get(Calendar.HOUR_OF_DAY) + ":" + myCal.get(Calendar.MINUTE) + " GMT");
        return myCal;
    }

    private long getDSToffset(long _timeMillisec, int timeZone) {

        long returnOffest = 0;

        if (timeZone == 0) {
            TimeZone london = TimeZone.getTimeZone("Europe/London");
            returnOffest = london.getOffset(_timeMillisec);
        } else if (timeZone == 1) {
            TimeZone london = TimeZone.getTimeZone("America/New_York");
            returnOffest = 18000000 + london.getOffset(_timeMillisec);
        }
        // console.getOut().println("ReturnOffest " + timeZone + " : " + returnOffest);

        return returnOffest;
    }

    // Calculate the number of decimals for the Instrument
    private int getInstrumentDecimalMultiplier(Instrument _instrument) {
        return (int) Math.pow(10, _instrument.getPipScale()) * 10;
    }

    private double roundFloor(double val, int dec) {
        //
        // printOut.print("**** roundFloor AVANT : "+val);
        double roundValue = (Math.floor(val * dec + 0.5)) / dec;
        //printOut.print("**** roundFloor APRES : "+roundValue);
        //
        return roundValue;
    }

    private void commentOnChart(Instrument instrument, String chartString) {

        Font ourFont;
        int fontSize = 13;
        String fontName = "ARIAL";
        ourFont = Font.decode(fontName + "-BOLD-" + fontSize);

        Set<IChart> setOfCharts = context.getCharts(instrument);
        for (IChart currentChart : setOfCharts) {

            if (currentChart == null) {
                return;
            }

            currentChart.comment(chartString);
            currentChart.setCommentFont(ourFont);
            currentChart.repaint();

        }
    }

    // Finding the position of the last XML orders to continue the backtest before last opened order
    private String[] getXMLLastOrdersPos(String folderName, Long currentTime, int lookback) {

        // Searching for files in the directory
        File folder = new File(folderName);
        File[] listOfFiles = folder.listFiles();
        String dateOfYesterday = dateFormatter.format(currentTime/* - (1000 * 60 * 60 * 24)*/);
        List<ParamList> paramList = new ArrayList<ParamList>();
        int bestlistLength = listOfFiles.length;
        String[] params = new String[bestlistLength];

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                // printOut.print("File " + listOfFiles[i].getName());

                // create an XML document fo each file
                try {
                    // création d'une fabrique de documents
                    DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();

                    // création d'un constructeur de documents
                    DocumentBuilder constructeur = fabrique.newDocumentBuilder();

                    // lecture du contenu d'un fichier XML avec DOM
                    File xml = new File(folderName + listOfFiles[i].getName());
                    org.w3c.dom.Document document = constructeur.parse(xml);
                    org.w3c.dom.Element XMLrootNode = document.getDocumentElement();

                    //  console.getOut().println("//////////////////////////////////  XMLrootNode : " + XMLrootNode.getNodeName());
                    NodeList orderOpenNodes = XMLrootNode.getElementsByTagName("orderOpen");
                    NodeList orderCloseNodes = XMLrootNode.getElementsByTagName("orderClose");

                    List<Long> openedOrders = new ArrayList<Long>();

                    int nbOrderOpenedNodes = orderOpenNodes.getLength();
                    int nbOrderClosedNodes = orderCloseNodes.getLength();

                    // we search which opened order has been closed
                    for (int j = 0; j < nbOrderOpenedNodes; j++) {

                        org.w3c.dom.Element orderOpenElt = (org.w3c.dom.Element) orderOpenNodes.item(j);
                        String orderOpenLabel = orderOpenElt.getAttribute("orderLabel");

                        // Searching if there is an orderClose node with the same label
                        int cpt = 0;
                        boolean found = false;

                        while (!found && cpt < nbOrderClosedNodes) {

                            org.w3c.dom.Element orderCloseElt = (org.w3c.dom.Element) orderCloseNodes.item(cpt);
                            String orderCloseLabel = orderCloseElt.getAttribute("orderLabel");

                            if (orderOpenLabel.equals(orderCloseLabel)) {
                                found = true;
                            } else {
                                found = false;
                            }
                            cpt++;
                        }

                        // if after the search we didn't find an order close, it means that the order is already opened
                        if (!found) {
                            openedOrders.add(Long.parseLong(orderOpenElt.getAttribute("orderFillTimeMillisec")));
                        }
                    }

                } catch (ParserConfigurationException pce) {
                    printOut.print("Erreur de configuration du parseur DOM");
                    printOut.print("lors de l'appel à fabrique.newDocumentBuilder();");
                } catch (SAXException se) {
                    printOut.print("Erreur lors du parsing du document");
                    printOut.print("lors de l'appel à construteur.parse(xml)");
                } catch (IOException ioe) {
                    printOut.print("Erreur d'entrée/sortie");
                    printOut.print("lors de l'appel à construteur.parse(xml)");
                }

            }
        }

        return params;
    }

    // Reading into the XML Files for the best robot params
    private String[] getRobotParams(String folderName, Long currentTime, int lookback) {

        // Searching for files in the directory
        File folder = new File(folderName);
        File[] listOfFiles = folder.listFiles();
        String dateOfYesterday = dateFormatter.format(currentTime/* - (1000 * 60 * 60 * 24)*/);
        List<ParamList> paramList = new ArrayList<ParamList>();
        int bestlistLength = listOfFiles.length;
        String[] params = new String[bestlistLength];

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                printOut.print("File " + listOfFiles[i].getName());

                // create an XML document fo each file
                try {
                    // création d'une fabrique de documents
                    DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();

                    // création d'un constructeur de documents
                    DocumentBuilder constructeur = fabrique.newDocumentBuilder();

                    // lecture du contenu d'un fichier XML avec DOM
                    File xml = new File(folderName + listOfFiles[i].getName());
                    org.w3c.dom.Document document = constructeur.parse(xml);
                    org.w3c.dom.Element XMLrootNode = document.getDocumentElement();

                    //  console.getOut().println("//////////////////////////////////  XMLrootNode : " + XMLrootNode.getNodeName());
                    NodeList dayNodes = XMLrootNode.getElementsByTagName("day");

                    int nbNodes = dayNodes.getLength();

                    // Searching for yesterdays Node
                    int cpt = nbNodes - 1;
                    boolean found = false;

                    while (!found && cpt != 0) {
                        org.w3c.dom.Element elt = (org.w3c.dom.Element) dayNodes.item(cpt);
                        if (elt.getAttribute("dateString").equals(dateOfYesterday)) {
                            nbNodes = cpt;
                            found = true;
                        } else {
                            cpt--;
                        }
                    }

                    org.w3c.dom.Element dayNodeElement0 = (org.w3c.dom.Element) dayNodes.item(nbNodes);
                    //console.getOut().println("NOEUD RETROUVE : " + XMLrootNode.getNodeName() + " " + dayNodeElement0.getAttribute("dateString") + " " + dayNodeElement0.getAttribute("dateMillisec"));

                    double totalProfit = 0;
                    double squareTotalProfit = 0;

                    for (int j = nbNodes - lookback; j < nbNodes; j++) {

                        org.w3c.dom.Element dayNodeElement = (org.w3c.dom.Element) dayNodes.item(j);

                        //console.getOut().println("NODE" + j + " : " + dayNodeElement.getAttribute("dateString"));
                        // console.getOut().println("hasChild" + j + " : " + dayNodeElement.hasChildNodes());
                        if (dayNodeElement.hasChildNodes()) {
                            NodeList orderNodes = dayNodeElement.getElementsByTagName("orderClose");

                            for (int y = 0; y < orderNodes.getLength(); y++) {
                                org.w3c.dom.Element orderNode = (org.w3c.dom.Element) orderNodes.item(y);
                                double orderProfit = Double.parseDouble(orderNode.getAttribute("orderProfitUSD"));
                                double squareOrderProfit = orderProfit * ((Math.pow(j, 3) / 1000));
                                /*  if(orderProfit>0){
                                 orderProfit = 1;
                                 } else if(orderProfit<0){
                                 orderProfit = -1;
                                 } else {
                                 orderProfit = 0;
                                 }*/

                                totalProfit = roundFloor(totalProfit + orderProfit, 100);
                                squareTotalProfit = roundFloor(squareTotalProfit + squareOrderProfit, 100);
                                // console.getOut().println("Profit" + j + " : " + totalProfit + " squareTotalProfit : " + squareTotalProfit + " square : " + orderProfit + "*" + Math.pow(j, 3) / 1000 + " = " + squareOrderProfit);

                            }
                        }
                    }
                    paramList.add(new ParamList(XMLrootNode.getNodeName(), roundFloor(totalProfit, 100)));

                } catch (ParserConfigurationException pce) {
                    printOut.print("Erreur de configuration du parseur DOM");
                    printOut.print("lors de l'appel à fabrique.newDocumentBuilder();");
                } catch (SAXException se) {
                    printOut.print("Erreur lors du parsing du document");
                    printOut.print("lors de l'appel à construteur.parse(xml)");
                } catch (IOException ioe) {
                    printOut.print("Erreur d'entrée/sortie");
                    printOut.print("lors de l'appel à construteur.parse(xml)");
                }

            }
        }

        Collections.sort(paramList);
        Collections.reverse(paramList);

        int paramInProfitCount = 0;
        double paramInProfitAmountPct = 0;

        // making
        for (int i = 0; i < bestlistLength; i++) {
            double currentProfit = paramList.get(i).getProfit();
            console.getOut().println("paramList.get(i).getProfit(); : " + currentProfit);
            if (currentProfit > 0) {
                paramInProfitCount++;
            }
        }

        paramInProfitAmountPct = roundFloor((double) paramInProfitCount / bestlistLength, 100);

        // making
        for (int i = 0; i < bestlistLength; i++) {
            double currentProfit = paramList.get(i).getProfit();

            params[i] = paramList.get(i).getParamString() + "_profit_" + currentProfit + "_paramInProfitAmountPct_" + paramInProfitAmountPct;

            console.getOut().println("Best profit list : " + params[i]);
        }

        // bestTradingTime = timeList.get(0).getTime();
        return params;
    }

    // Class who writes the trades in a file
    public class FileWrite {

        public void writeToFile(String _fileName, String message) {

            //  console.getOut().print(message);
            for (int i = 0; i < destFolders.length; i++) {

                String destFolder = destFolders[i];

                try {
                    FileWriter fstream = new FileWriter(destFolder + _fileName, true);
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.append(message);
                    out.close();
                } catch (Exception e) {
                    console.getOut().print("File access error: " + e.getMessage());
                }
            }
        }
    }

    private void XMLWrite(String fileName, org.jdom.Document _XMLDocument) {

        for (int i = 0; i < destFolders.length; i++) {

            String destFolder = destFolders[i];

            try {
                //On utilise ici un affichage classique avec getPrettyFormat()
                XMLOutputter xmlOut = new XMLOutputter(Format.getPrettyFormat());
                //Remarquez qu'il suffit simplement de créer une instance de FileOutputStream
                //avec en argument le nom du fichier pour effectuer la sérialisation.
                xmlOut.output(_XMLDocument, new FileOutputStream(destFolder + fileName));
            } catch (java.io.IOException e) {
                printOut.print("******** PB XMLWrite : " + e);
            }
        }
    }

    // Class to be able to sort a list of objects
    class ParamList implements java.lang.Comparable {

        String paramString = "";
        double profit = 0;

        public ParamList(String _paramString, double _profit) {
            paramString = _paramString;
            profit = _profit;
        }

        public double getProfit() {
            return profit;
        }

        public String getParamString() {
            return paramString;
        }

        public String getString() {
            return paramString + " # " + profit;
        }

        public int compareTo(Object other) {
            double number1 = ((ParamList) other).getProfit();
            double number2 = this.getProfit();
            if (number1 > number2) {
                return -1;
            } else if (number1 == number2) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    private int getDayOfTheWeek(long timeMillisec) {
        int result;
        cal.setTimeInMillis(timeMillisec);
        result = cal.get(Calendar.DAY_OF_WEEK);
        return result;
    }

    class PrintOut {

        private void print(Object message) {
            if (enablePrint == 1) {
                console.getOut().println(message);
            }
        }
    }
}
