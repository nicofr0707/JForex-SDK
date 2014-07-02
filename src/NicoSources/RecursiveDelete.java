package NicoSources;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecursiveDelete {

    private static final String SRC_FOLDER = "W:\\JForexCache\\.cache";

    public static void main(String[] args) {

        File directory = new File(SRC_FOLDER);

        //make sure directory exists
        if (!directory.exists()) {

            System.out.println("Directory does not exist.");
            System.exit(0);

        } else {

            String files[] = directory.list();

            for (String i : files) {
                //construct the file structure
                File currentDir = new File(directory, i);
                if (currentDir.isDirectory() && !currentDir.equals("jftemp")) {
                   // System.out.println("i : " + currentDir);
                    String files02[] = currentDir.list();

                    for (String j : files02) {
                        File dirToDelete = new File(currentDir, j);
                        String dirName = dirToDelete.getName();
                        if (dirName.contains("intraperiod")) {
                           // System.out.println("j : " + dirToDelete);
                            try {
                                delete(dirToDelete);
                            } catch (IOException ex) {
                                Logger.getLogger(RecursiveDelete.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Done");
    }

    public static void delete(File file)
            throws IOException {

        if (file.isDirectory()) {

            //directory is empty, then delete it
            if (file.list().length == 0) {

                file.delete();
                System.out.println("Directory is deleted : "
                        + file.getAbsolutePath());

            } else {

                //list all the directory contents
                String files[] = file.list();

                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp);
                    //recursive delete
                    delete(fileDelete);
                }

                //check the directory again, if empty then delete it
                if (file.list().length == 0) {
                    file.delete();
                    System.out.println("Directory is deleted : "
                            + file.getAbsolutePath());
                }
            }

        } else {
            //if file, then delete it
            file.delete();
            // System.out.println("File is deleted : " + file.getAbsolutePath());
        }
    }
}
