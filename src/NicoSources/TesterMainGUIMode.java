package NicoSources;

import singlejartest.*;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.log4j.*;

import com.dukascopy.api.IChart;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.Commissions;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.ITesterClient.DataLoadingMethod;
import com.dukascopy.api.system.TesterFactory;
import com.dukascopy.api.system.tester.ITesterExecution;
import com.dukascopy.api.system.tester.ITesterExecutionControl;
import com.dukascopy.api.system.tester.ITesterGui;
import com.dukascopy.api.system.tester.ITesterUserInterface;
import java.awt.AWTException;
import java.awt.Robot;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 * This small program demonstrates how to initialize Dukascopy tester and start
 * a strategy in GUI mode
 */
@SuppressWarnings("serial")
public class TesterMainGUIMode extends JFrame implements ITesterUserInterface, ITesterExecution {

    private String strDateFrom = "2007/05/01 23:59:00";
   // private String strDateTo = "2007/06/01 00:00:00";
    private String strDateTo = "2013/01/02 00:00:00";
    private String destWriteFolder = "";
    private String cacheFolderStr = "";

    //  private static final Logger LOGGER = LoggerFactory.getLogger(TesterMainGUIMode.class);
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(TesterMainGUIMode.class);
    private boolean DEBUG = false;

    private final int frameWidth = 1000;
    private final int frameHeight = 600;
    private final int controlPanelHeight = 40;

    private JPanel currentChartPanel = null;
    private ITesterExecutionControl executionControl = null;
    private List<ITesterExecutionControl> executionControlList = new ArrayList<ITesterExecutionControl>();

    private JPanel controlPanel = null;
    private JPanel tablePanel = null;
    private JTable orderTable = null;
    private JButton startStrategyButton = null;
    private JButton pauseButton = null;
    private JButton continueButton = null;
    private JButton cancelButton = null;
    private boolean cancelApp = false;

    private int maxStartedStrategies = 1;
    private int nbStartedStrategies;
    private int optimizationListIndex = 0;
    List<String> optimizationList = new ArrayList<String>();
    DateFormat dateFormat;
    DateFormat chronoFormat;
    Calendar cal;
    private long startTimeMillisec = 0;
    private long endTimeMillisec = 0;

    //url of the DEMO jnlp
    private static String jnlpUrl = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
    //user name
    private static String userName = "DEMO3zYTLu";
    //password
    private static String password = "zYTLu";

    R_CurrencyIndexRobotFX20140102_mvn strategy1;
    LoadingProgressListener loadingProgressListener;

    public TesterMainGUIMode(List<String> _optimizationList,String _fromDate, String _toDate, String _destWriteFolder, String _cacheFolderStr) {

       // LOGGER.setLevel(org.apache.log4j.Level.ALL);
     //   org.apache.log4j.Logger logger01 = org.apache.log4j.Logger.getLogger("com.dukascopy.charts.data.datacache.customticks.AbstractLoadCustomTicksFromCandlesAction");
        //   logger01.setLevel(org.apache.log4j.Level.OFF);
        optimizationList = _optimizationList;
        
        // if we have arguments we use them
        if(_fromDate !=""){
            strDateFrom = _fromDate;
        }
        if(_toDate !=""){
            strDateTo = _toDate;
        }
        if(_destWriteFolder !=""){
            destWriteFolder = _destWriteFolder;
        }
        if(_cacheFolderStr !=""){
            cacheFolderStr = _cacheFolderStr;
        }

        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.US);
        chronoFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        cal = Calendar.getInstance();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        showChartFrame();
    }

    @Override
    public void setChartPanels(Map<IChart, ITesterGui> chartPanels) {
        /* if (chartPanels != null && chartPanels.size() > 0) {

         IChart chart = chartPanels.keySet().iterator().next();
         Instrument instrument = chart.getInstrument();
         setTitle(instrument.toString() + " " + chart.getSelectedOfferSide() + " " + chart.getSelectedPeriod());

         JPanel chartPanel = chartPanels.get(chart).getChartPanel();
         addChartPanel(chartPanel);
         }*/
    }

    @Override
    public void setExecutionControl(ITesterExecutionControl executionControl) {
        LOGGER.info("setExecutionControl");
        executionControlList.add(executionControl);
    }

    public void startStrategy() throws Exception {
        //get the instance of the IClient interface
        final ITesterClient client = TesterFactory.getDefaultInstance();

        //set the listener that will receive system events
        //
        client.setSystemListener(new ISystemListener() {
            @Override
            public void onStart(long processId) {

                cal = Calendar.getInstance();
                startTimeMillisec = cal.getTimeInMillis();
                LOGGER.info("Strategy started: " + processId + " - Date : " + dateFormat.format(startTimeMillisec));
                updateButtons();
            }

            @Override
            public void onStop(long processId) {

                //  RecursiveDelete.main(null);
                // LOGGER.info("Strategy stopped strategy1 before :" + strategy1);
                // LOGGER.info("Strategy stopped loadingProgressListener before :" + loadingProgressListener);
                loadingProgressListener = null;
                strategy1 = null;

               // LOGGER.info("Strategy stopped strategy1 after :" + strategy1);
                // LOGGER.info("Strategy stopped loadingProgressListener after :" + loadingProgressListener);
                cal = Calendar.getInstance();
                endTimeMillisec = cal.getTimeInMillis();
               // LOGGER.info("Strategy stopped: " + processId + " - Date : " + dateFormat.format(endTimeMillisec));
                // LOGGER.info("ProcessLength: " + chronoFormat.format(endTimeMillisec - startTimeMillisec));

                if (!cancelApp) {
                    nbStartedStrategies = client.getStartedStrategies().size();
                    if (nbStartedStrategies < maxStartedStrategies && optimizationListIndex < optimizationList.size()) {
                        startLoopStrategies(optimizationListIndex, client);
                    }
                } else {
                    resetButtons();
                    System.exit(0);
                    /* File reportFile = new File("C:\\report.html");
                     try {
                     client.createReport(processId, reportFile);
                     } catch (Exception e) {
                     LOGGER.error(e.getMessage(), e);
                     }*/
                    /*   if (client.getStartedStrategies().size() == 0) {
                     //Do nothing
                     }*/
                }
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
        File CacheFolder = new File(cacheFolderStr);
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
        final Set<Instrument> instruments = new HashSet<Instrument>();
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

        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);

        //setting initial deposit
        client.setInitialDeposit(Instrument.EURUSD.getSecondaryCurrency(), 100000);

        Commissions commissions = client.getCommissions();

        SortedMap<Double, Double> depositLimits = commissions.getDepositLimits();
        SortedMap<Double, Double> equityLimits = commissions.getEquityLimits();
        SortedMap<Double, Double> turnoverLimits = commissions.getTurnoverLimits();

        // more elegant way
        for (Map.Entry<Double, Double> entry : depositLimits.entrySet()) {
            // System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
            if (entry.getKey() == 5000.0) {
                entry.setValue(48.0);
            } else {
                entry.setValue(38.0);
            }
        }
        for (Map.Entry<Double, Double> entry : equityLimits.entrySet()) {
            // System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
            entry.setValue(38.0);
        }

        for (Map.Entry<Double, Double> entry : turnoverLimits.entrySet()) {
            // System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
            entry.setValue(38.0);
        }

        // Outputinng commission values
        for (Map.Entry<Double, Double> entry : depositLimits.entrySet()) {
            // System.out.println("depositLimits Key : " + entry.getKey() + " Value : " + entry.getValue());
        }
        for (Map.Entry<Double, Double> entry : equityLimits.entrySet()) {
            // System.out.println("equityLimits Key : " + entry.getKey() + " Value : " + entry.getValue());
        }
        for (Map.Entry<Double, Double> entry : turnoverLimits.entrySet()) {
            // System.out.println("turnoverLimits Key : " + entry.getKey() + " Value : " + entry.getValue());
        }
        //
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date dateFrom = dateFormat.parse(strDateFrom);
        Date dateTo = dateFormat.parse(strDateTo);
        // client.setDataInterval(Period.FIVE_MINS, OfferSide.BID, ITesterClient.InterpolationMethod.FOUR_TICKS, dateFrom.getTime(), dateTo.getTime());
        client.setDataInterval(Period.ONE_MIN, OfferSide.BID, ITesterClient.InterpolationMethod.FOUR_TICKS, dateFrom.getTime(), dateTo.getTime());
       // DataLoadingMethod myDataloadingMethod = DataLoadingMethod.TICKS_WITH_TIME_INTERVAL;
        // myDataloadingMethod.setTimeIntervalBetweenTicks(60000);
        //client.setDataInterval(myDataloadingMethod, dateFrom.getTime(), dateTo.getTime());

        //client.setDataInterval(ITesterClient.DataLoadingMethod.ALL_TICKS, dateFrom.getTime(), dateTo.getTime());
        //load data
        LOGGER.info("Downloading data");
        Future<?> future = client.downloadData(null);
        //wait for downloading to complete
        future.get();
        //start the strategy
        //  LOGGER.info("Starting strategy");

        //workaround for LoadNumberOfCandlesAction for JForex-API versions > 2.6.64
        Thread.sleep(5000);

        for (int j = 0; j < maxStartedStrategies; j++) {
            startLoopStrategies(j, client);
        }
    }

    /**
     * Center a frame on the screen
     */
    private void centerFrame() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int screenHeight = screenSize.height;
        int screenWidth = screenSize.width;
        setSize(screenWidth / 2, screenHeight / 2);
        setLocation(screenWidth / 4, screenHeight / 4);
    }

    /**
     * Add chart panel to the frame
     *
     * @param panel
     */
    private void addChartPanel(JPanel chartPanel) {
        removecurrentChartPanel();

        this.currentChartPanel = chartPanel;
        chartPanel.setPreferredSize(new Dimension(frameWidth, frameHeight - controlPanelHeight));
        chartPanel.setMinimumSize(new Dimension(frameWidth, 200));
        chartPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        getContentPane().add(chartPanel);
        this.validate();
        chartPanel.repaint();
    }

    /**
     * Add buttons to start/pause/continue/cancel actions
     */
    private void addControlPanel() {

        controlPanel = new JPanel();
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
        controlPanel.setLayout(flowLayout);
        controlPanel.setPreferredSize(new Dimension(frameWidth, controlPanelHeight));
        controlPanel.setMinimumSize(new Dimension(frameWidth, controlPanelHeight));
        controlPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, controlPanelHeight));

        startStrategyButton = new JButton("Start strategy");
        startStrategyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startStrategyButton.setEnabled(false);
                Runnable r = new Runnable() {
                    public void run() {
                        try {
                            startStrategy();
                        } catch (Exception e2) {
                            LOGGER.error(e2.getMessage(), e2);
                            e2.printStackTrace();
                            startStrategyButton.setEnabled(true);
                        }
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });

        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!executionControlList.isEmpty()) {
                    for (ITesterExecutionControl executionControlObj : executionControlList) {
                        executionControlObj.pauseExecution();
                    }
                    updateButtons();
                }
            }
        });

        continueButton = new JButton("Continue");
        continueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!executionControlList.isEmpty()) {
                    for (ITesterExecutionControl executionControlObj : executionControlList) {
                        executionControlObj.continueExecution();
                    }
                    updateButtons();
                }
            }
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!executionControlList.isEmpty()) {
                    cancelApp = true;
                    for (ITesterExecutionControl executionControlObj : executionControlList) {
                        executionControlObj.cancelExecution();
                    }
                    updateButtons();
                }
            }
        });

        controlPanel.add(startStrategyButton);
        controlPanel.add(pauseButton);
        controlPanel.add(continueButton);
        controlPanel.add(cancelButton);
        getContentPane().add(controlPanel);

        pauseButton.setEnabled(false);
        continueButton.setEnabled(false);
        cancelButton.setEnabled(false);
    }

    private void updateButtons() {

        if (!executionControlList.isEmpty()) {
            startStrategyButton.setEnabled(executionControlList.get(0).isExecutionCanceled());
            pauseButton.setEnabled(!executionControlList.get(0).isExecutionPaused() && !executionControlList.get(0).isExecutionCanceled());
            cancelButton.setEnabled(!executionControlList.get(0).isExecutionCanceled());
            continueButton.setEnabled(executionControlList.get(0).isExecutionPaused());
        }
    }

    private void resetButtons() {
        startStrategyButton.setEnabled(false);
        pauseButton.setEnabled(false);
        continueButton.setEnabled(false);
        cancelButton.setEnabled(false);
    }

    private void removecurrentChartPanel() {
        if (this.currentChartPanel != null) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        TesterMainGUIMode.this.getContentPane().remove(TesterMainGUIMode.this.currentChartPanel);
                        TesterMainGUIMode.this.getContentPane().repaint();
                    }
                });
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public void showChartFrame() {
        setSize(frameWidth, frameHeight);
        centerFrame();
        addControlPanel();
        setVisible(true);
    }

    private void startLoopStrategies(int paramIndex, final ITesterClient _client) {

        String optimisationParameters = optimizationList.get(paramIndex);

        strategy1 = new R_CurrencyIndexRobotFX20140102_mvn();
        optimizationListIndex++;
        
        // If we don't pass as argument a destination folder, use the one from the strategy
        // else overide it
        if(destWriteFolder == ""){
            destWriteFolder = strategy1.destFolders[0];
        } else {
            strategy1.destFolders[0] = destWriteFolder;
        }

        if (getExistingXMLFile(optimisationParameters, destWriteFolder) == 0) {

            String[] listPlit = optimisationParameters.split("_");
            strategy1.usedPeriodsConfig = Integer.parseInt(listPlit[0]);
            strategy1.stopLossMultiplier = Double.parseDouble(listPlit[1]);
            strategy1.usedSLConfig = Integer.parseInt(listPlit[2]);
            strategy1.atrSLDEVMultiplier = Integer.parseInt(listPlit[3]);

            //start the strategy
            LOGGER.info("Starting strategy");

            loadingProgressListener = new LoadingProgressListener() {
                @Override
                public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
                    LOGGER.info("dataLoaded startTime : " + dateFormat.format(startTime) + " - endTime : " + dateFormat.format(endTime) + " - currentTime : " + dateFormat.format(currentTime) + " - information : " + information);
                    // Stop manually the strategy 1 day before end time to workaround Dukascopy onStop BUG
                    if ((currentTime + 86400000) >= endTime) {
                        LOGGER.info("dataLoaded startTime : " + dateFormat.format(startTime) + " - endTime : " + dateFormat.format(endTime) + " - currentTime : " + dateFormat.format(currentTime) + " - information : " + information);
                        for (ITesterExecutionControl executionControlObj : executionControlList) {
                            executionControlObj.cancelExecution();
                        }
                    }
                }

                @Override
                public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
                    LOGGER.info("loadingFinished startTime : " + dateFormat.format(startTime) + " - endTime : " + dateFormat.format(endTime) + " - currentTime : " + dateFormat.format(currentTime));
                }

                @Override
                public boolean stopJob() {
                    // LOGGER.info("4 - stopJob()");
                    return false;
                }
            };

            // _client.startStrategy(strategy1);
            // clearNetbeansOutput();
            _client.startStrategy(strategy1, loadingProgressListener, null, this, this, false);

        } else {
            nbStartedStrategies = _client.getStartedStrategies().size();
            if (nbStartedStrategies < maxStartedStrategies && optimizationListIndex < optimizationList.size()) {
                startLoopStrategies(optimizationListIndex, _client);
            } else {
                resetButtons();
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

           // LOGGER.info(fileName + " - " + currentParameters);
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

    private void clearNetbeansOutput() {
        try {
            Robot pressbot = new Robot();
            pressbot.keyPress(17); // Holds CTRL key.
            pressbot.keyPress(76); // Holds L key.
            pressbot.keyRelease(17); // Releases CTRL key.
            pressbot.keyRelease(76); // Releases L key.
        } catch (AWTException ex) {
            LOGGER.info(" Clear Output exception " + ex);
        }
    }
}
