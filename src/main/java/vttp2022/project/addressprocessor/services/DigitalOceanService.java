package vttp2022.project.addressprocessor.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.opencsv.CSVWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import vttp2022.project.addressprocessor.exceptions.WriteToByteArrayException;
import vttp2022.project.addressprocessor.models.AddressResult;

//for writing to DO spaces
@Service
public class DigitalOceanService {

    @Autowired
    private AmazonS3 s3; 
    
    public String writeToByteArray(List<AddressResult> addResultList) throws WriteToByteArrayException 
    {
        //CSVWriter writes to OutputStreamWriter writes to ByteArrayOutputStream
        //PutObjectRequest InputStream uses ByteArrayOutputStream to write to DO
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        try (CSVWriter writer = new CSVWriter(streamWriter)) {
            String[] colHeader = {
                "BLK_NO", 
                "ROAD_NAME", 
                "BUILDING", 
                "ADDRESS", 
                "POSTAL"
            };
            writer.writeNext(colHeader);

            for (AddressResult addResult : addResultList) {
                String[] addressResultObjStr = {
                    addResult.getBlkNo(), 
                    addResult.getRoadName(), 
                    addResult.getBuilding(), 
                    addResult.getFullAddress(), 
                    addResult.getPostalCode()
                };
                writer.writeNext(addressResultObjStr);
            }
        } catch (IOException ioe) {
            WriteToByteArrayException wtbae = new WriteToByteArrayException("WriteToByteArrayException occurred");
            throw wtbae; //something went wrong while writing, at this point shouldnt occur 
        } 
        return writeByteArrayToDO(outputStream);
    }

    private String writeByteArrayToDO(ByteArrayOutputStream outputStream) 
    {
        String filename = UUID.randomUUID().toString().substring(0, 8);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(outputStream.toByteArray().length);

        PutObjectRequest putReq = new PutObjectRequest(
            "bigcontainer", 
            "OMAC/csv/%s.csv".formatted(filename), 
            new ByteArrayInputStream(outputStream.toByteArray()),
            metadata
        );
        putReq.setCannedAcl(CannedAccessControlList.PublicRead);

        try {
            s3.putObject(putReq);
            return filename;
        } catch (SdkClientException sce) {
            return ""; //something went wrong while putting into DO
        }
    }

    public boolean deleteObjectRequest(String fileToDelete) 
    {
        try {
            DeleteObjectRequest delReq = new DeleteObjectRequest(
                "bigcontainer", 
                "OMAC/csv/%s.csv".formatted(fileToDelete));
            s3.deleteObject(delReq);
            return true;
        } catch (SdkClientException sce) {
            //delete from Repo then delete from spaces, if spaces fail then repo deletion should rollback
            throw new RuntimeException("delete obj on DO spaces failed");
        }
    }
}
