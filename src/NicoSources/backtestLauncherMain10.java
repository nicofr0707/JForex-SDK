/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NicoSources;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Nicolas
 */
public class backtestLauncherMain10 {

    private static final Logger LOGGER = LoggerFactory.getLogger(TesterMainGUIMode.class);

    private static int usedPeriodsConfig_Start = 2;
    private static int usedPeriodsConfig_End = 2;

    private static double stopLossMultiplier_Start = 0.3;
    private static double stopLossMultiplier_End = 1.5;

    private static int usedSLConfig_Start = 0;
    private static int usedSLConfig_End = 5;

    private static int atrSLDEVMultiplier_Start = 0;
    private static int atrSLDEVMultiplier_End = 6;

    private static String fromDate = "";
    private static String toDate = "";
    private static String destWriteFolder = "";
    private static String JForexCacheFolder = "";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // cmd line to use : java -jar JForex-SDK-2.36.1-jar-with-dependencies.jar "2007/05/01 23:59:00" "2007/05/10 00:00:00" "C:/Users/Nicolas/Documents/JForex/Strategies/JForexStrategies/reports/currencyIndex2014/tmp/"
        if (args.length > 0) {
            fromDate = args[0];
            toDate = args[1];
            destWriteFolder = args[2];
            JForexCacheFolder = args[3];
            
        } else {
            /* For linux server
            
            
            
            */
            fromDate = "2012/11/30 23:59:00";
            //toDate = "2012/12/05 00:00:00";
            toDate = "2014/06/10 00:00:00";
            destWriteFolder = "C:/Users/Nicolas/Documents/JForex/Strategies/JForexStrategies/reports/currencyIndex2014/XMLWriteTmp_MVN/2013-2014_fini/";
            JForexCacheFolder = "W:/JForexCache/.cache";
        }

        try {
            // List with all optimization parameters
            List<String> optimizationList = new ArrayList<String>();

            for (int i = usedPeriodsConfig_Start; i <= usedPeriodsConfig_End; i++) {
                for (double j = stopLossMultiplier_Start * 10; j <= stopLossMultiplier_End * 10; j++) {
                    for (int k = usedSLConfig_Start; k <= usedSLConfig_End; k++) {
                        // on ignore 1 (atr sl 4h)
                        if (k != 1) {
                            for (int l = atrSLDEVMultiplier_Start; l <= atrSLDEVMultiplier_End; l++) {
                                optimizationList.add(i + "_" + j / 10 + "_" + k + "_" + l);
                            }
                        }
                    }
                }
            }

            int cpt = 0;
            for (String value : optimizationList) {
                cpt++;
                LOGGER.info("loop " + cpt + " : " + value);
            }
            TesterMainGUIMode testerMainGUI = new TesterMainGUIMode(optimizationList, fromDate, toDate, destWriteFolder, JForexCacheFolder);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(backtestLauncherMain10.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
