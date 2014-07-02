package NicoSources;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.Commissions;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.Future;

/**
 * This small program demonstrates how to initialize Dukascopy tester and start
 * a strategy
 */
public class TesterMainNoGui {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    //url of the DEMO jnlp
    private static String jnlpUrl = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
    //user name
    private static String userName = "DEMO3zYTLu";
    //password
    private static String password = "zYTLu";

    private int maxStartedStrategies = 1;
    private int nbStartedStrategies;
    private int optimizationListIndex = 0;
    private List<String> optimizationList;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat chronoFormat;
    private Calendar cal;
    private long startTimeMillisec = 0;
    private long endTimeMillisec = 0;
    
    R_CurrencyIndexRobotFX20140102_mvn strategy1;

    public TesterMainNoGui(List<String> _optimizationList) throws Exception {

        optimizationList = _optimizationList;

        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.US);
        chronoFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        cal = Calendar.getInstance();

        //get the instance of the IClient interface
        final ITesterClient client = TesterFactory.getDefaultInstance();
        //set the listener that will receive system events
        client.setSystemListener(new ISystemListener() {

            @Override
            public void onStart(long processId) {
                cal = Calendar.getInstance();
                startTimeMillisec = cal.getTimeInMillis();
                LOGGER.info("Strategy started: " + processId + " - Date : " + dateFormat.format(startTimeMillisec));
            }

            @Override
            public void onStop(long processId) {
                
                LOGGER.info("Strategy stopped strategy1 before :" + strategy1);
                strategy1 = null;
                LOGGER.info("Strategy stopped strategy1 after :" + strategy1);

                
                cal = Calendar.getInstance();
                endTimeMillisec = cal.getTimeInMillis();
                LOGGER.info("Strategy stopped: " + processId + " - Date : " + dateFormat.format(endTimeMillisec));
                LOGGER.info("ProcessLength: " + chronoFormat.format(endTimeMillisec - startTimeMillisec));
                /*
                 File reportFile = new File("C:\\report.html");
                 try {
                 client.createReport(processId, reportFile);
                 } catch (Exception e) {
                 LOGGER.error(e.getMessage(), e);
                 }*/

                nbStartedStrategies = client.getStartedStrategies().size();

                if (nbStartedStrategies < maxStartedStrategies && optimizationListIndex < optimizationList.size()) {
                    startLoopStrategies(optimizationListIndex, client);
                }

                /*  if (client.getStartedStrategies().size() == 0) {
                 System.exit(0);
                 }*/
            }

            @Override
            public void onConnect() {
                LOGGER.info("Connected");
            }

            @Override
            public void onDisconnect() {
                //tester doesn't disconnect
            }
        });

        // setting the cacheFolder
        File CacheFolder = new File("E:/JForexCache/.cache");
        client.setCacheDirectory(CacheFolder);

        LOGGER.info("Connecting...");
        //connect to the server using jnlp, user name and password
        //connection is needed for data downloading
        client.connect(jnlpUrl, userName, password);

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }

        //set instruments that will be used in testing
        Set<Instrument> instruments = new HashSet<Instrument>();
        LOGGER.info("Subscribing instruments...");
        instruments.add(Instrument.EURUSD);
        instruments.add(Instrument.AUDCAD);
        instruments.add(Instrument.AUDJPY);
        instruments.add(Instrument.AUDNZD);
        instruments.add(Instrument.AUDUSD);
        instruments.add(Instrument.CADJPY);
        instruments.add(Instrument.EURAUD);
        instruments.add(Instrument.EURCAD);
        instruments.add(Instrument.EURGBP);
        instruments.add(Instrument.EURJPY);
        instruments.add(Instrument.EURNZD);
        instruments.add(Instrument.GBPAUD);
        instruments.add(Instrument.GBPCAD);
        instruments.add(Instrument.GBPJPY);
        instruments.add(Instrument.GBPNZD);
        instruments.add(Instrument.GBPUSD);
        instruments.add(Instrument.NZDCAD);
        instruments.add(Instrument.NZDJPY);
        instruments.add(Instrument.NZDUSD);
        instruments.add(Instrument.USDCAD);
        instruments.add(Instrument.USDJPY);
        client.setSubscribedInstruments(instruments);

//setting initial deposit
        client.setInitialDeposit(Instrument.EURUSD.getSecondaryCurrency(), 100000);
        //
        // Setting commission amounts
        Commissions commissions = client.getCommissions();

        SortedMap<Double, Double> depositLimits = commissions.getDepositLimits();
        SortedMap<Double, Double> equityLimits = commissions.getEquityLimits();
        SortedMap<Double, Double> turnoverLimits = commissions.getTurnoverLimits();

        // more elegant way
        for (Map.Entry<Double, Double> entry : depositLimits.entrySet()) {
            System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
            if (entry.getKey() == 5000.0) {
                entry.setValue(48.0);
            } else {
                entry.setValue(38.0);
            }
        }
        for (Map.Entry<Double, Double> entry : equityLimits.entrySet()) {
            System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
            entry.setValue(38.0);
        }

        for (Map.Entry<Double, Double> entry : turnoverLimits.entrySet()) {
            System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
            entry.setValue(38.0);
        }

        // Outputinng commission values
        for (Map.Entry<Double, Double> entry : depositLimits.entrySet()) {
            System.out.println("depositLimits Key : " + entry.getKey() + " Value : " + entry.getValue());
        }
        for (Map.Entry<Double, Double> entry : equityLimits.entrySet()) {
            System.out.println("equityLimits Key : " + entry.getKey() + " Value : " + entry.getValue());
        }
        for (Map.Entry<Double, Double> entry : turnoverLimits.entrySet()) {
            System.out.println("turnoverLimits Key : " + entry.getKey() + " Value : " + entry.getValue());
        }

        //
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date dateFrom = dateFormat.parse("2007/12/03 23:59:00");
        Date dateTo = dateFormat.parse("2014/02/14 00:00:00");

        //client.setDataInterval(ITesterClient.DataLoadingMethod.ALL_TICKS, dateFrom.getTime(), dateTo.getTime());
        client.setDataInterval(Period.ONE_MIN, OfferSide.BID, ITesterClient.InterpolationMethod.FOUR_TICKS, dateFrom.getTime(), dateTo.getTime());
        
        //load data
        LOGGER.info("Downloading data");
        Future<?> future = client.downloadData(null);
        //wait for downloading to complete
        future.get();

        //workaround for LoadNumberOfCandlesAction for JForex-API versions > 2.6.64
        Thread.sleep(5000);

        for (int j = 0; j < maxStartedStrategies; j++) {
            startLoopStrategies(j, client);
        }

    }

    private void startLoopStrategies(int paramIndex, ITesterClient _client) {
 String optimisationParameters = optimizationList.get(paramIndex);

        strategy1 = new R_CurrencyIndexRobotFX20140102_mvn();
        optimizationListIndex++;

        if (getExistingXMLFile(optimisationParameters, strategy1.destFolders[0]) == 0) {

            String[] listPlit = optimisationParameters.split("_");
            strategy1.usedPeriodsConfig = Integer.parseInt(listPlit[0]);
            strategy1.stopLossMultiplier = Double.parseDouble(listPlit[1]);
            strategy1.usedSLConfig = Integer.parseInt(listPlit[2]);
            strategy1.atrSLDEVMultiplier = Integer.parseInt(listPlit[3]);

            //start the strategy
            LOGGER.info("Starting strategy");

     /*       LoadingProgressListener loadingProgressListener = new LoadingProgressListener() {
                @Override
                public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
                    // LOGGER.info(information + " - " + nbStartedStrategies);
                }

                @Override
                public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
                }

                @Override
                public boolean stopJob() {
                    return false;
                }
            };*/
           _client.startStrategy(strategy1);
           //_client.startStrategy(strategy1, loadingProgressListener);
            
            
        } else {
            nbStartedStrategies = _client.getStartedStrategies().size();
            if (nbStartedStrategies < maxStartedStrategies && optimizationListIndex < optimizationList.size()) {
                startLoopStrategies(optimizationListIndex, _client);
            } else {
                 System.exit(0);
            }
        }
        
        //now it's running
    }
     //
    // Test if file allready exists
    private int getExistingXMLFile(String _optimisationParameters, String readFolder) {

        int ret = 0;

        // Searching for files in the directory
        File folder = new File(readFolder);
        File[] listOfFiles = folder.listFiles();

        // Searching if there is already a file in the folder named like the parameters
        int cpt = 0;
        boolean found = false;

        while (!found && cpt < listOfFiles.length) {

            String fileName = listOfFiles[cpt].getName();
            String currentParameters = "currencyIndex_" + _optimisationParameters + ".xml";

            LOGGER.info(fileName + " - " + currentParameters);

            if (fileName.equals(currentParameters)) {
                found = true;
                ret = 1;
            } else {
                found = false;
            }
            cpt++;
        }
        return ret;
    }
}
