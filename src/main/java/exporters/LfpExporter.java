package exporters;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import eAdapter.Document;
import eAdapter.Representative;

public class LfpExporter {

    private static final String DOC_BREAK = "D";
    private static final String CHILD_BREAK = "C";
    private static final String PAGE_BREAK = "";
    private static final String PAGE_COUNT_FIELD = "Page Count";
    private static final String TIF_EXT = "TIF";
    private static final String JPG_EXT = "JPG";
    private static final String PDF_EXT = "PDF";

    public void export(List<Document> documents, Path filePath, String imagesName, String nativeName, String volumeName) {

        BufferedWriter writer = null;

        try {
            // initialize the writer
            writer = new BufferedWriter(new FileWriter(filePath.toString()));
            // write documents
            for (Document document : documents) {
                List<String> pages = getPageRecords(document, imagesName, nativeName, volumeName);
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

    private List<String> getPageRecords(Document document, String imagesName, String nativeName, String volumeName) {
        // setup
        List<String> pageRecords = new ArrayList<>();
        Representative imageRep = null;
        Representative nativeRep = null;
        // find representatives
        for (Representative rep : document.getRepresentatives()) {
            if (rep.getType().equals(Representative.Type.IMAGE) && rep.getName().equals(imagesName)) {
                imageRep = rep;
            }
            else if (rep.getType().equals(Representative.Type.NATIVE) && rep.getName().equals(nativeName)) {
                nativeRep = rep;
            }
        }
        // check if an image representative was found
        if (imageRep != null) {
            int counter = 0;
            int iterations = getOffsetIterations(document, imageRep);
            int startIndex = (iterations == 1) ? 0 : 1;

            for (int i = startIndex; i < iterations; i++) {
                for (String file : imageRep.getFiles()) {
                    Path path = Paths.get(file);
                    String basePath = FilenameUtils.getPath(file);
                    String ext = FilenameUtils.getExtension(file).toUpperCase();
                    String imageType = "";
                    // select image type
                    if (ext.equals(TIF_EXT)) {
                        imageType = "2";
                    }
                    else if (ext.equals(JPG_EXT)) {
                        imageType = "4";
                    }
                    else if (ext.equals(PDF_EXT)) {
                        imageType = "7";
                    }
                    else {
                        throw new RuntimeException("Inalid file extension.");
                    }
                    // Token,ImageKey,BoundaryFlag,Offset,@Volume;Path;FileName;ImageType
                    String record = String.format(
                            "IM,%1$s,%2$s,%3$s,@%4$s;%5$s;%6$s;%7$s",
                            document.getKey(),
                            (counter == 0) ? ((document.getParent() == null) ? DOC_BREAK : CHILD_BREAK) : PAGE_BREAK,
                            Integer.toString(i),
                            volumeName,
                            basePath.substring(0, basePath.length() - 1),
                            path.getFileName(),
                            imageType);

                    counter++;
                    pageRecords.add(record);
                    // check for a native rep
                    if (counter == 1 && nativeRep != null) {
                        // Token,DocID,@Volume;Path;FileName,1
                        for (String nativeFile : nativeRep.getFiles()) {
                            basePath = FilenameUtils.getPath(nativeFile);
                            path = Paths.get(nativeFile);
                            String nativeRecord = String.format(
                                    "OF,%1$s,@%2$s;%3$s;%4$s,1",
                                    document.getKey(),
                                    volumeName,
                                    basePath.substring(0, basePath.length() - 1),
                                    path.getFileName());
                            pageRecords.add(nativeRecord);
                        }
                    }
                }
            }
        }
        return pageRecords;
    }

    private int getOffsetIterations(Document doc, Representative rep) {
        int iterations = 1;
        // check if the page count is greater than representative file count
        // if it is then we have a multipage image which requires a sequential 
        // offset otherwise the offset is zero
        if (doc.getMetadata().containsKey(PAGE_COUNT_FIELD)) {
            String pageCountValue = doc.getMetadata().get(PAGE_COUNT_FIELD);
            int pageCount = Integer.parseInt(pageCountValue);

            if (pageCount > 1 && rep.getFiles().size() == 1) {
                iterations = pageCount + 1;
            }
        }

        return iterations;
    }

}
