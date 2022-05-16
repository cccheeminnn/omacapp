package vttp2022.project.addressprocessor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.web.multipart.MultipartFile;

//this util class takes in the uploaded .csv file and 
//parse the search values into a Set<String> to avoid duplicates
public final class ReadCsvUtil {
    
    public static Set<String> parseSearchValue(MultipartFile file) throws IOException 
    {
        InputStreamReader isr = new InputStreamReader(file.getInputStream());
        BufferedReader buffReader = new BufferedReader(isr);
        
        //readLine below goes to the first row in the file (column header)
        String rowStr = buffReader.readLine().toLowerCase();
        int addColIndex = getColumnIndex(rowStr, "address");
        System.out.println("Address array index is at " + addColIndex);
        if (addColIndex < 0) {
            System.out.println("\"address\" could not be found in column header");
            return Collections.emptySet();
        }

        Set<String> addressSet = new HashSet<>();
        //iterate rows after col header to add search terms into addressSet
        while ((rowStr = buffReader.readLine()) != null) {
            //.csv separates each column using a , so we split the values by , and find the
            //column index containing the search value
            String[] strArray = rowStr.trim().split(",");
            //why we replace spaces with + is because OneMapAPI does not accept spaces
            String addSearchVal = strArray[addColIndex].replace(" ", "+");
            
            addressSet.add(addSearchVal);
        }
        return addressSet;
    }

    //parse the entire col header into a String separated by a comma
    //then into a String array to find the index of "address"
    private static int getColumnIndex(String rowStr, String searchStr) 
    {
        System.out.println("rowStr >>> " + rowStr);
        String[] colHeaderStrArray = rowStr.split(",");
        int addressColIndex = ArrayUtils.indexOf(colHeaderStrArray, searchStr);
        System.out.println("Address array index is at " + addressColIndex);
        return addressColIndex;
    }

}