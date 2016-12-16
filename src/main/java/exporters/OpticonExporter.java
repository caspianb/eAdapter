package exporters;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import eAdapter.Document;
import eAdapter.Representative;
import parsers.Delimiters;

/**
 * 
 * @author Jeff Gillispie
 * @version December 2016
 * 
 * Purpose: Exports documents to an opticon file
 */
public class OpticonExporter {

    private static final String TRUE_VALUE = "Y";
    private static final String FALSE_VALUE = "";

    /**
     * Exports a list of documents to an opticon file
     * @param documents the documents to export
     * @param filePath the path to save the opticon file
     * @param imagesName the name of the images representative to export
     * @param volumeName the name of the volume to use in the export
     */
    public void export(List<Document> documents, Path filePath, String imagesName, String volumeName) {

        BufferedWriter writer = null;

        try {
            // initialize the writer
            writer = new BufferedWriter(new FileWriter(filePath.toString()));
            // get the field delimiter
            Delimiters delimiters = Delimiters.COMMA_DELIMITED;
            // write documents
            for (Document document : documents) {
                List<String> pages = getPageRecords(document, delimiters, imagesName, volumeName);
                // write pages
                for (String page : pages) {
                    writer.write(page);
                }
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finally {
            // close the file
            try {
                writer.close();
            }
            catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private List<String> getPageRecords(Document document, Delimiters delimiters, String imagesName, String volumeName) {
        // set up
        Representative imageRep = null;
        List<String> pageRecords = new ArrayList<>();
        // find the image representative that matches the images name
        for (Representative rep : document.getRepresentatives()) {
            if (rep.getType().equals(Representative.Type.IMAGE) && rep.getName().equals(imagesName)) {
                imageRep = rep;
                break;
            }
        }
        // check if the representative was found
        if (imageRep == null) {
            // TODO: is it ok if there is no rep found?
        }
        else {
            int counter = 0;
            for (String file : imageRep.getFiles()) {
                String record = String.format(
                        "%s,%s,%s,%s,%s,%s,%s\n", // ImageKey,VolumeName,FullPath,DocBreak,BoxBreak,FolderBreak,PageCount
                        document.getKey(),
                        volumeName,
                        file,
                        (counter == 0) ? TRUE_VALUE : FALSE_VALUE,
                        FALSE_VALUE,
                        FALSE_VALUE,
                        (counter == 0) ? imageRep.getFiles().size() : FALSE_VALUE);
                counter++;
                pageRecords.add(record);
            }
        }

        return pageRecords;
    }

}
