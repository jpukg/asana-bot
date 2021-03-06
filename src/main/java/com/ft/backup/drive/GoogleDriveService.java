package com.ft.backup.drive;

import com.asana.models.Project;
import com.ft.FileSharer;
import com.ft.config.GoogleApiConfig;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
public class GoogleDriveService {

    private static final List<String> DRIVE_SCOPES =  Collections.singletonList(DriveScopes.DRIVE);
    private static final HttpTransport httpTransport = new NetHttpTransport();
    private static final JacksonFactory jsonFactory = new JacksonFactory();
    private static final int TWO_MINUTES = 2 * 60 * 1000;
    private static final int THREE_MINUTES = 3 * 60 * 1000;

    @Autowired private GoogleApiConfig googleApiConfig;

    @Getter @Setter
    private Drive drive;

    @PostConstruct
    public void connectToDrive() throws GeneralSecurityException, IOException {
        InputStream in = googleApiConfig.toInputStream();
        GoogleCredential credential = GoogleCredential.fromStream(in, httpTransport, jsonFactory)
                .createScoped(DRIVE_SCOPES);
        this.drive = new Drive.Builder(httpTransport, jsonFactory, setHttpTimeout(credential))
                .setHttpRequestInitializer(credential).build();
    }

    private HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
        return httpRequest -> {
            requestInitializer.initialize(httpRequest);
            httpRequest.setConnectTimeout(TWO_MINUTES);
            httpRequest.setReadTimeout(THREE_MINUTES);
        };
    }

    public void uploadProjectFile(Project project, File folder, String body) throws IOException {
        CsvFileUploader csvFileUploader = new CsvFileUploader(drive, project.name, folder, body);
        csvFileUploader.upload();
    }

    public File findOrCreateRootFolder() throws IOException {
        File folder = new FileFinder(drive).findOrCreateRootFolder(googleApiConfig.getRootFolder());
        FileSharer fileSharer = new FileSharer(drive, folder, googleApiConfig.getSharedWith());
        fileSharer.share();
        return folder;
    }

    public void removeFilesOlderThan(LocalDateTime dateTimeFrom) throws IOException{
        File root = new FileFinder(drive).findFolder(googleApiConfig.getRootFolder());
        new FileRemover(drive).removeFilesOlderThan(root, dateTimeFrom);
    }
}
